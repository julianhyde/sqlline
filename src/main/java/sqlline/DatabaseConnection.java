/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Modified BSD License
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at:
//
// http://opensource.org/licenses/BSD-3-Clause
*/
package sqlline;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;

/**
 * Holds a database connection, credentials, and other associated state.
 */
class DatabaseConnection {
  private final SqlLine sqlLine;
  Connection connection;
  DatabaseMetaData meta;
  private final String driver;
  private final String url;
  private final Properties info;
  private String nickname;
  private Schema schema = null;
  private Completer sqlCompleter = null;
  private Dialect dialect;

  DatabaseConnection(SqlLine sqlLine, String driver, String url,
      String username, String password, Properties properties) {
    this.sqlLine = sqlLine;
    this.driver = driver;
    this.url = url;
    this.info = properties == null ? new Properties() : properties;
    if (username != null) {
      this.info.put("user", username);
    }
    if (password != null) {
      this.info.put("password", password);
    }
  }

  @Override public String toString() {
    return getUrl() + "";
  }

  void setCompletions(boolean skipmeta) {
    // setup the completer for the database
    sqlCompleter = new ArgumentCompleter(new SqlCompleter(sqlLine, skipmeta));
    // not all argument elements need to hold true
    ((ArgumentCompleter) sqlCompleter).setStrict(false);
  }

  /**
   * Initializes a syntax rule for a given database connection.
   *
   * <p>The rule is good for different highlighter and completer instances,
   * but not necessarily for other database connections (because it
   * depends on the set of keywords and identifier quote string).
   */
  private void initSyntaxRule() throws SQLException {
    // Deduce the string used to quote identifiers. For example, Oracle
    // uses double-quotes:
    //   SELECT * FROM "My Schema"."My Table"
    String identifierQuoteString = meta.getIdentifierQuoteString();
    if (identifierQuoteString.length() > 1) {
      sqlLine.error("Identifier quote string is '" + identifierQuoteString
          + "'; quote strings longer than 1 char are not supported");
      identifierQuoteString = null;
    }
    final String productName = meta.getDatabaseProductName();
    final Set<String> keywords =
        Stream.of(meta.getSQLKeywords().split(","))
            .collect(Collectors.toSet());
    dialect = DialectImpl.create(keywords, identifierQuoteString,
        productName, meta.storesLowerCaseIdentifiers(),
        meta.storesUpperCaseIdentifiers(), meta.getExtraNameCharacters());
  }

