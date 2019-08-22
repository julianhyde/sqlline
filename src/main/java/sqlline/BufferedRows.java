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
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Rows implementation that buffers rows in a linked list.
 *
 * <p>Detailed behavior depends on
 * {@link SqlLineOpts#getIncrementalBufferRows() incrementalBufferRows},
 * as follows:
 *
 * <ul>
 * <li>If {@code incrementalBufferRows} is negative, it buffers all rows;
 * <li>If {@code incrementalBufferRows} is zero, it buffers nothing;
 * <li>If the number of rows in result set is more than
 *     {@code incrementalBufferRows} and incremental property is false,
 *     then it enters incremental mode, with buffered limit
 *     {@code incrementalBufferRows}.
 * </ul>
 */
class BufferedRows extends Rows {
  private final ResultSet rs;
  private final Row columnNames;
  private final int columnCount;
  private final int limit;
  private List<Row> list;
  private Iterator<Row> iterator;
  private int batch = 0;
  private int[] max = null;

  BufferedRows(SqlLine sqlLine, ResultSet rs) throws SQLException {
    super(sqlLine, rs);
    this.rs = rs;
    limit = sqlLine.getOpts().getIncrementalBufferRows();
    columnCount = rsMeta.getColumnCount();
    columnNames = new Row(columnCount);
    list = nextList();
    iterator = list.iterator();
  }

  public boolean hasNext() {
    if (iterator.hasNext()) {
      return true;
    } else {
      try {
        list = nextList();
        iterator = list.iterator();
        return iterator.hasNext();
      } catch (SQLException ex) {
        throw new WrappedSqlException(ex);
      }
    }
  }

  public Row next() {
    final Row row = iterator.next();
    if (batch > 0) {
      normalizeWidth(sqlLine.getOpts().getMaxColumnWidth(), row);
    }
    return row;
  }

  void normalizeWidths(int maxColumnWidth) {
    for (Row row : list) {
      normalizeWidth(maxColumnWidth, row);
    }
  }

  private void normalizeWidth(int maxColumnWidth, Row row) {
    if (max == null) {
      max = new int[row.values.length];
    }
    for (int j = 0; j < max.length; j++) {
      int currentMaxWidth = Math.max(max[j], row.sizes[j]);
      // ensure that calculated column width
      // does not exceed max column width
      max[j] = maxColumnWidth > 0
              ? Math.min(currentMaxWidth, maxColumnWidth)
              : currentMaxWidth;
    }
    row.sizes = max;
  }

  private List<Row> nextList() throws SQLException {
    final List<Row> list = new LinkedList<>();
    if (batch == 0) {
      // Add a row of column names as the first row of the first batch.
      list.add(columnNames);
    }
    if (limit > 0) {
      // Obey the limit if the limit is non-negative and this is the first
      // batch.
      int counter = 0;
      while (counter++ < limit && rs.next()) {
        list.add(new Row(columnCount, rs));
      }
    } else if (limit == 0) {
      if (rs.next()) {
        list.add(new Row(columnCount, rs));
      }
    } else {
      while (rs.next()) {
        final Row row = new Row(columnCount, rs);
        list.add(row);
      }
    }
    ++batch;
    return list;
  }
}

// End BufferedRows.java
