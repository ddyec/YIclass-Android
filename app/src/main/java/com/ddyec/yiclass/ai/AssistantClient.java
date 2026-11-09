package com.ddyec.yiclass.ai;

import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public final class AssistantClient {
  public record Turn(String role, String content) {}

  public Plan propose(
      String question, Store.Snapshot snapshot, Settings settings, String key, List<Turn> history)
      throws Exception {
    String rawEndpoint = settings.get("endpoint", "https://api.deepseek.com").trim().replaceAll("/+$", "");
    if (rawEndpoint.endsWith("/chat/completions")) {
      rawEndpoint = rawEndpoint.substring(0, rawEndpoint.length() - "/chat/completions".length());
    }
    URI base = URI.create(rawEndpoint);
    if (!"https".equals(base.getScheme())
        || base.getHost() == null
        || base.getUserInfo() != null
        || base.getQuery() != null
        || base.getFragment() != null) throw new IllegalArgumentException("接口必须是有效的 HTTPS 地址");
    String url = rawEndpoint + "/chat/completions";
    String model = settings.get("model", "deepseek-chat").trim();
    boolean isReasoner = model.contains("reasoner");

    JsonObject body = new JsonObject();
    body.addProperty("model", model);
    body.addProperty("stream", false);
    body.addProperty("max_tokens", 4096);
    if (!isReasoner) {
      body.addProperty("temperature", 0.2);
      JsonObject format = new JsonObject();
      format.addProperty("type", "json_object");
      body.add("response_format", format);
    }

    JsonArray messages = new JsonArray();
    JsonObject system = new JsonObject();
    system.addProperty("role", "system");
    system.addProperty(
        "content",
        "你是YI课表助手，严格输出合法的 json 对象（不要输出 markdown 代码块之外的多余文字）。"
            + "今天="
            + LocalDate.now(settings.zone())
            + "；学期第1周周一="
            + settings.term()
            + "；时区="
            + settings.zone()
            + "。你可以直接通过 operations 增删改课表，或者直接写入完整的 items JSON 数组。"
            + "JSON 結構示例："
            + "{\"message\":\"简明中文说明\",\"operations\":[{\"action\":\"add|update|delete\",\"id\":\"现有课程id\",\"scopeDate\":\"单次调课日期YYYY-MM-DD，否则空字符串\",\"item\":{\"title\":\"名称\",\"type\":\"COURSE或EVENT\",\"day\":1,\"start\":510,\"end\":610,\"weeks\":\"1-16或1-15(单)\",\"date\":\"单次日期YYYY-MM-DD，循环课留空\",\"location\":\"地点\",\"teacher\":\"教师\",\"notes\":\"备注\",\"reminder\":15,\"color\":0}}]}"
            + "。若需要直接重写整个课表 JSON，也可返回 {\"message\":\"说明\",\"items\":[...]}。"
            + "星期 day 取值 1-7；start/end 为当天零点起分钟数（如 08:30=510，10:10=610，14:00=840）。"
            + "仅回答问题无需改动课表时，operations 置为空数组 []。");
    messages.add(system);
    for (Turn turn : history.subList(Math.max(0, history.size() - 6), history.size())) {
      JsonObject message = new JsonObject();
      message.addProperty("role", turn.role());
      message.addProperty("content", turn.content());
      messages.add(message);
    }
    JsonObject user = new JsonObject();
    user.addProperty("role", "user");
    user.addProperty(
        "content",
        "当前课表 json 数据：\n" + Store.JSON.toJson(snapshot.items()) + "\n\n用户请求（请返回 json）：" + question);
    messages.add(user);
    body.add("messages", messages);

    HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
    connection.setInstanceFollowRedirects(true);
    connection.setConnectTimeout(20000);
    connection.setReadTimeout(90000);
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestProperty("Authorization", "Bearer " + key.trim());
    try {
      try (OutputStream out = connection.getOutputStream()) {
        out.write(Store.JSON.toJson(body).getBytes(StandardCharsets.UTF_8));
      }
      int code = connection.getResponseCode();
      if (code != 200) {
        String errBody = "";
        try {
          if (connection.getErrorStream() != null) {
            errBody = readBounded(connection.getErrorStream(), 64 * 1024);
          }
        } catch (Exception ignored) {
        }
        String detail = extractErrorMessage(errBody);
        throw new IOException(
            code == 401
                ? "API Key 无效或已过期，请在设置中检查 DeepSeek API Key"
                : code == 402
                    ? "DeepSeek 账户余额不足，请前往开放平台充值"
                    : code == 429
                        ? "请求过于频繁或触发限流，请稍后重试"
                        : "AI 服务返回 HTTP " + code + (detail.isEmpty() ? "" : "：" + detail));
      }
      String text = readBounded(connection.getInputStream(), 2 * 1024 * 1024);
      JsonObject response = JsonParser.parseString(text).getAsJsonObject();
      JsonObject choice = response.getAsJsonArray("choices").get(0).getAsJsonObject();
      String rawContent = choice.getAsJsonObject("message").get("content").getAsString();
      String jsonContent = extractJsonObject(rawContent);
      Plan plan = Store.JSON.fromJson(jsonContent, Plan.class);
      if (plan == null) throw new IOException("AI 回复格式无效");
      if (plan.message == null) plan.message = "已完成课表操作";
      plan.preview(snapshot.items(), settings.term());
      return plan;
    } finally {
      connection.disconnect();
    }
  }

  public static String extractJsonObject(String raw) {
    if (raw == null) return "{}";
    String s = raw.trim();
    if (s.startsWith("```")) {
      int firstNewline = s.indexOf('\n');
      if (firstNewline > 0) s = s.substring(firstNewline + 1).trim();
      if (s.endsWith("```")) s = s.substring(0, s.length() - 3).trim();
    }
    int start = s.indexOf('{');
    int end = s.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return s.substring(start, end + 1);
    }
    return s;
  }

  private static String extractErrorMessage(String errBody) {
    if (errBody == null || errBody.isEmpty()) return "";
    try {
      JsonObject obj = JsonParser.parseString(errBody).getAsJsonObject();
      if (obj.has("error") && obj.get("error").isJsonObject()) {
        JsonObject err = obj.getAsJsonObject("error");
        if (err.has("message")) return err.get("message").getAsString();
      }
    } catch (Exception ignored) {
    }
    return "";
  }

  public static String readBounded(InputStream stream, int limit) throws IOException {
    try (InputStream in = stream;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int n;
      while ((n = in.read(buffer)) != -1) {
        if (out.size() + n > limit) throw new IOException("文件或回复过大");
        out.write(buffer, 0, n);
      }
      return out.toString("UTF-8");
    }
  }
}
