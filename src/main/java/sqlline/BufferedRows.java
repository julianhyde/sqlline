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
  private final List<Row> list;

  private final Iterator<Row> iterator;

  BufferedRows(SqlLine sqlLine, ResultSet rs) throws SQLException {
    super(sqlLine, rs);

    list = new LinkedList<>();

    int count = rsMeta.getColumnCount();

    list.add(new Row(count));

    while (rs.next()) {
      list.add(new Row(count, rs));
    }

    iterator = list.iterator();
  }

  public boolean hasNext() {
    return iterator.hasNext();
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
}

// End BufferedRows.java
