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
 * OutputFormat for a pretty, table-like format.
 */
class TableOutputFormat implements OutputFormat {
  private final SqlLine sqlLine;

  TableOutputFormat(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int print(Rows rows) {
    int index = 0;
    ColorBuffer header = null;
    ColorBuffer headerCols = null;
    final int maxWidth =
        sqlLine.getOpts().getInt(BuiltInProperty.MAX_WIDTH);
    final int width = (maxWidth == 0
            && sqlLine.getLineReader() != null
        ? sqlLine.getLineReader().getTerminal().getWidth()
        : maxWidth) - 4;

    // normalize the columns sizes
    rows.normalizeWidths(sqlLine.getOpts().getMaxColumnWidth());

    for (; rows.hasNext();) {
      Rows.Row row = rows.next();
      ColorBuffer cbuf = getOutputString(rows, row);
      cbuf = cbuf.truncate(width);

      if (index == 0) {
        StringBuilder h = new StringBuilder();
        for (int j = 0; j < row.sizes.length; j++) {
          for (int k = 0; k < row.sizes[j]; k++) {
            h.append('-');
          }
          h.append("-+-");
        }

        headerCols = cbuf;
        header =
            sqlLine.getColorBuffer().green(h.toString()).truncate(
                headerCols.getVisibleLength());
      }

      if (sqlLine.getOpts().getShowHeader()) {
        final int headerInterval =
            sqlLine.getOpts().getHeaderInterval();
        if (index == 0
            || headerInterval > 0 && index % headerInterval == 0) {
          printRow(header, true);
          printRow(headerCols, false);
          printRow(header, true);
        }
      }

      if (index != 0) { // don't output the header twice
        printRow(cbuf, false);
      }

      index++;
    }

    if (header != null && sqlLine.getOpts().getShowHeader()) {
      printRow(header, true);
    }

    return index - 1;
  }

  void printRow(ColorBuffer cbuff, boolean header) {
    if (header) {
      sqlLine.output(sqlLine.getColorBuffer()
          .green("+-")
          .append(cbuff)
          .green("-+"));
    } else {
      sqlLine.output(sqlLine.getColorBuffer()
          .green("| ")
          .append(cbuff)
          .green(" |"));
    }
  }

  public ColorBuffer getOutputString(Rows rows, Rows.Row row) {
    return getOutputString(rows, row, " | ");
  }

  ColorBuffer getOutputString(Rows rows, Rows.Row row, String delim) {
    ColorBuffer buf = sqlLine.getColorBuffer();

    for (int i = 0; i < row.values.length; i++) {
      if (buf.getVisibleLength() > 0) {
        buf.green(delim);
      }

      ColorBuffer v;

      if (row.isMeta) {
        v = sqlLine.getColorBuffer().center(row.values[i], row.sizes[i]);
        if (rows.isPrimaryKey(i)) {
          buf.cyan(v.getMono());
        } else {
          buf.bold(v.getMono());
        }
      } else {
        v = sqlLine.getColorBuffer().pad(row.values[i], row.sizes[i]);
        if (rows.isPrimaryKey(i)) {
          buf.cyan(v.getMono());
        } else {
          buf.append(v.getMono());
        }
      }
    }

    if (row.deleted) { // make deleted rows red
      buf = sqlLine.getColorBuffer().red(buf.getMono());
    } else if (row.updated) { // make updated rows blue
      buf = sqlLine.getColorBuffer().blue(buf.getMono());
    } else if (row.inserted) { // make new rows green
      buf = sqlLine.getColorBuffer().green(buf.getMono());
    }

    return buf;
  }
}

// End TableOutputFormat.java
