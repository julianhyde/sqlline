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
 * Rows implementation which buffers all rows in a linked list.
 */
class BufferedRows extends Rows {
  private final ResultSet rs;
  private final Row columnNames;
  private final int columnCount;
  private final int limit;
  private List<Row> list;
  private Iterator<Row> iterator;

  BufferedRows(SqlLine sqlLine, ResultSet rs) throws SQLException {
    super(sqlLine, rs);
    this.rs = rs;
    int incrementalBufferRows = sqlLine.getOpts().getIncrementalBufferRows();

    limit = incrementalBufferRows < 0
        ? Integer.MAX_VALUE : incrementalBufferRows;
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
        // Drain the first Row, which just contains column names
        if (!iterator.hasNext()) {
          return false;
        }
        iterator.next();

        return iterator.hasNext();
      } catch (SQLException ex) {
        throw new WrappedSqlException(ex);
      }
    }
  }

  public Row next() {
    return iterator.next();
  }

  void normalizeWidths(int maxColumnWidth) {
    int[] max = null;
    for (Row row : list) {
      if (max == null) {
        max = new int[row.values.length];
      }

      for (int j = 0; j < max.length; j++) {
        int currentMaxWidth = Math.max(max[j], row.sizes[j] + 1);
        // ensure that calculated column width
        // does not exceed max column width
        max[j] = maxColumnWidth > 0
                ? Math.min(currentMaxWidth, maxColumnWidth)
                : currentMaxWidth;
      }
    }

    for (Row row : list) {
      row.sizes = max;
    }
  }

  private List<Row> nextList() throws SQLException {
    List<Row> list = new LinkedList<>();
    list.add(columnNames);

    int counter = 0;
    while (counter++ < limit && rs.next()) {
      list.add(new Row(columnCount, rs));
    }

    return list;
  }
}

// End BufferedRows.java
