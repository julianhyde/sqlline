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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract base class representing a set of rows to be displayed.
 */
abstract class Rows implements Iterator<Rows.Row> {
  static final Map<Character, String> ESCAPING_MAP = createEscapeMap();

  protected final SqlLine sqlLine;
  final ResultSetMetaData rsMeta;
  final Boolean[] primaryKeys;
  final Map<TableKey, Set<String>> tablePrimaryKeysCache = new HashMap<>();
  final NumberFormat numberFormat;
  final DateFormat dateFormat;
  final DateFormat timeFormat;
  final DateFormat timestampFormat;
  final String nullValue;
  final boolean escapeOutput;

  Rows(SqlLine sqlLine, ResultSet rs) throws SQLException {
    this.sqlLine = sqlLine;
    rsMeta = rs.getMetaData();
    int count = rsMeta.getColumnCount();
    primaryKeys = new Boolean[count];
    final String numberFormatPropertyValue =
        sqlLine.getOpts().getNumberFormat();
    if (Objects.equals(numberFormatPropertyValue,
        BuiltInProperty.NUMBER_FORMAT.defaultValue())) {
      numberFormat = null;
    } else {
      numberFormat = new DecimalFormat(numberFormatPropertyValue,
          DecimalFormatSymbols.getInstance(Locale.ROOT));
    }
    final String dateFormatPropertyValue =
        sqlLine.getOpts().getDateFormat();
    if (Objects.equals(dateFormatPropertyValue,
        BuiltInProperty.DATE_FORMAT.defaultValue())) {
      dateFormat = null;
    } else {
      dateFormat =
          new SimpleDateFormat(dateFormatPropertyValue, Locale.ROOT);
    }
    final String timeFormatPropertyValue =
        sqlLine.getOpts().getTimeFormat();
    if (Objects.equals(timeFormatPropertyValue,
        BuiltInProperty.TIME_FORMAT.defaultValue())) {
      timeFormat = null;
    } else {
      timeFormat =
          new SimpleDateFormat(timeFormatPropertyValue, Locale.ROOT);
    }
    final String timestampFormatPropertyValue =
        sqlLine.getOpts().getTimestampFormat();
    if (Objects.equals(timestampFormatPropertyValue,
        BuiltInProperty.TIMESTAMP_FORMAT.defaultValue())) {
      timestampFormat = null;
    } else {
      timestampFormat =
          new SimpleDateFormat(timestampFormatPropertyValue, Locale.ROOT);
    }
    final String nullPropertyValue =
        sqlLine.getOpts().getNullValue();
    if (Objects.equals(nullPropertyValue,
        BuiltInProperty.NULL_VALUE.defaultValue())) {
      nullValue = null;
    } else {
      nullValue = String.valueOf(nullPropertyValue);
    }
    escapeOutput = sqlLine.getOpts().getEscapeOutput();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Update all of the rows to have the same size, set to the
   * maximum length of each column in the Rows.
   *
   * @param maxColumnWidth max allowed column width
   */
  abstract void normalizeWidths(int maxColumnWidth);

  /**
   * Return whether the specified column (0-based index) is
   * a primary key. Since this method depends on whether
   * the JDBC driver property implements
   * {@link java.sql.ResultSetMetaData#getTableName} (many do not), it
   * is not reliable for all databases.
   */
  boolean isPrimaryKey(int col) {
    if (primaryKeys[col] != null) {
      return primaryKeys[col];
    }

    try {
      // Convert column index into JDBC column number.
      int colNum = col + 1;

      // If the table name can't be determined exit quickly.
      // this doesn't always work, since some JDBC drivers (e.g.,
      // Oracle's) return a blank string from getTableName.
      String table = rsMeta.getTableName(colNum);
      if (table == null || table.length() == 0) {
        return primaryKeys[col] = false;
      }

      // If the column name can't be determined exit quickly.
      String column = rsMeta.getColumnName(colNum);
      if (column == null || column.length() == 0) {
        return primaryKeys[col] = false;
      }

      // Retrieve the catalog and schema name for this connection.
      // Either or both may be null.
      DatabaseMetaData dbMeta = sqlLine.getDatabaseConnection().meta;
      String catalog = dbMeta.getConnection().getCatalog();
      String schema = rsMeta.getSchemaName(colNum);

      // Get the (possibly cached) set of primary key columns
      // for the table containing this column.
      Set<String> tablePrimaryKeys =
          getTablePrimaryKeys(catalog, schema, table);

      // Determine if this column is a primary key and cache the result.
      return primaryKeys[col] = tablePrimaryKeys != null
          && tablePrimaryKeys.contains(column);

    } catch (SQLException e) {
      // Ignore the exception and assume that this column
      // isn't a primary key for display purposes.
      return primaryKeys[col] = false;
    }
  }

  private static Map<Character, String> createEscapeMap() {
    final Map<Character, String> map = new HashMap<>();
    map.put('\\', "\\\\");
    map.put('\"', "\\\"");
    map.put('\b', "\\b");
    map.put('\f', "\\f");
    map.put('\n', "\\n");
    map.put('\r', "\\r");
    map.put('\t', "\\t");
    map.put('/', "\\/");
    map.put('\u0000', "\\u0000");
    map.put('\u0001', "\\u0001");
    map.put('\u0002', "\\u0002");
    map.put('\u0003', "\\u0003");
    map.put('\u0004', "\\u0004");
    map.put('\u0005', "\\u0005");
    map.put('\u0006', "\\u0006");
    map.put('\u0007', "\\u0007");
    // ESCAPING_MAP.put('\u0008', "\\u0008");
    // covered by ESCAPING_MAP.put('\b', "\\b");
    // ESCAPING_MAP.put('\u0009', "\\u0009");
    // covered by ESCAPING_MAP.put('\t', "\\t");
    // ESCAPING_MAP.put((char) 10, "\\u000A");
    // covered by ESCAPING_MAP.put('\n', "\\n");
    map.put('\u000B', "\\u000B");
    // ESCAPING_MAP.put('\u000C', "\\u000C");
    // covered by ESCAPING_MAP.put('\f', "\\f");
    // ESCAPING_MAP.put((char) 13, "\\u000D");
    // covered by ESCAPING_MAP.put('\r', "\\r");
    map.put('\u000E', "\\u000E");
    map.put('\u000F', "\\u000F");
    map.put('\u0010', "\\u0010");
    map.put('\u0011', "\\u0011");
    map.put('\u0012', "\\u0012");
    map.put('\u0013', "\\u0013");
    map.put('\u0014', "\\u0014");
    map.put('\u0015', "\\u0015");
    map.put('\u0016', "\\u0016");
    map.put('\u0017', "\\u0017");
    map.put('\u0018', "\\u0018");
    map.put('\u0019', "\\u0019");
    map.put('\u001A', "\\u001A");
    map.put('\u001B', "\\u001B");
    map.put('\u001C', "\\u001C");
    map.put('\u001D', "\\u001D");
    map.put('\u001E', "\\u001E");
    map.put('\u001F', "\\u001F");
    return Collections.unmodifiableMap(map);
  }

  /** Row from a result set. */
  class Row {
    final String[] values;
    final boolean isMeta;
    protected boolean deleted;
    protected boolean inserted;
    protected boolean updated;
    protected int[] sizes;

    Row(int size) throws SQLException {
      isMeta = true;
      values = new String[size];
      sizes = new int[size];
      for (int i = 0; i < size; i++) {
        values[i] = rsMeta.getColumnLabel(i + 1);
        sizes[i] = values[i] == null ? 1 : values[i].length();
      }

      deleted = false;
      updated = false;
      inserted = false;
    }

    Row(int size, ResultSet rs) throws SQLException {
      isMeta = false;
      values = new String[size];
      sizes = new int[size];

      try {
        deleted = rs.rowDeleted();
      } catch (Throwable t) {
        // ignore
      }
      try {
        updated = rs.rowUpdated();
      } catch (Throwable t) {
        // ignore
      }
      try {
        inserted = rs.rowInserted();
      } catch (Throwable t) {
        // ignore
      }

      for (int i = 0; i < size; i++) {
        switch (rs.getMetaData().getColumnType(i + 1)) {
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
        case Types.REAL:
        case Types.FLOAT:
        case Types.DOUBLE:
        case Types.DECIMAL:
        case Types.NUMERIC:
          setFormat(rs.getObject(i + 1), numberFormat, i);
          break;
        case Types.BIT:
        case Types.CLOB:
        case Types.BLOB:
        case Types.REF:
        case Types.JAVA_OBJECT:
        case Types.STRUCT:
        case Types.ROWID:
        case Types.NCLOB:
        case Types.SQLXML:
          setFormat(rs.getObject(i + 1), null, i);
          break;
        case Types.TIME:
          setFormat(rs.getObject(i + 1), timeFormat, i);
          break;
        case Types.DATE:
          setFormat(rs.getObject(i + 1), dateFormat, i);
          break;
        case Types.TIMESTAMP:
          setFormat(rs.getObject(i + 1), timestampFormat, i);
          break;
        default:
          values[i] = rs.getString(i + 1);
          break;
        }
        values[i] = values[i] == null
            ? nullValue
            : escapeOutput
                ? escapeControlSymbols(values[i])
                : values[i];
        sizes[i] = values[i] == null ? 1 : values[i].length();
      }
    }

    private void setFormat(Object o, Format format, int i) {
      if (o == null) {
        values[i] = String.valueOf(nullValue);
      } else if (format != null) {
        values[i] = format.format(o);
      } else {
        values[i] = o.toString();
      }
    }
  }

  /**
   * Escapes control symbols (Character.getType(ch) == Character.CONTROL).
   *
   * @param value String to be escaped
   *
   * @return escaped string if input value contains control symbols
   * otherwise returns the input value
   */
  static String escapeControlSymbols(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    StringBuilder result = null;
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (Character.getType(ch) == Character.CONTROL) {
        if (result == null) {
          result = new StringBuilder();
          if (i != 0) {
            result.append(value, 0, i);
          }
        }
        result.append(ESCAPING_MAP.get(ch));
      } else if (result != null) {
        result.append(ch);
      }
    }
    return result == null ? value : result.toString();
  }

