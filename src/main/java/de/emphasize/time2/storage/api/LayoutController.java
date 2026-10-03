package de.emphasize.time2.storage.api;

import de.emphasize.time2.storage.persistence.DbType;
import de.emphasize.time2.storage.persistence.Persistence;
import de.emphasize.time2.storage.persistence.PersistenceCreator;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/layout/")
public class LayoutController {

  @Value("${dbType}")
  DbType dbType;

  @Autowired DataSource dataSource;

  @GetMapping(value = "/", produces = "application/json;charset=utf-8;")
  public Map<String, Object> getLayout(@RequestParam String topic, @RequestParam Long at)
      throws SQLException {
    Persistence db = PersistenceCreator.create(dbType, topic, dataSource);
    var map = db.loadLayoutAndChanged(at);
    return map;
  }
}
