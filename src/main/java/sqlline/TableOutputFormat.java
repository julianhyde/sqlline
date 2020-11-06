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
    AttributedString bottomHeader = null;
    AttributedString headerCols = null;
    final int width = getCalculatedWidth();
    final boolean showTypes = sqlLine.getOpts().getShowTypes();
    final TableOutputFormatStyle style =
        BuiltInTableOutputFormatStyles.BY_NAME.get(
            sqlLine.getOpts().getTableStyle());
    final String bLine = style.getBodyLine() + "";
    final String hLine = style.getHeaderLine() + "";

    // normalize the columns sizes
    rows.normalizeWidths(sqlLine.getOpts().getMaxColumnWidth());

    while (rows.hasNext()) {
      final boolean isHeader = index == 0 || index == 1 && showTypes;
      Rows.Row row = rows.next();
      AttributedString attributedString =
          getOutputString(rows, row, style, isHeader);
      attributedString = attributedString
          .substring(0, Math.min(attributedString.length(), width));

      if (index <= 1 || index <= 2 && showTypes) {
        StringBuilder top = buildHeaderLine(row, style, index == 0, false);
        StringBuilder bottom = buildHeaderLine(row, style, false, true);
        headerCols = isHeader ? attributedString : headerCols;
        header = buildHeader(headerCols, top);
        bottomHeader = buildHeader(headerCols, bottom);
      }

      if (sqlLine.getOpts().getShowHeader()) {
        final int headerInterval =
            sqlLine.getOpts().getHeaderInterval();
        if ((index <= 1 || index <= 2 && showTypes)
            || headerInterval > 0 && index % headerInterval == 0) {
          if (index == 0) {
            printRow(header, style.getHeaderTopLeft() + hLine,
                hLine + style.getHeaderTopRight());
            printRow(headerCols, style.getHeaderSeparator() + " ",
                " " + style.getHeaderSeparator());
          } else if (index == 1 && showTypes) {
            printRow(headerCols, style.getHeaderSeparator() + " ",
                " " + style.getHeaderSeparator());
          } else {
            printRow(header, style.getHeaderBodyCrossLeft() + hLine,
                hLine + style.getHeaderBodyCrossRight());
          }
        }
      }

      if (!isHeader) { // don't output the header twice
        printRow(attributedString,
            style.getBodySeparator() + " ", " " + style.getBodySeparator());
      }

      index++;
    }

    if (bottomHeader != null && sqlLine.getOpts().getShowHeader()) {
      printRow(bottomHeader, style.getBodyBottomLeft() + bLine,
          bLine + style.getBodyBottomRight());
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

  void printRow(AttributedString attributedString, String left, String right) {
    AttributedStringBuilder builder = new AttributedStringBuilder();
    sqlLine.output(
        builder.append(left, AttributedStyles.GREEN)
            .append(attributedString)
            .append(right, AttributedStyles.GREEN)
            .toAttributedString());
  }

  private StringBuilder buildHeaderLine(Rows.Row row,
      TableOutputFormatStyle style, boolean top, boolean lastLine) {
    StringBuilder header = new StringBuilder();
    final String bLine = style.getBodyLine() + "";
    final String hLine = style.getHeaderLine() + "";
    for (int j = 0; j < row.sizes.length; j++) {
      for (int k = 0; k < row.sizes[j]; k++) {
        header.append(lastLine ? bLine : hLine);
      }
      header.append(lastLine ? bLine : hLine);
      if (lastLine) {
        header.append(style.getBodyCrossUp());
      } else {
        header.append(
            top ? style.getHeaderCrossDown() : style.getHeaderBodyCross());
      }
      header.append(lastLine ? bLine : hLine);
    }
    return header;
  }

  private AttributedString buildHeader(
      AttributedString headerCols, StringBuilder hTop) {
    AttributedString topHeader;
    topHeader =
        new AttributedStringBuilder()
            .append(hTop.toString(), AttributedStyles.GREEN)
            .toAttributedString()
            .subSequence(0, Math.min(hTop.length(), headerCols.length()));
    return topHeader;
  }

  public AttributedString getOutputString(
      Rows rows, Rows.Row row, TableOutputFormatStyle style, boolean header) {
    return getOutputString(rows, row,
        " " + (header ? style.getHeaderSeparator() : style.getBodySeparator())
        + ' ');
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
