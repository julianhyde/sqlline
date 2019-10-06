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

/**
 * OutputFormat for a table-like but borderless format.
 */
public class AnsiConsoleOutputFormat implements OutputFormat {
  private final SqlLine sqlLine;

  AnsiConsoleOutputFormat(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int print(Rows rows) {
    int index = 0;
    final int width = getCalculatedWidth();

    // normalize the columns sizes
    rows.normalizeWidths(sqlLine.getOpts().getMaxColumnWidth());

    for (; rows.hasNext();) {
      Rows.Row row = rows.next();
      AttributedString cbuf = getOutputString(
          row, index == 0 ? AttributedStyle.INVERSE : AttributedStyle.DEFAULT);
      cbuf = cbuf.substring(0, Math.min(cbuf.length(), width));

      printRow(cbuf);

      index++;
    }

    return index - 1;
  }

  private int getCalculatedWidth() {
    final int maxWidth = sqlLine.getOpts().getMaxWidth();
    return Math.max(maxWidth == 0 && sqlLine.getLineReader() != null
        ? sqlLine.getLineReader().getTerminal().getWidth()
        : maxWidth, 0);
  }

  void printRow(AttributedString cbuff) {
    sqlLine.output(cbuff);
  }

  public AttributedString getOutputString(Rows.Row row, AttributedStyle style) {
    return getOutputString(row, " ", style);
  }

  private AttributedString getOutputString(
      Rows.Row row, String delim, AttributedStyle style) {
    AttributedStringBuilder builder = new AttributedStringBuilder();

    for (int i = 0; i < row.values.length; i++) {
      if (builder.length() > 0) {
        builder.append(delim);
      }
      builder.append(SqlLine.rpad(row.values[i], row.sizes[i]), style);
    }

    return builder.toAttributedString();
  }
}

// End AnsiConsoleOutputFormat.java
