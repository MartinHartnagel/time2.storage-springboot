package de.emphasize.time2.storage.persistence;

import de.emphasize.time2.storage.checksum.Hasher;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

public final class Persistence {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private final DbType dbType;
  private final CustomerDb customerDb;

  private Persistence(DbType dbType, CustomerDb customerDb) {
    this.dbType = dbType;
    this.customerDb = customerDb;
  }

  public static Persistence create(DbType dbType, CustomerDb customerDb) {
    return new Persistence(dbType, customerDb);
  }

  public List<Map<String, Object>> loadEvents(Long from, Long to) {
    return customerDb.query(
        "SELECT `time` as s, `name` as n, `color` as c, `end` as e FROM `"
            + customerDb.getPrefix()
            + "EVENT` WHERE (`end` IS NULL OR `end` >= ?) AND `time` <= ? ORDER BY `time` ASC",
        from,
        to);
  }

  public List<Map<String, Object>> loadInfos(Long from, Long to) {
    return customerDb.query(
        "SELECT `time` as s, `info` as i FROM `"
            + customerDb.getPrefix()
            + "INFO` WHERE `time` >= ? AND `time` <= ? ORDER BY `time` ASC",
        from,
        to);
  }

  public Map<String, Object> loadLayoutAndChanged(Long at) {
    var result =
        customerDb.query(
            "SELECT `value` as layout, `time` as changed FROM `"
                + customerDb.getPrefix()
                + "LAYOUT` WHERE `time` <= ? ORDER BY `time` DESC LIMIT 1",
            at);
    if (result.isEmpty()) {
      return null;
    }
    return result.get(0);
  }

  public List<String> loadEventDays() {
    List<String> days = new ArrayList<>();
    switch (dbType) {
      case DbType.SQLITE:
        {
          // Events
          var list =
              customerDb.query(
                  "SELECT DISTINCT DATE(ROUND(`time` / 1000), 'unixepoch', 'localtime') AS `day` FROM `EVENT` ORDER BY `day` ASC");
          days.addAll(list.stream().map(m -> (String) m.get("day")).toList());
          // Infos
          var list2 =
              customerDb.query(
                  "SELECT DISTINCT DATE(ROUND(`time` / 1000), 'unixepoch', 'localtime') AS `day` FROM `INFO` ORDER BY `day` ASC");
          days.addAll(list2.stream().map(m -> (String) m.get("day")).toList());
          break;
        }
      case DbType.MYSQL:
        {
          // Events
          var list =
              customerDb.query(
                  "SELECT DISTINCT DATE_FORMAT(CONVERT_TZ(FROM_UNIXTIME(ROUND(`time` / 1000)), '+00:00', @@session.time_zone), '%Y-%m-%d') AS `day` FROM `"
                      + customerDb.getPrefix()
                      + "EVENT` ORDER BY `day` ASC");
          days.addAll(list.stream().map(m -> (String) m.get("day")).toList());
          // Infos
          var list2 =
              customerDb.query(
                  "SELECT DISTINCT DATE_FORMAT(CONVERT_TZ(FROM_UNIXTIME(ROUND(`time` / 1000)), '+00:00', @@session.time_zone), '%Y-%m-%d') AS `day` FROM `"
                      + customerDb.getPrefix()
                      + "INFO` ORDER BY `day` ASC");
          days.addAll(list2.stream().map(m -> (String) m.get("day")).toList());
          break;
        }
      case DbType.POSTGRES:
        {
          // Events
          var list =
              customerDb.query(
                  "SELECT DISTINCT TO_CHAR(TO_TIMESTAMP(ROUND(`time` / 1000)::DECIMAL)::DATE, 'YYYY-MM-DD') AS `day` FROM `"
                      + customerDb.getPrefix()
                      + "EVENT` ORDER BY `day` ASC");
          days.addAll(list.stream().map(m -> (String) m.get("day")).toList());
          // Infos
          var list2 =
              customerDb.query(
                  "SELECT DISTINCT TO_CHAR(TO_TIMESTAMP(ROUND(`time` / 1000)::DECIMAL)::DATE, 'YYYY-MM-DD') AS `day` FROM `"
                      + customerDb.getPrefix()
                      + "INFO` ORDER BY `day` ASC");
          days.addAll(list2.stream().map(m -> (String) m.get("day")).toList());
          break;
        }
    }

    return days.stream().distinct().sorted().toList();
  }

  public String loadNote(String id) {
    String key = "note_" + id;
    var result =
        customerDb.query(
            "SELECT `value` FROM `" + customerDb.getPrefix() + "NOTE` WHERE `key` = ?", key);
    if (result.isEmpty()) {
      return null;
    }
    return (String) result.get(0).get("value");
  }

  public boolean deleteNote(String id) {
    String key = "note_" + id;
    return customerDb.execute(
        "DELETE FROM `" + customerDb.getPrefix() + "NOTE` WHERE `key` = ?", key);
  }

  public boolean storeNote(String id, String json) {
    deleteNote(id);
    String key = "note_" + id;
    return customerDb.execute(
        "INSERT INTO `" + customerDb.getPrefix() + "NOTE`  (`key`, `value`) VALUES (?, ?)",
        key,
        json);
  }

