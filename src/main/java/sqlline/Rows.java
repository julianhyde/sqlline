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
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class representing a set of rows to be displayed.
 */
abstract class Rows implements Iterator<Rows.Row> {
  protected final SqlLine sqlLine;
  final ResultSetMetaData rsMeta;
  final Boolean[] primaryKeys;
  final Map<TableKey, Set<String>> tablePrimaryKeysCache = new HashMap<>();
  final NumberFormat numberFormat;
  final DateFormat dateFormat;
  final DateFormat timeFormat;
  final DateFormat timestampFormat;
  final String nullValue;

  Rows(SqlLine sqlLine, ResultSet rs) throws SQLException {
    this.sqlLine = sqlLine;
    rsMeta = rs.getMetaData();
    int count = rsMeta.getColumnCount();
    primaryKeys = new Boolean[count];
    if (SqlLineOpts.DEFAULT.equals(sqlLine.getOpts().getNumberFormat())) {
      numberFormat = null;
    } else {
      numberFormat = new DecimalFormat(sqlLine.getOpts().getNumberFormat());
    }
    if (SqlLineOpts.DEFAULT.equals(sqlLine.getOpts().getDateFormat())) {
      dateFormat = null;
    } else {
      dateFormat = new SimpleDateFormat(sqlLine.getOpts().getDateFormat());
    }
    if (SqlLineOpts.DEFAULT.equals(sqlLine.getOpts().getTimeFormat())) {
      timeFormat = null;
    } else {
      timeFormat = new SimpleDateFormat(sqlLine.getOpts().getTimeFormat());
    }
    if (SqlLineOpts.DEFAULT.equals(sqlLine.getOpts().getTimestampFormat())) {
      timestampFormat = null;
    } else {
      timestampFormat =
          new SimpleDateFormat(sqlLine.getOpts().getTimestampFormat());
    }
    if (SqlLineOpts.DEFAULT.equals(sqlLine.getOpts().getNullValue())) {
      nullValue = null;
    } else {
      nullValue = String.valueOf(sqlLine.getOpts().getNullValue());
    }
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Update all of the rows to have the same size, set to the
   * maximum length of each column in the Rows.
   */
  abstract void normalizeWidths();

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
        final Object o;
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
        values[i] = values[i] == null ? nullValue : values[i];
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
