package de.emphasize.time2.storage.api;

import de.emphasize.time2.storage.checksum.Hasher;
import de.emphasize.time2.storage.persistence.BagSystem;
import de.emphasize.time2.storage.persistence.DbType;
import de.emphasize.time2.storage.persistence.EventSystem;
import de.emphasize.time2.storage.persistence.Persistence;
import de.emphasize.time2.storage.persistence.PersistenceCreator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(path = "/")
public class MainController {

  private static final long BAG_RETRY = 2000; // ms
  private static final Pattern PATTERN_EVENT_TYPE = Pattern.compile("^event: (.*)");
  private static final long SYNC_OVERRIDE_SEND_INIT = 20000;

  @Value("${dbType}")
  DbType dbType;

  @Autowired DataSource dataSource;

  @Autowired BagSystem bagSystem;

  @Autowired EventSystem eventSystem;

  @Autowired ObjectMapper objectMapper;

  @PostMapping(value = "/", consumes = "text/plain;charset=UTF-8")
  public ResponseEntity<?> addToStorage(
      @RequestParam String topic,
      @RequestParam(name = "u") String userId,
      @RequestParam(name = "a", required = false) Long at,
      @RequestBody String payload) {
    Persistence db = PersistenceCreator.create(dbType, topic, dataSource);
    String content =
        payload.replaceAll("%CURRENT_TIMESTAMP%", Long.toString(System.currentTimeMillis()));
    // compatibility for curl posts
    if (!"event: ".equals(content.substring(0, 7))) {
      if (content.contains("\"r\"")) {
        String bid =
            bagSystem.postBag(content.substring(1, content.length() - 1)); // strip of [ and ]
        content = "event: layout\ndata: " + bid;
      } else {
        content = "event: event\ndata: " + content;
      }
    }

    Matcher m = PATTERN_EVENT_TYPE.matcher(content);
    if (!m.find()) {
      return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    String eventType = m.group(1);
    if (!List.of(
            "layout",
            "event",
            "invoice",
            "invoice-delete",
            "invoice-delete-all",
            "note",
            "note-delete",
            "note-delete-all",
            "sync-check")
        .contains(eventType)) {
      return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // debugLog($customer . " content-len " . strlen(content) . " content " .
    // content);
    int offset = ("event: " + eventType + "\ndata: ").length();
    // debugLog($customer . " offset " . $offset . " content " . substr(content,
    // $offset, null));
    String data = content.substring(offset);

    Object input;
    try {
      if (List.of("layout", "invoice", "note", "sync-check")
          .contains(eventType)) { // bag contains json
        String bid = data.trim();
        long start = System.currentTimeMillis();
        String json = null;
        do {
          json = bagSystem.getBag(bid);
        } while (json == null && (System.currentTimeMillis() - start < BAG_RETRY));
        input = objectMapper.readValue(json, Map.class);
      } else if (List.of("invoice-delete", "invoice-delete-all", "note-delete", "note-delete-all")
          .contains(eventType)) { // data is text input
        input = data.trim();
      } else { // data is json
        String json = data.trim();
        input = objectMapper.readValue(json, List.class);
      }
    } catch (JacksonException e) {
      return new ResponseEntity<>(
          "input json_error: " + e.toString() + "\nmessage: " + e.getMessage() + "\n",
          HttpStatus.BAD_REQUEST);
    }

    // debugLog($customer . " eventType " . $eventType[1]);
    if ("sync-check".equals(eventType)) {
      var layoutAndChanged = db.loadLayoutAndChanged(System.currentTimeMillis());
      if (layoutAndChanged == null) { // this db is empty, so trigger a full import
        // debugLog($customer . " sync-import");
        eventSystem.postSSE(topic, "store", userId, "event: sync-import\ndata: {}");
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      }
      LinkedHashMap<String, String> checksums = new LinkedHashMap<>();
      List<String> days = db.loadEventDays();
      checksums.putAll(db.loadEventChecksums(days));
      checksums.putAll(db.loadInvoiceChecksums());
      checksums.putAll(db.loadNoteChecksums());

      if (layoutAndChanged != null && layoutAndChanged.get("changed") != null) {
        checksums.put("layout-changed", Long.toString((Long) layoutAndChanged.get("changed")));
        checksums.put("layout", Hasher.create().hash((String) layoutAndChanged.get("layout")));
      }

      // compare incoming and checksums
      var incoming = (Map<String, String>) input;
      LinkedHashSet<String> uniqueKeys = new LinkedHashSet<>();
      uniqueKeys.addAll(incoming.keySet());
      uniqueKeys.addAll(checksums.keySet());
      List<String> keys = uniqueKeys.stream().sorted().toList().reversed();

      Map<String, String> overrides = new HashMap<>();
      long estimatedSize = 0;
      for (String k : keys) {
        if (incoming.containsKey(k) && !checksums.containsKey(k)) {
          if (!k.startsWith("invoice_") && !k.startsWith("note_")) {
            overrides.put(k, "");
          }
        } else if (!incoming.containsKey(k) && checksums.containsKey(k)) {
          if (k.startsWith("invoice_")) {
            var invoiceJson = db.loadInvoiceValue(k);
            overrides.put(k, invoiceJson);
            var invoice = new ObjectMapper().readValue(invoiceJson, Map.class);
            String mainKey = (String) invoice.get("main");
            overrides.put(mainKey, db.loadInvoiceValue(mainKey));
            String footerKey = (String) invoice.get("footer");
            overrides.put(footerKey, db.loadInvoiceValue(footerKey));
            for (String assetKey : (List<String>) invoice.get("assets")) {
              overrides.put(assetKey, db.loadInvoiceValue(assetKey));
            }
          } else if (k.startsWith("note_")) {
            String id = k.substring("note_".length());
            var noteJson = db.loadNote(id);
            overrides.put(k, noteJson);
          } else if (k.startsWith("events_")) {
            String day = k.substring("events_".length());
            overrides.put(k, db.loadEventsOnDay(day));
          } else {
            System.err.println("incoming misses " + k);
          }
        } else if ((!"layout-changed".equals(k)
                && incoming.containsKey(k)
                && checksums.containsKey(k)
                && !incoming.get(k).equals(checksums.get(k)))
            || ("layout-changed".equals(k)
                && incoming.get(k) != null
                && checksums.containsKey(k)
                && Long.valueOf(incoming.get(k)) < Long.valueOf(checksums.get(k)))) {
          // debugLog($customer . " checksum-mismatch " . $k);
          if (k.startsWith("invoice_")) {
            var invoiceJson = db.loadInvoiceValue(k);
            overrides.put(k, invoiceJson);
            var invoice = new ObjectMapper().readValue(invoiceJson, Map.class);
            String mainKey = (String) invoice.get("main");
            overrides.put(mainKey, db.loadInvoiceValue(mainKey));
            String footerKey = (String) invoice.get("footer");
            overrides.put(footerKey, db.loadInvoiceValue(footerKey));
            for (String assetKey : (List<String>) invoice.get("assets")) {
              overrides.put(assetKey, db.loadInvoiceValue(assetKey));
            }
          } else if (k.startsWith("note_")) {
            String id = k.substring("note_".length());
            var noteJson = db.loadNote(id);
            overrides.put(k, noteJson);
          } else if (k.startsWith("events_")) {
            String day = k.substring("events_".length());
            overrides.put(k, db.loadEventsOnDay(day));
          } else if ("layout-changed".equals(k)) {
            overrides.put("layout", (String) layoutAndChanged.get("layout"));
          } else if (!"layout".equals(k)) {
            System.err.println("unhandled " + k);
          }
        }

        if (overrides.containsKey(k) && overrides.get(k) != null) {
          estimatedSize += overrides.get(k).length();
          if (estimatedSize > SYNC_OVERRIDE_SEND_INIT) {
            break;
          }
        }
      }
      if (!overrides.containsKey("layout") && overrides.containsKey("layout-changed")) {
        overrides.remove("layout-changed");
      }
      if (!overrides.isEmpty()) {
        // debugLog($customer . " sync-override " . count($overrides));
        eventSystem.postSSE(
            topic, "store", "*", "event: sync-info\ndata: sync-override " + overrides.size());
        String c;
        try {
          c = new ObjectMapper().writeValueAsString(overrides);
        } catch (Exception e) {
          return new ResponseEntity<>(
              "overrides json_error: " + e.getMessage() + "\nmessage: " + e.getMessage() + "\n",
              HttpStatus.BAD_REQUEST);
        }

        String bid = bagSystem.postBag(c);
        eventSystem.postSSE(topic, "store", userId, "event: sync-override\ndata: " + bid);
      } else {
        // debugLog($customer . " in sync");
        eventSystem.postSSE(topic, "store", "*", "event: sync-info\ndata: in sync");

        if (Math.floor(Math.random() * 100) == 42) {
          // cleanup
          // debugLog($customer . " cleanup run");
          db.cleanup();
        }
      }
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } else {
      if (!"event"
          .equals(eventType)) { // check id multi allowed, elsewise wrap to process in loop below
        input = List.of(input);
      }
      // debugLog($customer . " input " . var_export($input, true));
      List<Object> list = (List<Object>) input;
      for (Object i : list) {
        switch (eventType) {
          case "invoice":
            {
              String invoiceNumber =
                  (String)
                      ((Map<String, Object>) ((Map<String, Object>) i).get("invoice"))
                          .get("invoiceNumber");
              db.storeInvoice(invoiceNumber, (Map<String, Object>) i);
              eventSystem.postSSE(
                  topic,
                  "store",
                  "*",
                  "event: sync-info\ndata: storeInvoice " + invoiceNumber + " completed");
              break;
            }
          case "invoice-delete":
            {
              db.deleteInvoice((String) i);
              eventSystem.postSSE(
                  topic, "store", "*", "event: sync-info\ndata: deleteInvoice " + i + " completed");
              break;
            }
          case "invoice-delete-all":
            {
              db.deleteAllInvoices();
              eventSystem.postSSE(
                  topic, "store", "*", "event: sync-info\ndata: deleteAllInvoices completed");
              break;
            }
          case "note":
            {
              String id = (String) ((Map<String, Object>) i).get("id");
              db.storeNote(id, new ObjectMapper().writeValueAsString(i));
              eventSystem.postSSE(
                  topic, "store", "*", "event: sync-info\ndata: storeNote " + id + " completed");
              break;
            }
          case "note-delete":
            {
              db.deleteNote((String) i);
              eventSystem.postSSE(
                  topic,
                  "store",
                  "*",
                  "event: sync-info\ndata: deleteNote " + ((String) i) + " completed");
              break;
            }
          case "note-delete-all":
            {
              db.deleteAllNotes();
              eventSystem.postSSE(
                  topic, "store", "*", "event: sync-info\ndata: deleteAllNotes completed");
              break;
            }
          case "event":
            {
              var o = (Map<String, Object>) i;
              if (o.containsKey("n")) {
                // event
                db.storeEvent(
                    (Long) o.get("s"),
                    ((String) o.get("n")).replaceAll("\n", ""),
                    (String) o.get("c"),
                    o.containsKey("e") ? (Long) o.get("e") : null);
                // debugLog($customer . " storeEvent completed");
                eventSystem.postSSE(
                    topic, "store", "*", "event: sync-info\ndata: storeEvent completed");
              } else if (o.containsKey("i")) {

                // info
                db.storeInfo(((Long) o.get("s")), ((String) o.get("i")).replaceAll("\n", ""));
                // debugLog($customer . " storeInfo completed");
                eventSystem.postSSE(
                    topic, "store", "*", "event: sync-info\ndata: storeInfo completed");
              } else if (o.containsKey("s")) {
                // stop event
                db.storeEvent((Long) o.get("s"), "", "", (Long) o.get("s"));
                // debugLog($customer . " storeEvent completed");
                eventSystem.postSSE(
                    topic, "store", "*", "event: sync-info\ndata: storeEvent completed");
              }
              break;
            }
          case "layout":
            {
              long layoutAt = at != null ? at : System.currentTimeMillis();
              db.storeLayout(layoutAt, new ObjectMapper().writeValueAsString(i));
              // debugLog($customer . " storeLayout completed");
              eventSystem.postSSE(
                  topic, "store", "*", "event: sync-info\ndata: storeLayout completed");

              break;
            }
          default:
            // debugLog($customer . " unknown input " . var_export($i, true));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
      }

      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }
}
