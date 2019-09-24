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

/**
 * OutputFormat for vertical column name: value format.
 */
class VerticalOutputFormat implements OutputFormat {
  private final SqlLine sqlLine;

  VerticalOutputFormat(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int print(Rows rows) {
    int count = 0;
    Rows.Row header = rows.next();

    while (rows.hasNext()) {
      printRow(rows, header, rows.next());
      count++;
    }

    return count;
  }

  public void printRow(Rows rows, Rows.Row header, Rows.Row row) {
    String[] head = header.values;
    String[] vals = row.values;
    int headWidth = 0;
    for (int i = 0; (i < head.length) && (i < vals.length); i++) {
      headWidth = Math.max(headWidth, head[i].length());
    }

    headWidth += 2;

    for (int i = 0; (i < head.length) && (i < vals.length); i++) {
      sqlLine.output(
          sqlLine.getColorBuffer()
              .bold(sqlLine.getColorBuffer().pad(head[i], headWidth).getMono())
              .append((vals[i] == null) ? "" : vals[i]));
    }

    sqlLine.output(""); // spacing
  }
}

// End VerticalOutputFormat.java
