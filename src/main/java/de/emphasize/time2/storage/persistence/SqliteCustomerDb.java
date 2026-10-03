package de.emphasize.time2.storage.persistence;

import java.io.File;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

public final class SqliteCustomerDb extends CustomerDb {

  private final String file;
  private final DataSource customerDb;

  protected SqliteCustomerDb(String customer, DataSource derivedFrom) {
    super("");
    this.file = "./" + customer + ".db";

    boolean toCreate = !new File(file).exists();
    this.customerDb = DataSourceBuilder.derivedFrom(derivedFrom).url("jdbc:sqlite:" + file).build();
    if (toCreate) {
      try {
        createDm();
      } catch (SQLException e) {
        throw new RuntimeException("creating DM failed", e);
      }
    }
  }

  protected DataSource getCustomerDb() {
    return customerDb;
  }

  protected void createDm() throws SQLException {
    execute("CREATE TABLE IF NOT EXISTS LAYOUT (time int NOT NULL UNIQUE, value text NOT NULL);");
    execute(
        "CREATE TABLE IF NOT EXISTS EVENT (time int NOT NULL UNIQUE, name varchar(256) NOT NULL, color varchar(7) NOT NULL, end int);");
    execute("CREATE TABLE IF NOT EXISTS INFO (time int NOT NULL UNIQUE, info text NOT NULL);");
    execute(
        "CREATE TABLE IF NOT EXISTS INVOICE (`key` varchar(256) NOT NULL UNIQUE, `value` text NOT NULL);");
    execute(
        "CREATE TABLE IF NOT EXISTS NOTE (`key` varchar(256) NOT NULL UNIQUE, `value` text NOT NULL);");
  }
}
