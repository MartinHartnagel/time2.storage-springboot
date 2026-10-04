package de.emphasize.time2.storage.persistence;

import java.sql.SQLException;
import javax.sql.DataSource;

public final class PostgresCustomerDb extends CustomerDb {

  private final DataSource customerDb;

  protected PostgresCustomerDb(String customer, DataSource source) {
    super(customer + "_");
    this.customerDb = source;
    boolean exists = true;
    try {
      query("SELECT 1 FROM `" + getPrefix() + "LAYOUT` LIMIT 1");
    } catch (Exception $e) {
      exists = false;
    }
    if (!exists) {
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

  protected String formatSql(String sql) {
    return sql.replaceAll("`", "\"");
  }

  protected void createDm() throws SQLException {
    execute(
        "CREATE TABLE IF NOT EXISTS `"
            + getPrefix()
            + "LAYOUT` (time bigint NOT NULL UNIQUE, value text NOT NULL);");
    execute(
        "CREATE TABLE IF NOT EXISTS `"
            + getPrefix()
            + "EVENT` (time bigint NOT NULL UNIQUE, name varchar(256) NOT NULL, color varchar(7) NOT NULL, `end` bigint);");
    execute(
        "CREATE TABLE IF NOT EXISTS `"
            + getPrefix()
            + "INFO` (time bigint NOT NULL UNIQUE, info text NOT NULL);");
    execute(
        "CREATE TABLE IF NOT EXISTS `"
            + getPrefix()
            + "INVOICE` (`key` varchar(256) NOT NULL UNIQUE, value text NOT NULL);");
    execute(
        "CREATE TABLE IF NOT EXISTS `"
            + getPrefix()
            + "NOTE` (`key` varchar(256) NOT NULL UNIQUE, value text NOT NULL);");
  }
}
