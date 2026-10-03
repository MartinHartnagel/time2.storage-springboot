package de.emphasize.time2.storage.persistence;

import javax.sql.DataSource;

public final class PersistenceCreator {
  public static Persistence create(DbType dbType, String customer, DataSource dataSource) {
    switch (dbType) {
      case DbType.SQLITE:
        return Persistence.create(dbType, new SqliteCustomerDb(customer, dataSource));
      case DbType.MYSQL:
        return Persistence.create(dbType, new MysqlCustomerDb(customer, dataSource));
      case DbType.POSTGRES:
        return Persistence.create(dbType, new PostgresCustomerDb(customer, dataSource));
    }
    throw new IllegalArgumentException(dbType + " not implemented");
  }
}