  /**
   * Load and cache a set of primary key column names given a table key
   * (i.e. catalog, schema and table name).  The result cannot be considered
   * authoritative as since it depends on whether the JDBC driver property
   * implements {@link java.sql.ResultSetMetaData#getTableName} and many
   * drivers/databases do not.
   *
   * @param tableKey A key (containing catalog, schema and table names) into
   *                 the table primary key cache.  Must not be null.
   * @return A set of primary key column names.  May be empty but will
   *         never be null.
   */
  private Set<String> loadAndCachePrimaryKeysForTable(TableKey tableKey) {
    Set<String> primaryKeys = new HashSet<>();
    try {
      ResultSet pks = sqlLine.getDatabaseConnection().meta.getPrimaryKeys(
          tableKey.catalog, tableKey.schema, tableKey.table);
      try {
        while (pks.next()) {
          primaryKeys.add(pks.getString("COLUMN_NAME"));
        }
      } finally {
        pks.close();
      }
    } catch (SQLException e) {
      // Ignore exception and proceed with the current state (possibly empty)
      // of the primaryKey set.
    }
    tablePrimaryKeysCache.put(tableKey, primaryKeys);
    return primaryKeys;
  }

  /**
   * Gets a set of primary key column names given a table key (i.e. catalog,
   * schema and table name).  The returned set may be cached as a result of
   * previous requests for the same table key.
   *
   * <p>The result cannot be considered authoritative as since it depends on
   * whether the JDBC driver property implements
   * {@link java.sql.ResultSetMetaData#getTableName} and many drivers/databases
   * do not.
   *
   * @param catalog The catalog for the table.  May be null.
   * @param schema The schema for the table.  May be null.
   * @param table The name of table.  May not be null.
   * @return A set of primary key column names.  May be empty but
   *         will never be null.
   */
  private Set<String> getTablePrimaryKeys(
      String catalog, String schema, String table) {
    TableKey tableKey = new TableKey(catalog, schema, table);
    Set<String> primaryKeys = tablePrimaryKeysCache.get(tableKey);
    if (primaryKeys == null) {
      primaryKeys = loadAndCachePrimaryKeysForTable(tableKey);
    }
    return primaryKeys;
  }

  /**
   * The table coordinates used as key into table primary key cache.
   */
  private static class TableKey {
    private final String catalog;
    private final String schema;
    private final String table;

    private TableKey(String catalog, String schema, String table) {
      this.catalog = catalog;
      this.schema = schema;
      this.table = table;
    }

    public int hashCode() {
      return catalog == null ? 13 : catalog.hashCode()
          + schema == null ? 17 : schema.hashCode()
          + table == null ? 19 : table.hashCode();
    }

    public boolean equals(Object obj) {
      TableKey that = obj instanceof TableKey ? (TableKey) obj : null;
      return that != null && this.toString().equals(that.toString());
    }

    public String toString() {
      return catalog == null ? "" : catalog + ":"
          + schema == null ? "" : schema + ":"
          + table == null ? "" : table;
    }
  }

}

// End Rows.java
