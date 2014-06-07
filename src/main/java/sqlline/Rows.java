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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

/**
 * Abstract base class representing a set of rows to be displayed.
 */
abstract class Rows implements Iterator<Rows.Row> {
  protected final SqlLine sqlLine;
  final ResultSetMetaData rsMeta;
  final Boolean[] primaryKeys;
  final NumberFormat numberFormat;

  Rows(SqlLine sqlLine, ResultSet rs) throws SQLException {
    this.sqlLine = sqlLine;
    rsMeta = rs.getMetaData();
    int count = rsMeta.getColumnCount();
    primaryKeys = new Boolean[count];
    if (sqlLine.getOpts().getNumberFormat().equals("default")) {
      numberFormat = null;
    } else {
      numberFormat = new DecimalFormat(sqlLine.getOpts().getNumberFormat());
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
      // this doesn't always work, since some JDBC drivers (e.g.,
      // Oracle's) return a blank string from getTableName.
      String table = rsMeta.getTableName(col + 1);
      String column = rsMeta.getColumnName(col + 1);

      if (table == null
          || table.length() == 0
          || column == null
          || column.length() == 0) {
        return primaryKeys[col] = false;
      }

      ResultSet pks =
          sqlLine.getDatabaseConnection().meta.getPrimaryKeys(
              sqlLine.getDatabaseConnection().meta.getConnection().getCatalog(),
              null,
              table);

      try {
        while (pks.next()) {
          if (column.equalsIgnoreCase(
              pks.getString("COLUMN_NAME"))) {
            return primaryKeys[col] = true;
          }
        }
      } finally {
        pks.close();
      }

      return primaryKeys[col] = false;
    } catch (SQLException sqle) {
      return primaryKeys[col] = false;
    }
  }

  static Object readResult(ResultSet rs, int col) throws SQLException {
    switch (rs.getMetaData().getColumnType(col)) {
    case Types.ARRAY:
      return rs.getArray(col);
    case Types.BIGINT:
      return rs.getLong(col);
    case Types.BINARY:
    case Types.LONGVARBINARY:
    case Types.VARBINARY:
      return rs.getBytes(col);
    case Types.BIT:
    case Types.BOOLEAN:
      return rs.getBoolean(col);
    case Types.BLOB:
      return rs.getBlob(col);
    case Types.CHAR:
    case Types.LONGNVARCHAR:
    case Types.LONGVARCHAR:
    case Types.NCHAR:
    case Types.NVARCHAR:
    case Types.VARCHAR:
      return rs.getString(col);
    case Types.CLOB:
      return rs.getClob(col);
    case Types.DATALINK:
      return rs.getURL(col);
    case Types.DATE:
      return rs.getDate(col);
    case Types.DECIMAL:
    case Types.NUMERIC:
      return rs.getBigDecimal(col);
    case Types.DISTINCT:
    case Types.STRUCT:
    case Types.JAVA_OBJECT:
    case Types.NULL:
    case Types.OTHER:
    default:
      return rs.getObject(col);
    case Types.DOUBLE:
      return rs.getDouble(col);
    case Types.FLOAT:
    case Types.REAL:
      return rs.getFloat(col);
    case Types.INTEGER:
      return rs.getInt(col);
    case Types.NCLOB:
      return rs.getNClob(col);
    case Types.REF:
      return rs.getRef(col);
    case Types.ROWID:
      return rs.getRowId(col);
    case Types.SMALLINT:
      return rs.getShort(col);
    case Types.SQLXML:
      return rs.getSQLXML(col);
    case Types.TIME:
      return rs.getTime(col);
    case Types.TIMESTAMP:
      return rs.getTimestamp(col);
    case Types.TINYINT:
      return rs.getByte(col);
    }
  }

  static String toBinaryString(byte []buf) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < buf.length; ++i) {
      int ch = buf[i] & 0xFF;
      if ((ch >= '0' && ch <= '9')
          || (ch >= 'A' && ch <= 'Z')
          || (ch >= 'a' && ch <= 'z')
          || " `~!@#$%^&*()-_=+[]{}|;:'\",.<>/?".indexOf(ch) >= 0) {
        result.append((char) ch);
      } else {
        result.append(String.format("\\x%02X", ch));
      }
    }
    return result.toString();
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
        Object o = readResult(rs, i + 1);

        if (o == null) {
          values[i] = null;
        } else if (o instanceof Number) {
          if (numberFormat != null) {
            values[i] = numberFormat.format(o);
          } else {
            values[i] = o.toString();
          }
        } else if (o instanceof byte[]) {
          values[i] = toBinaryString((byte []) o);
        } else {
          // Use ResultSet.getObject and let Java do the conversion rather than
          // assuming ResultSet.getString() can cast properly.
          values[i] = String.valueOf(o);
        }
        sizes[i] = values[i] == null ? 1 : values[i].length();
      }
    }
  }
}

// End Rows.java