  public void storeEvent(Long time, String name, String color, Long end) {
    customerDb.execute("DELETE FROM `" + customerDb.getPrefix() + "EVENT` WHERE `time` = ?", time);

    customerDb.execute(
        "UPDATE `"
            + customerDb.getPrefix()
            + "EVENT` SET `end` = ? WHERE `time` < ? and (`end` is null OR `end` > ?)",
        time,
        time,
        time);

    if (end != time) {
      customerDb.execute(
          "INSERT INTO `"
              + customerDb.getPrefix()
              + "EVENT` (`time`, `name`, `color`, `end`) VALUES (?, ?, ?, ?)",
          time,
          name,
          color,
          end);
    }
  }

  public Map<String, String> loadInvoiceChecksums() {
    switch (dbType) {
      case DbType.SQLITE:
        {
          var list =
              customerDb.query(
                  "SELECT `key`, `value` FROM `"
                      + customerDb.getPrefix()
                      + "INVOICE` WHERE `key` like 'invoice_%'");
          return mapValuesToHash(list);
        }
      case DbType.MYSQL:
        {
          var list =
              customerDb.query(
                  "SELECT `key`, SHA2(`value`, 256) as  `checksum` FROM `"
                      + customerDb.getPrefix()
                      + "INVOICE` WHERE `key` like 'invoice_%'");
          Map<String, String> checksums = new HashMap<>();
          for (Map<String, Object> row : list) {
            checksums.put((String) row.get("key"), (String) row.get("checksum"));
          }
          return checksums;
        }
      case DbType.POSTGRES:
        {
          var list =
              customerDb.query(
                  "SELECT `key`, ENCODE(SHA256(CONVERT_TO(`value`, 'UTF8')), 'hex') as  `checksum` FROM `"
                      + customerDb.getPrefix()
                      + "INVOICE` WHERE `key` like 'invoice_%'");
          Map<String, String> checksums = new HashMap<>();
          for (Map<String, Object> row : list) {
            checksums.put((String) row.get("key"), (String) row.get("checksum"));
          }
          return checksums;
        }
    }
    throw new UnsupportedOperationException(dbType + " not implemented");
  }

  public Map<String, String> loadNoteChecksums() {
    switch (dbType) {
      case DbType.SQLITE:
        {
          var list =
              customerDb.query(
                  "SELECT `key`, `value` FROM `"
                      + customerDb.getPrefix()
                      + "NOTE` WHERE `key` like 'note_%'");
          return mapValuesToHash(list);
        }
      case DbType.MYSQL:
        {
          var list =
              customerDb.query(
                  "SELECT `key`, SHA2(`value`, 256) as  `checksum` FROM `"
                      + customerDb.getPrefix()
                      + "NOTE` WHERE `key` like 'note_%'");
          Map<String, String> checksums = new HashMap<>();
          for (Map<String, Object> row : list) {
            checksums.put((String) row.get("key"), (String) row.get("checksum"));
          }
          return checksums;
        }
      case DbType.POSTGRES:
        {
          var list =
              customerDb.query(
                  "SELECT `key`, ENCODE(SHA256(CONVERT_TO(`value`, 'UTF8')), 'hex') as  `checksum` FROM `"
                      + customerDb.getPrefix()
                      + "NOTE` WHERE `key` like 'note_%'");
          Map<String, String> checksums = new HashMap<>();
          for (Map<String, Object> row : list) {
            checksums.put((String) row.get("key"), (String) row.get("checksum"));
          }
          return checksums;
        }
    }
    throw new UnsupportedOperationException(dbType + " not implemented");
  }

  private Map<String, String> mapValuesToHash(List<Map<String, Object>> list) {
    Map<String, String> checksums = new HashMap<>();
    Hasher hasher = Hasher.create();
    for (Map<String, Object> row : list) {
      checksums.put((String) row.get("key"), hasher.hash((String) row.get("value")));
    }
    return checksums;
  }

