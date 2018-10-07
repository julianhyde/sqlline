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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Suggests completions for SQL statements.
 */
class SqlCompleter extends StringsCompleter {
  SqlCompleter(SqlLine sqlLine, boolean skipMeta)
      throws IOException, SQLException {
    super(getCompletions(sqlLine, skipMeta));
  }

  private static Iterable<String> getCompletions(
      SqlLine sqlLine, boolean skipMeta) throws IOException, SQLException {
    Set<String> completions = new TreeSet<>();

    // add the default SQL completions
    String keywords =
        new BufferedReader(
            new InputStreamReader(
                SqlCompleter.class.getResourceAsStream(
                    "sql-keywords.properties"), StandardCharsets.UTF_8)
      ).readLine();

    // now add the keywords from the current connection

    DatabaseMetaData meta = sqlLine.getDatabaseConnection().meta;
    try {
      keywords += "," + meta.getSQLKeywords();
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords += "," + meta.getStringFunctions();
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords += "," + meta.getNumericFunctions();
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords += "," + meta.getSystemFunctions();
    } catch (Throwable t) {
      // ignore
    }
    try {
      keywords += "," + meta.getTimeDateFunctions();
    } catch (Throwable t) {
      // ignore
    }

    // also allow lower-case versions of all the keywords
    keywords += "," + keywords.toLowerCase(Locale.ROOT);

    for (StringTokenizer tok = new StringTokenizer(keywords, ", ");
        tok.hasMoreTokens();) {
      completions.add(tok.nextToken());
    }

    // now add the tables and columns from the current connection
    if (!skipMeta) {
      completions.addAll(sqlLine.getColumnNames(meta));
    }

    // set the Strings that will be completed
    return completions;
  }
}

// End SqlCompleter.java
