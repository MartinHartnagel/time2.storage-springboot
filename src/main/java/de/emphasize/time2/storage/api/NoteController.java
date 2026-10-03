package de.emphasize.time2.storage.api;

import de.emphasize.time2.storage.persistence.DbType;
import de.emphasize.time2.storage.persistence.Persistence;
import de.emphasize.time2.storage.persistence.PersistenceCreator;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(path = "/note/")
public class NoteController {

  @Value("${dbType}")
  DbType dbType;

  @Autowired DataSource dataSource;

  @Autowired ObjectMapper parser;

  @GetMapping(value = "/", produces = "application/json;charset=utf-8;")
  public ResponseEntity<?> getNote(
      @RequestParam String topic, @RequestParam String id, @RequestParam Long nt_changed)
      throws SQLException {
    Persistence db = PersistenceCreator.create(dbType, topic, dataSource);
    String json = db.loadNote(id);
    if (json == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    var o = parser.readValue(json, Map.class);
    if ((Long) o.get("changed") > nt_changed) {
      return new ResponseEntity<>(json, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
  }

  @PostMapping(value = "/", produces = "application/json;charset=utf-8;")
  public ResponseEntity<?> setNote(
      @RequestParam String topic, @RequestParam String id, @RequestBody String note)
      throws SQLException {
    Persistence db = PersistenceCreator.create(dbType, topic, dataSource);
    db.deleteNote(id);
    var success = db.storeNote(id, note);
    return success
        ? new ResponseEntity<>(HttpStatus.ACCEPTED)
        : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  @DeleteMapping(value = "/", produces = "application/json;charset=utf-8;")
  public ResponseEntity<?> removeNote(@RequestParam String topic, @RequestParam String id)
      throws SQLException {
    Persistence db = PersistenceCreator.create(dbType, topic, dataSource);
    var success = db.deleteNote(id);
    return success
        ? new ResponseEntity<>(HttpStatus.ACCEPTED)
        : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }
}
