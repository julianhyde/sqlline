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
 * Abstract OutputFormat.
 */
abstract class AbstractOutputFormat implements OutputFormat {
  protected final SqlLine sqlLine;

  public AbstractOutputFormat(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int print(Rows rows) {
    int count = 0;
    Rows.Row header = rows.next();

    printHeader(header);

    while (rows.hasNext()) {
      if (count > 0 || (count == 0 && sqlLine.getOpts().getShowHeader())) {
        printRow(rows, header, rows.next());
      } else {
        rows.next();
      }
      count++;
    }

    printFooter(header);

    return count;
  }

  abstract void printHeader(Rows.Row header);

  abstract void printFooter(Rows.Row header);

  abstract void printRow(Rows rows, Rows.Row header, Rows.Row row);
}

// End AbstractOutputFormat.java
