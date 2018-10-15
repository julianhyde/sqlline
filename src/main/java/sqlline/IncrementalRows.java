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
import java.util.NoSuchElementException;

/**
 * Rows implementation which returns rows incrementally from result set
 * without any buffering.
 */
class IncrementalRows extends Rows {
  private final ResultSet rs;
  private final Row labelRow;
  private final Row maxRow;
  private Row nextRow;
  private boolean endOfResult;
  private boolean normalizingWidths;
  private DispatchCallback dispatchCallback;

  IncrementalRows(SqlLine sqlLine, ResultSet rs,
      DispatchCallback dispatchCallback) throws SQLException {
    super(sqlLine, rs);
    this.rs = rs;
    this.dispatchCallback = dispatchCallback;

    labelRow = new Row(rsMeta.getColumnCount());
    maxRow = new Row(rsMeta.getColumnCount());

    // pre-compute normalization so we don't have to deal
    // with SQLExceptions later
    for (int i = 0; i < maxRow.sizes.length; ++i) {
      // Normalized display width is based on maximum of display size
      // and label size.
      //
      // H2 returns Integer.MAX_VALUE, so avoid that.
      final int displaySize = rsMeta.getColumnDisplaySize(i + 1);
      if (displaySize > maxRow.sizes[i]
          && displaySize < Integer.MAX_VALUE) {
        maxRow.sizes[i] = displaySize;
      }
    }

    nextRow = labelRow;
    endOfResult = false;
  }

  public boolean hasNext() {
    if (endOfResult || dispatchCallback.isCanceled()) {
      return false;
    }

    if (nextRow == null) {
      try {
        if (rs.next()) {
          nextRow = new Row(labelRow.sizes.length, rs);

          if (normalizingWidths) {
            // perform incremental normalization
            nextRow.sizes = labelRow.sizes;
          }
        } else {
          endOfResult = true;
        }
      } catch (SQLException ex) {
        throw new WrappedSqlException(ex);
      }
    }

    return nextRow != null;
  }

  public Row next() {
    if (!hasNext() && !dispatchCallback.isCanceled()) {
      throw new NoSuchElementException();
    }

    Row ret = nextRow;
    nextRow = null;
    return ret;
  }

  void normalizeWidths(int maxColumnWidth) {
    // ensure that calculated column width
    // does not exceed max column width
    if (maxColumnWidth > 0) {
      for (int i = 0; i < maxRow.sizes.length; i++) {
        maxRow.sizes[i] = Math.min(maxRow.sizes[i],
                maxColumnWidth);
      }
    }

    // normalize label row
    labelRow.sizes = maxRow.sizes;

    // and remind ourselves to perform incremental normalization
    // for each row as it is produced
    normalizingWidths = true;
  }
}

// End IncrementalRows.java
