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

import java.sql.SQLException;
import java.sql.Types;

/**
 * Implementation of {@link OutputFormat} that formats rows as JSON.
 */
public class JsonOutputFormat extends AbstractOutputFormat {
  private int[] columnTypes;
  public JsonOutputFormat(SqlLine sqlLine) {
    super(sqlLine);
  }

  @Override void printHeader(Rows.Row header) {
    sqlLine.output("{\"resultset\":[");
  }

  @Override void printFooter(Rows.Row header) {
    sqlLine.output("]}");
  }

  @Override void printRow(Rows rows, Rows.Row header, Rows.Row row) {
    String[] head = header.values;
    String[] vals = row.values;
    StringBuilder sb = new StringBuilder("{");
    for (int i = 0; (i < head.length) && (i < vals.length); i++) {
      if (columnTypes == null) {
        initColumnTypes(rows, header);
      }
      sb.append("\"").append(head[i]).append("\":");
      setJsonValue(sb, vals[i], columnTypes[i]);
      sb.append((i < head.length - 1) && (i < vals.length - 1) ? "," : "");
    }
    sb.append(rows.hasNext() ? "}," : "}");
    sqlLine.output(sb.toString());
  }

  private void setJsonValue(StringBuilder sb, String value, int columnTypeId) {
    if (value == null) {
      sb.append(value);
      return;
    }
    switch (columnTypeId) {
    case Types.TINYINT:
    case Types.SMALLINT:
    case Types.INTEGER:
    case Types.BIGINT:
    case Types.REAL:
    case Types.FLOAT:
    case Types.DOUBLE:
    case Types.DECIMAL:
    case Types.NUMERIC:
    case Types.NULL:
      sb.append(value);
      return;
    case Types.BOOLEAN:
      // JSON requires true and false, not TRUE and FALSE
      sb.append(value.equalsIgnoreCase("TRUE"));
      return;
    }
    sb.append("\"");
    for (int i = 0; i < value.length(); i++) {
      if (Rows.ESCAPING_MAP.get(value.charAt(i)) != null) {
        sb.append(Rows.ESCAPING_MAP.get(value.charAt(i)));
      } else {
        sb.append(value.charAt(i));
      }
    }
    sb.append("\"");
  }

  private void initColumnTypes(Rows rows, Rows.Row header) {
    columnTypes = new int[header.values.length];
    for (int j = 0; j < header.values.length; j++) {
      try {
        columnTypes[j] = rows.rsMeta.getColumnType(j + 1);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

// End JsonOutputFormat.java
