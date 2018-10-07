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
 * Implementation of {@link OutputFormat} that formats rows as XML
 * elements, and each of their columns as a nested XML element.
 */
class XmlElementOutputFormat extends AbstractOutputFormat {
  private static final String ALLOWED_NOT_ENCODE_SYMBOLS = "'\">";
  XmlElementOutputFormat(SqlLine sqlLine) {
    super(sqlLine);
  }

  public void printHeader(Rows.Row header) {
    sqlLine.output("<resultset>");
  }

  public void printFooter(Rows.Row header) {
    sqlLine.output("</resultset>");
  }

  public void printRow(Rows rows, Rows.Row header, Rows.Row row) {
    String[] head = header.values;
    String[] vals = row.values;

    sqlLine.output("  <result>");
    for (int i = 0; (i < head.length) && (i < vals.length); i++) {
      sqlLine.output(
          "    <" + head[i] + ">"
              + (SqlLine.xmlEncode(vals[i], ALLOWED_NOT_ENCODE_SYMBOLS))
              + "</" + head[i] + ">");
    }

    sqlLine.output("  </result>");
  }
}

// End XmlElementOutputFormat.java
