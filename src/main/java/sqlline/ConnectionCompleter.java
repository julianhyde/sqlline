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

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * Suggests completions for connection names.
 */
public class ConnectionCompleter implements Completer {
  private SqlLine sqlLine;

  ConnectionCompleter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  @Override public void complete(
      LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
    DatabaseConnections databaseConnections = sqlLine.getDatabaseConnections();
    if (databaseConnections.size() == 0) {
      return;
    }
    int i = 0;
    for (DatabaseConnection dbConnection: databaseConnections) {
      String strValue = String.valueOf(i);
      list.add(new SqlLineCommandCompleter.SqlLineCandidate(
          sqlLine, strValue, strValue, sqlLine.loc("connections"),
          dbConnection.getNickname() == null
              ? dbConnection.getUrl() : dbConnection.getNickname(),
          null, null, true));
      i++;
    }
  }
}

// End ConnectionCompleter.java
