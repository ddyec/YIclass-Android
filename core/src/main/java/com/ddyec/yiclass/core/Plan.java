package com.ddyec.yiclass.core;

import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.*;

public final class Plan {
  public String message = "";
  public List<Operation> operations = new ArrayList<>();
  public List<Item> items = null;

  public static final class Operation {
    public String action = "", id = "", scopeDate = "";
    public Item item;
  }

  public List<Item> preview(List<Item> current, LocalDate term) {
    Gson gson = new Gson();
    if (items != null && (operations == null || operations.isEmpty())) {
      if (items.size() > 5000) throw new IllegalArgumentException("课程数量超过上限");
      List<Item> direct = new ArrayList<>();
      Set<String> ids = new HashSet<>();
      for (Item raw : items) {
        if (raw == null) continue;
        Item copy = gson.fromJson(gson.toJson(raw), Item.class);
        if (copy.id == null || copy.id.trim().isEmpty() || !ids.add(copy.id)) {
          copy.id = UUID.randomUUID().toString();
          ids.add(copy.id);
        }
        if (copy.excludedDates == null) copy.excludedDates = new HashSet<>();
        if (copy.location == null) copy.location = "";
        if (copy.teacher == null) copy.teacher = "";
        if (copy.notes == null) copy.notes = "";
        if (copy.type == null || copy.type.isEmpty()) copy.type = "COURSE";
        if (copy.weeks == null || copy.weeks.isEmpty()) copy.weeks = "1-16";
        if (copy.date == null) copy.date = "";
        copy.validate();
        direct.add(copy);
      }
      return direct;
    }

    if (operations == null) operations = new ArrayList<>();
    if (operations.size() > 100) throw new IllegalArgumentException("一次最多修改100项");
    List<Item> next = new ArrayList<>();
    for (Item i : current) next.add(gson.fromJson(gson.toJson(i), Item.class));
    for (Operation op : operations) {
      if (op == null) continue;
      if (op.scopeDate == null) op.scopeDate = "";
      if ("add".equals(op.action)) {
        if (op.item == null) throw new IllegalArgumentException("缺少课程内容");
        Item copy = gson.fromJson(gson.toJson(op.item), Item.class);
        copy.id = UUID.randomUUID().toString();
        if (copy.excludedDates == null) copy.excludedDates = new HashSet<>();
        if (copy.location == null) copy.location = "";
        if (copy.teacher == null) copy.teacher = "";
        if (copy.notes == null) copy.notes = "";
        if (copy.type == null || copy.type.isEmpty()) copy.type = "COURSE";
        if (copy.weeks == null || copy.weeks.isEmpty()) copy.weeks = "1-16";
        if (copy.date == null) copy.date = "";
        copy.validate();
        next.add(copy);
        continue;
      }
      Item existing =
          next.stream()
              .filter(i -> i.id.equals(op.id))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("未找到原课程，请重试"));
      if (!Set.of("update", "delete").contains(op.action))
        throw new IllegalArgumentException("未知操作：" + op.action);
      boolean scoped = !op.scopeDate.isEmpty();
      if (scoped) {
        LocalDate date = LocalDate.parse(op.scopeDate);
        if (!existing.on(date, term)) throw new IllegalArgumentException("指定日期没有这门课");
      }
      boolean single = scoped && existing.date.isEmpty();
      if (single) existing.excludedDates.add(op.scopeDate);
      else next.remove(existing);
      if ("update".equals(op.action)) {
        if (op.item == null) throw new IllegalArgumentException("缺少修改内容");
        Item copy = gson.fromJson(gson.toJson(op.item), Item.class);
        copy.id = single ? UUID.randomUUID().toString() : existing.id;
        if (copy.excludedDates == null) copy.excludedDates = new HashSet<>();
        if (copy.location == null) copy.location = "";
        if (copy.teacher == null) copy.teacher = "";
        if (copy.notes == null) copy.notes = "";
        if (copy.type == null || copy.type.isEmpty()) copy.type = existing.type;
        if (copy.weeks == null || copy.weeks.isEmpty()) copy.weeks = existing.weeks;
        if (copy.date == null) copy.date = "";
        if (single && copy.date.isEmpty()) copy.date = op.scopeDate;
        if (single) copy.excludedDates = new HashSet<>();
        if (!single) copy.excludedDates.addAll(existing.excludedDates);
        copy.validate();
        next.add(copy);
      }
    }
    Set<String> ids = new HashSet<>();
    for (Item i : next) {
      i.validate();
      if (!ids.add(i.id)) throw new IllegalArgumentException("重复课程 ID");
    }
    return next;
  }

  public List<String> summarize(List<Item> before, List<Item> after) {
    List<String> lines = new ArrayList<>();
    String[] days = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    if (operations != null && !operations.isEmpty()) {
      for (Operation op : operations) {
        if (op == null) continue;
        Item old =
            before.stream().filter(i -> i.id.equals(op.id)).findFirst().orElse(null);
        String scope = op.scopeDate == null || op.scopeDate.isEmpty() ? "" : "（仅 " + op.scopeDate + "）";
        if ("add".equals(op.action) && op.item != null) {
          String when =
              op.item.date != null && !op.item.date.isEmpty()
                  ? op.item.date
                  : days[Math.max(1, Math.min(7, op.item.day))] + " " + op.item.weeks;
          lines.add(
              "＋ 新增："
                  + op.item.title
                  + " · "
                  + when
                  + " "
                  + Schedule.time(op.item.start)
                  + "–"
                  + Schedule.time(op.item.end)
                  + (op.item.location == null || op.item.location.isEmpty()
                      ? ""
                      : " · " + op.item.location));
        } else if ("delete".equals(op.action)) {
          String name = old != null ? old.title : (op.item != null ? op.item.title : op.id);
          lines.add("－ 删除：" + name + scope);
        } else if ("update".equals(op.action) && op.item != null) {
          String when =
              op.item.date != null && !op.item.date.isEmpty()
                  ? op.item.date
                  : days[Math.max(1, Math.min(7, op.item.day))] + " " + op.item.weeks;
          lines.add(
              "✎ 修改："
                  + op.item.title
                  + scope
                  + " → "
                  + when
                  + " "
                  + Schedule.time(op.item.start)
                  + "–"
                  + Schedule.time(op.item.end)
                  + (op.item.location == null || op.item.location.isEmpty()
                      ? ""
                      : " · " + op.item.location));
        }
      }
      return lines;
    }

    if (items != null) {
      Map<String, Item> oldByFp = new LinkedHashMap<>();
      for (Item i : before) oldByFp.put(i.fingerprint(), i);
      Map<String, Item> newByFp = new LinkedHashMap<>();
      for (Item i : after) newByFp.put(i.fingerprint(), i);
      for (Item i : after) {
        if (!oldByFp.containsKey(i.fingerprint())) {
          String when =
              i.date.isEmpty() ? days[Math.max(1, Math.min(7, i.day))] + " " + i.weeks : i.date;
          lines.add(
              "＋ 写入："
                  + i.title
                  + " · "
                  + when
                  + " "
                  + Schedule.time(i.start)
                  + "–"
                  + Schedule.time(i.end));
        }
      }
      for (Item i : before) {
        if (!newByFp.containsKey(i.fingerprint())) {
          lines.add("－ 移除：" + i.title + "（" + days[Math.max(1, Math.min(7, i.day))] + "）");
        }
      }
    }
    return lines;
  }
}
