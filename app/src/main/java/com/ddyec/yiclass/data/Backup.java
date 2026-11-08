package com.ddyec.yiclass.data;

import com.ddyec.yiclass.core.Item;
import com.google.gson.*;
import java.io.*;
import java.time.*;
import java.util.*;

public final class Backup {
  public static String export(Store.Snapshot snapshot, Settings settings) {
    JsonObject root = new JsonObject();
    root.addProperty("format", "yiclass-native");
    root.addProperty("version", 1);
    root.addProperty("term", settings.term().toString());
    root.addProperty("zone", settings.zone().getId());
    root.addProperty("starts", settings.get("starts", ""));
    root.addProperty("ends", settings.get("ends", ""));
    root.add("items", Store.JSON.toJsonTree(snapshot.items()));
    return Store.JSON.toJson(root);
  }

  public record Decoded(List<Item> items, String term, String zone, String starts, String ends) {}

  public static Decoded decode(String input) {
    JsonObject root = JsonParser.parseString(input).getAsJsonObject();
    if (!"yiclass-native".equals(root.get("format").getAsString())
        || root.get("version").getAsInt() != 1) throw new IllegalArgumentException("不支持的备份格式");
    LocalDate term = LocalDate.parse(root.get("term").getAsString());
    if (term.getDayOfWeek() != DayOfWeek.MONDAY) throw new IllegalArgumentException("首周日期必须是周一");
    String zone = ZoneId.of(root.get("zone").getAsString()).getId();
    Item[] array = Store.JSON.fromJson(root.get("items"), Item[].class);
    if (array == null || array.length > 5000) throw new IllegalArgumentException("备份条目过多");
    Set<String> ids = new HashSet<>();
    for (Item i : array) {
      i.validate();
      if (!ids.add(i.id)) throw new IllegalArgumentException("备份包含重复ID");
    }
    String starts = root.has("starts") ? root.get("starts").getAsString() : "",
        ends = root.has("ends") ? root.get("ends").getAsString() : "";
    validateSlots(starts, ends);
    return new Decoded(new ArrayList<>(Arrays.asList(array)), term.toString(), zone, starts, ends);
  }

  public static void validateSlots(String starts, String ends) {
    if (starts.isEmpty() && ends.isEmpty()) return;
    String[] a = starts.split(","), b = ends.split(",");
    if (a.length != 10 || b.length != 10) throw new IllegalArgumentException("需要10节课的作息时间");
    int previous = 0;
    for (int i = 0; i < 10; i++) {
      int start = Integer.parseInt(a[i]), end = Integer.parseInt(b[i]);
      if (start < previous || end <= start || end > 1440)
        throw new IllegalArgumentException("作息时间必须顺序排列且不重叠");
      previous = end;
    }
  }
}
