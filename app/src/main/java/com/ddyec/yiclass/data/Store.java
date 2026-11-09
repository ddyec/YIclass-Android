package com.ddyec.yiclass.data;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import com.ddyec.yiclass.core.*;
import com.google.gson.*;
import java.util.*;

public final class Store extends SQLiteOpenHelper {
  public static final Gson JSON = new Gson();

  public record Snapshot(long revision, List<Item> items) {}

  public Store(Context c) {
    super(c.getApplicationContext(), "yiclass.db", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE items(id TEXT PRIMARY KEY, payload TEXT NOT NULL)");
    db.execSQL("CREATE TABLE meta(revision INTEGER NOT NULL)");
    db.execSQL("INSERT INTO meta VALUES(0)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int old, int next) {
    throw new IllegalStateException("不支持的数据库迁移");
  }

  public Snapshot snapshot() {
    SQLiteDatabase db = getReadableDatabase();
    db.beginTransaction();
    try {
      long revision =
          android.database.DatabaseUtils.longForQuery(db, "SELECT revision FROM meta", null);
      List<Item> list = new ArrayList<>();
      try (Cursor c = db.rawQuery("SELECT payload FROM items ORDER BY rowid", null)) {
        while (c.moveToNext()) {
          Item i = JSON.fromJson(c.getString(0), Item.class);
          i.validate();
          list.add(i);
        }
      }
      db.setTransactionSuccessful();
      return new Snapshot(revision, list);
    } finally {
      db.endTransaction();
    }
  }

  public void replace(long expected, List<Item> items) {
    Set<String> ids = new HashSet<>();
    for (Item i : items) {
      i.validate();
      if (!ids.add(i.id)) throw new IllegalArgumentException("重复课程ID");
    }
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      long revision =
          android.database.DatabaseUtils.longForQuery(db, "SELECT revision FROM meta", null);
      if (revision != expected) throw new IllegalStateException("课表已改变，请重新预览后保存");
      db.delete("items", null, null);
      for (Item i : items) {
        ContentValues v = new ContentValues();
        v.put("id", i.id);
        v.put("payload", JSON.toJson(i));
        db.insertOrThrow("items", null, v);
      }
      db.execSQL("UPDATE meta SET revision=revision+1");
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
