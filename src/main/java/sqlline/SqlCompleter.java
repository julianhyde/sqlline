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

import java.sql.DatabaseMetaData;
import java.util.*;

import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Suggests completions for SQL statements.
 */
class SqlCompleter extends StringsCompleter {
  SqlCompleter(SqlLine sqlLine, boolean skipMeta) {
    super(getCompletions(sqlLine, skipMeta));
  }

  private static Iterable<String> getCompletions(
      SqlLine sqlLine, boolean skipMeta) {
    Set<String> completions = new TreeSet<>();

    StringBuilder keywords = new StringBuilder();

    // now add the keywords from the current connection

    DatabaseMetaData meta = sqlLine.getDatabaseConnection().meta;
    try {
      keywords.append(",").append(meta.getSQLKeywords());
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords.append(",").append(meta.getStringFunctions());
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords.append(",").append(meta.getNumericFunctions());
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords.append(",").append(meta.getSystemFunctions());
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords.append(",").append(meta.getTimeDateFunctions());
    } catch (Throwable t) {
      // ignore
    }

    for (StringTokenizer tok = new StringTokenizer(keywords.toString(), ", ");
        tok.hasMoreTokens();) {
      completions.add(tok.nextToken());
    }

    // now add the tables and columns from the current connection
    if (!skipMeta) {
      completions.addAll(sqlLine.getColumnNames(meta));
    }

    completions.addAll(Dialect.DEFAULT_KEYWORD_SET);
    // set the Strings that will be completed
    return completions;
  }
}

// End SqlCompleter.java
