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
 * OutputFormat for values separated by a delimiter.
 */
class SeparatedValuesOutputFormat extends AbstractOutputFormat {
  private static final char DEFAULT_QUOTE_CHARACTER = '"';
  final String separator;
  final char quoteCharacter;

  SeparatedValuesOutputFormat(SqlLine sqlLine,
      String separator, char quoteCharacter) {
    super(sqlLine);
    this.separator = separator;
    this.quoteCharacter = quoteCharacter;
  }

  SeparatedValuesOutputFormat(SqlLine sqlLine, String separator) {
    this(sqlLine, separator, DEFAULT_QUOTE_CHARACTER);
  }

  @Override void printHeader(Rows.Row header) {
    printRow(header);
  }

  @Override void printFooter(Rows.Row header) {
  }

  @Override void printRow(Rows rows, Rows.Row header, Rows.Row row) {
    printRow(row);
  }

  private void printRow(Rows.Row row) {
    String[] vals = row.values;
    StringBuilder buf = new StringBuilder();
    for (String val : vals) {
      buf.append(buf.length() == 0 ? "" : "" + separator)
          .append(quoteCharacter);
      if (val != null) {
        for (char c : val.toCharArray()) {
          if (c == quoteCharacter) {
            buf.append(c);
          }
          buf.append(c);
        }
      }
      buf.append(quoteCharacter);
    }
    sqlLine.output(buf.toString());
  }
}

// End SeparatedValuesOutputFormat.java
