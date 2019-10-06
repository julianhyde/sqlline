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

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static sqlline.SqlLine.rpad;

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
    AttributedString header = null;
    AttributedString headerCols = null;
    final int width = getCalculatedWidth();

    // normalize the columns sizes
    rows.normalizeWidths(sqlLine.getOpts().getMaxColumnWidth());

    for (; rows.hasNext();) {
      Rows.Row row = rows.next();
      AttributedString attributedString = getOutputString(rows, row);
      attributedString = attributedString
          .substring(0, Math.min(attributedString.length(), width));

      if (index == 0) {
        StringBuilder h = new StringBuilder();
        for (int j = 0; j < row.sizes.length; j++) {
          for (int k = 0; k < row.sizes[j]; k++) {
            h.append('-');
          }
          h.append("-+-");
        }

        headerCols = attributedString;
        header =
            new AttributedStringBuilder()
                .append(h.toString(), AttributedStyles.GREEN)
                .toAttributedString()
                .subSequence(0, Math.min(h.length(), headerCols.length()));
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
        printRow(attributedString, false);
      }

      index++;
    }

    if (header != null && sqlLine.getOpts().getShowHeader()) {
      printRow(header, true);
    }

    return index - 1;
  }

  private int getCalculatedWidth() {
    final int maxWidth = sqlLine.getOpts().getMaxWidth();
    int width = (maxWidth == 0 && sqlLine.getLineReader() != null
        ? sqlLine.getLineReader().getTerminal().getWidth()
        : maxWidth) - 4;
    return Math.max(width, 0);
  }

  void printRow(AttributedString attributedString, boolean header) {
    AttributedStringBuilder builder = new AttributedStringBuilder();
    if (header) {
      sqlLine.output(
          builder.append("+-", AttributedStyles.GREEN)
              .append(attributedString)
              .append("-+", AttributedStyles.GREEN)
              .toAttributedString());
    } else {
      sqlLine.output(
          builder.append("| ", AttributedStyles.GREEN)
              .append(attributedString)
              .append(" |", AttributedStyles.GREEN)
              .toAttributedString());
    }
  }

  public AttributedString getOutputString(Rows rows, Rows.Row row) {
    return getOutputString(rows, row, " | ");
  }

  private AttributedString getOutputString(
      Rows rows, Rows.Row row, String delim) {
    AttributedStringBuilder builder = new AttributedStringBuilder();

    boolean isStyled = sqlLine.getOpts().getColor();
    for (int i = 0; i < row.values.length; i++) {
      if (builder.length() > 0) {
        builder.append(delim,
            isStyled ? AttributedStyles.GREEN : AttributedStyle.DEFAULT);
      }

      String v;

      final int[] sizes = row.sizes;
      if (row.isMeta) {
        v = SqlLine.center(row.values[i], row.sizes[i]);
        if (rows.isPrimaryKey(i)) {
          builder.append(v, AttributedStyles.CYAN);
        } else {
          builder.append(v, AttributedStyle.BOLD);
        }
      } else {
        v = rpad(row.values[i], row.sizes[i]);
        if (rows.isPrimaryKey(i)) {
          builder.append(v, AttributedStyles.CYAN);
        } else {
          builder.append(v);
        }
      }
    }

    if (row.deleted) { // make deleted rows red
      return new AttributedStringBuilder()
          .append(builder.toString(), AttributedStyles.RED)
          .toAttributedString();
    } else if (row.updated) { // make updated rows blue
      return new AttributedStringBuilder()
          .append(builder.toString(), AttributedStyles.BLUE)
          .toAttributedString();
    } else if (row.inserted) { // make new rows green
      return new AttributedStringBuilder()
          .append(builder.toString(), AttributedStyles.GREEN)
          .toAttributedString();
    }

    return builder.toAttributedString();
  }

}

// End TableOutputFormat.java
