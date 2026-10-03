package de.emphasize.time2.storage.api;

import de.emphasize.time2.storage.persistence.DbType;
import de.emphasize.time2.storage.persistence.Persistence;
import de.emphasize.time2.storage.persistence.PersistenceCreator;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/event/")
public class EventController {

  @Value("${dbType}")
  DbType dbType;

  @Autowired DataSource dataSource;

  @GetMapping(value = "/", produces = "application/json;charset=utf-8;")
  public Map<String, Object> getEvents(
      @RequestParam String topic, @RequestParam Long from, @RequestParam Long to) {
    Persistence db = PersistenceCreator.create(dbType, topic, dataSource);
    var events = db.loadEvents(from, to);
    var infos = db.loadInfos(from, to);
    return Map.of("events", events, "infos", infos);
  }
}
