package de.emphasize.time2.storage.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

public abstract class CustomerDb {

  private final String prefix;

  protected CustomerDb(String prefix) {
    this.prefix = prefix;
  }

  protected abstract void createDm() throws SQLException;

  protected String formatSql(String sql) {
    return sql;
  }

  public boolean execute(String sql, Object... params) {
    try {
      Connection connection = getCustomerDb().getConnection();
      PreparedStatement statement = connection.prepareStatement(formatSql(sql));
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      boolean result = statement.execute();
      connection.close();
      return result;
    } catch (SQLException e) {
      throw new RuntimeException("executing sql failed", e);
    }
  }

  public List<Map<String, Object>> query(String sql, Object... params) {
    try {
      Connection connection = getCustomerDb().getConnection();
      PreparedStatement statement = connection.prepareStatement(formatSql(sql));
      for (int i = 0; i < params.length; i++) {
        statement.setObject(i + 1, params[i]);
      }
      ResultSet results = statement.executeQuery();
      List<Map<String, Object>> list = new ArrayList<>();
      ResultSetMetaData resultSetMetaData = results.getMetaData();
      while (results.next()) {
        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < resultSetMetaData.getColumnCount(); i++) {
          String column = resultSetMetaData.getColumnName(i + 1);
          values.put(column, results.getObject(i + 1));
        }
        list.add(values);
      }
      connection.close();
      return list;
    } catch (SQLException e) {
      throw new RuntimeException("query sql failed", e);
    }
  }

  protected abstract DataSource getCustomerDb();

  public String getPrefix() {
    return prefix;
  }
}