  /**
   * Connection to the specified data source.
   */
  boolean connect() throws SQLException {
    try {
      if (driver != null && driver.length() != 0) {
        Class.forName(driver);
      }
    } catch (ClassNotFoundException cnfe) {
      return sqlLine.error(cnfe);
    }

    boolean foundDriver = false;
    Driver theDriver = null;
    try {
      theDriver = DriverManager.getDriver(url);
      foundDriver = theDriver != null;
    } catch (Exception e) {
      // ignore
    }

    if (!foundDriver) {
      sqlLine.output(sqlLine.loc("autoloading-known-drivers", url));
      sqlLine.registerKnownDrivers();
      theDriver = DriverManager.getDriver(url);
    }

    try {
      close();
    } catch (Exception e) {
      return sqlLine.error(e);
    }

    // Avoid using DriverManager.getConnection(). It is a synchronized
    // method and thus holds the lock while making the connection.
    // Deadlock can occur if the driver's connection processing uses any
    // synchronized DriverManager methods.  One such example is the
    // RMI-JDBC driver, whose RJDriverServer.connect() method uses
    // DriverManager.getDriver(). Because RJDriverServer.connect runs in
    // a different thread (RMI) than the getConnection() caller (here),
    // this sequence will hang every time.
/*
          connection = DriverManager.getConnection (url, username, password);
*/
    // Instead, we use the driver instance to make the connection

    connection = theDriver.connect(url, info);
    meta = (DatabaseMetaData) Proxy.newProxyInstance(
        DatabaseMetaData.class.getClassLoader(),
        new Class[] {DatabaseMetaData.class},
        new DatabaseMetaDataHandler(connection.getMetaData()));
    try {
      sqlLine.debug(
          sqlLine.loc("connected",
              meta.getDatabaseProductName(),
              meta.getDatabaseProductVersion()));
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    try {
      sqlLine.debug(
          sqlLine.loc("driver",
              meta.getDriverName(),
              meta.getDriverVersion()));
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    try {
      connection.setAutoCommit(sqlLine.getOpts().getAutoCommit());
      sqlLine.autocommitStatus(connection);
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    try {
      connection.setReadOnly(sqlLine.getOpts().getReadOnly());
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    try {
      // nothing is done off of this command beyond the handle so no
      // need to use the callback.
      sqlLine.getCommands().isolation("isolation: " + sqlLine.getOpts()
          .getIsolation(),
          new DispatchCallback());
      initSyntaxRule();
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    sqlLine.showWarnings();

    return true;
  }

  public Connection getConnection() throws SQLException {
    if (connection != null) {
      return connection;
    }

    connect();
    sqlLine.setCompletions();
    return connection;
  }

  public void reconnect() throws Exception {
    close();
    getConnection();
  }

  public void close() {
    try {
      try {
        if (connection != null && !connection.isClosed()) {
          sqlLine.debug(
              sqlLine.loc("closing", connection.getClass().getName()));
          connection.close();
        }
      } catch (Exception e) {
        sqlLine.handleException(e);
      }
    } finally {
      connection = null;
      meta = null;
      schema = null;
    }
  }

  public Collection<String> getTableNames(boolean force) {
    return getSchema().getSchema2tables().values().stream()
        .map(Map::keySet).flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  Schema getSchema(boolean force) {
    if (schema == null || force) {
      schema = new Schema();
    }
    return schema;
  }

  Schema getSchema() {
    return getSchema(false);
  }

  DatabaseMetaData getDatabaseMetaData() {
    return meta;
  }

  String getUrl() {
    return url;
  }

  String getNickname() {
    return nickname;
  }

  void setNickname(String nickname) {
    this.nickname = nickname;
  }

  Completer getSqlCompleter() {
    return sqlCompleter;
  }

  Dialect getDialect() {
    return dialect;
  }

  String getCurrentSchema() {
    try {
      return connection.getSchema();
    } catch (Exception e) {
      // ignore
      return null;
    }
  }

  /** Schema. */
  class Schema {
    private Map<String, Map<String, Set<String>>> schema2tables;

    Map<String, Map<String, Set<String>>> getSchema2tables() {
      if (schema2tables != null) {
        return schema2tables;
      }

      schema2tables = new HashMap<>();

      try (ResultSet rs = getDatabaseMetaData()
          .getTables(getConnection().getCatalog(),
          null, "%", new String[] {"TABLE"})) {
        while (rs.next()) {
          final String tableSchema = rs.getString("TABLE_SCHEM");
          schema2tables.computeIfAbsent(tableSchema, k -> new HashMap<>());
          schema2tables.get(tableSchema).put(rs.getString("TABLE_NAME"), null);
        }
      } catch (Throwable t) {
        // ignore
      }

      return schema2tables;
    }


    Set<String> getColumnNames(String schemaName, String tableName) {
      Map<String, Set<String>> table2Column = schema2tables.get(schemaName);
      if (schemaName == null) {
        schema2tables.putIfAbsent(null, new HashMap<>());
        table2Column = schema2tables.get(null);
      }
      if (table2Column == null) {
        return Collections.emptySet();
      }
      if (table2Column.get(tableName) != null) {
        return table2Column.get(tableName);
      }
      try (ResultSet rs = getDatabaseMetaData().getColumns(
          getConnection().getCatalog(), schemaName, tableName, "%")) {
        table2Column.put(tableName, new HashSet<>());
        while (rs.next()) {
          final String columnName = rs.getString("COLUMN_NAME");
          table2Column.get(tableName).add(columnName);
        }
      } catch (Throwable t) {
        // ignore
      }
      table2Column.putIfAbsent(tableName, Collections.emptySet());
      return table2Column.get(tableName);
    }
  }
}

// End DatabaseConnection.java