  public String loadEventsOnDay(String day) {
    long from;
    long to;
    try {
      from = DATE_FORMAT.parse(day + " 00:00:00").getTime();
      to = DATE_FORMAT.parse(day + " 23:59:59").getTime();
    } catch (ParseException e) {
      throw new RuntimeException(day + " date parsing failed", e);
    }

    LinkedHashMap<String, String> lines = new LinkedHashMap<>();
    // Events:
    var events =
        customerDb.query(
            "SELECT `time`, `name`, `color`, `end` FROM `"
                + customerDb.getPrefix()
                + "EVENT` WHERE `time` >= ? AND `time` <= ? ORDER BY `time` DESC",
            from,
            to);

    Long lastTime = null;
    for (Map<String, Object> data : events) {
      if (((String) data.get("name")).length() > 0) {
        StringBuilder line = new StringBuilder();
        line.append(data.get("time"))
            .append("\t")
            .append(data.get("name"))
            .append("\t")
            .append(data.get("color"));
        if (data.get("end") != null) {
          line.append("\t").append(data.get("end"));
        } else if (lastTime != null) {
          line.append("\t").append(lastTime);
        }
        lines.put(DATE_FORMAT.format(new Date((Long) data.get("time"))) + 'e', line.toString());
        lastTime = (Long) data.get("time");
      }
    }

    // Infos:
    var infos =
        customerDb.query(
            "SELECT `time`, `info` FROM `"
                + customerDb.getPrefix()
                + "INFO` WHERE `time` >= ? AND `time` <= ? ORDER BY `time` ASC",
            from,
            to);
    for (Map<String, Object> data : infos) {
      if (((String) data.get("info")).length() > 0) {
        lines.put(
            DATE_FORMAT.format(new Date((Long) data.get("time"))) + 'i',
            data.get("time") + "\t" + data.get("info"));
      }
    }

    return String.join(
            "\n",
            lines.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue())
                .toList())
        .trim();
  }

  public Map<String, String> loadEventChecksums(List<String> days) {
    Map<String, String> checksums = new HashMap<>();
    Hasher hasher = Hasher.create();
    for (String day : days) {
      String events = loadEventsOnDay(day);
      checksums.put("events_" + day, hasher.hash(events));
    }
    return checksums;
  }

  public void cleanup() {
    // orphaned twigs and assets
    var invoices =
        customerDb.query(
            "SELECT `value` FROM `"
                + customerDb.getPrefix()
                + "INVOICE` WHERE `key` like 'invoice_%'");
    Map<String, Boolean> inUse = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    for (Map<String, Object> invoice : invoices) {
      var map = objectMapper.readValue((String) invoice.get("value"), Map.class);
      inUse.put((String) map.get("main"), Boolean.TRUE);
      inUse.put((String) map.get("footer"), Boolean.TRUE);
      for (String assetKey : (List<String>) map.get("assets")) {
        inUse.put(assetKey, Boolean.TRUE);
      }
    }
    var assets =
        customerDb.query(
            "SELECT `key` FROM `"
                + customerDb.getPrefix()
                + "INVOICE` WHERE `key` not like 'invoice_%'");
    Set<String> orphaned = new HashSet<>();
    for (Map<String, Object> asset : assets) {
      if (!inUse.containsKey(asset.get("key"))) {
        orphaned.add((String) asset.get("key"));
      }
    }
    for (String key : orphaned) {
      deleteInvoiceKeyValue(key);
    }
  }

  public void deleteInvoice(String invoiceNumber) {
    String key = "invoice_" + invoiceNumber;
    deleteInvoiceKeyValue(key);
  }

  private void deleteInvoiceKeyValue(String key) {
    customerDb.execute("DELETE FROM `" + customerDb.getPrefix() + "INVOICE` WHERE `key` = ?", key);
  }

  public void storeInvoice(String invoiceNumber, Map<String, Object> extracted) {
    ObjectMapper objectMapper = new ObjectMapper();
    storeInvoiceKeyValue(
        "invoice_" + invoiceNumber, objectMapper.writeValueAsString(extracted.get("invoice")));
    Map<String, String> twigs = (Map<String, String>) extracted.get("twigs");
    if (twigs != null) {
      for (String key : twigs.keySet()) {
        storeInvoiceKeyValue(key, twigs.get(key));
      }
    }
    Map<String, String> assets = (Map<String, String>) extracted.get("assets");
    if (assets != null) {
      for (String key : assets.keySet()) {
        storeInvoiceKeyValue(key, assets.get(key));
      }
    }
  }

  private void storeInvoiceKeyValue(String key, String value) {
    deleteInvoiceKeyValue(key);
    customerDb.execute(
        "INSERT INTO `" + customerDb.getPrefix() + "INVOICE` (`key`, `value`) VALUES (?, ?)",
        key,
        value);
  }

  public String loadInvoiceValue(String key) {
    var result =
        customerDb.query(
            "SELECT `value` FROM `" + customerDb.getPrefix() + "INVOICE` WHERE `key` = ?", key);
    if (result.isEmpty()) {
      return null;
    }
    return (String) result.get(0).get("value");
  }

  public void deleteAllInvoices() {
    customerDb.execute("DELETE FROM `" + customerDb.getPrefix() + "INVOICE`");
  }

  public void deleteAllNotes() {
    customerDb.execute("DELETE FROM `" + customerDb.getPrefix() + "NOTE`");
  }

  public void storeInfo(Long time, String info) {
    customerDb.execute("DELETE FROM `" + customerDb.getPrefix() + "INFO` WHERE `time` = ?", time);
    customerDb.execute(
        "INSERT INTO `" + customerDb.getPrefix() + "INFO` (`time`, `info`) VALUES (?, ?)",
        time,
        info);
  }

  public void storeLayout(Long at, String value) {
    customerDb.execute("DELETE FROM `" + customerDb.getPrefix() + "LAYOUT` WHERE `time` = ?", at);
    customerDb.execute(
        "INSERT INTO `" + customerDb.getPrefix() + "LAYOUT` (`time`, `value`) VALUES (?, ?)",
        at,
        value);
  }
}
