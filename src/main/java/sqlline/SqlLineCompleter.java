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
 * Completer for SQLLine. It dispatches to sub-completers based on the
 * current arguments.
 */
class SqlLineCompleter
    implements Completer {
  private SqlLine sqlLine;

  SqlLineCompleter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  @Override public void complete(LineReader reader, ParsedLine line,
      List<Candidate> candidates) {
    String bufferStr = reader.getBuffer().substring(0);
    if (bufferStr.startsWith(SqlLine.COMMAND_PREFIX)
        && !bufferStr.startsWith(SqlLine.COMMAND_PREFIX + "all")
        && !bufferStr.startsWith(SqlLine.COMMAND_PREFIX + "sql")) {
      sqlLine.getCommandCompleter().complete(reader, line, candidates);
    } else if (sqlLine.getDatabaseConnection() != null
        && sqlLine.getDatabaseConnection().getSqlCompleter() != null) {
      sqlLine.getDatabaseConnection().getSqlCompleter()
          .complete(reader, line, candidates);
    }
  }
}

// End SqlLineCompleter.java
