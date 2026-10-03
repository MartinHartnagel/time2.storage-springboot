package de.emphasize.time2.storage.persistence;

public enum DbType {
  SQLITE("sqlite"),
  MYSQL("mysql"),
  POSTGRES("postgres");

  private final String type;

  private DbType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
