package com.ddyec.yiclass.core;

import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.*;

/** AI output is a proposal, validated against a snapshot and applied atomically after review. */
public final class Plan {
  public String message = "";
  public List<Operation> operations = new ArrayList<>();

  public static final class Operation {
    public String action = "", id = "", scopeDate = "";
    public Item item;
  }

  public List<Item> preview(List<Item> current, LocalDate term) {
    if (operations == null || operations.size() > 50)
      throw new IllegalArgumentException("一次最多修改50项");
    Gson gson = new Gson();
    List<Item> next = new ArrayList<>();
    for (Item i : current) next.add(gson.fromJson(gson.toJson(i), Item.class));
    for (Operation op : operations) {
      if (op == null || op.scopeDate == null) throw new IllegalArgumentException("无效的修改操作");
      if ("add".equals(op.action)) {
        if (op.item == null) throw new IllegalArgumentException("缺少课程内容");
        Item copy = gson.fromJson(gson.toJson(op.item), Item.class);
        copy.id = UUID.randomUUID().toString();
        copy.validate();
        next.add(copy);
        continue;
      }
      Item existing =
          next.stream()
              .filter(i -> i.id.equals(op.id))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("课程已变化，请重新生成方案"));
      if (!Set.of("update", "delete").contains(op.action))
        throw new IllegalArgumentException("未知操作");
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
}
