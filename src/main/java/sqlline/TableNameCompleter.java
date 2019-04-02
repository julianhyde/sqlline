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
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Suggests completions for table names.
 */
class TableNameCompleter implements Completer {
  private SqlLine sqlLine;

  TableNameCompleter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  @Override public void complete(LineReader lineReader, ParsedLine parsedLine,
      List<Candidate> list) {
    if (sqlLine.getDatabaseConnection() == null) {
      return;
    }

    new StringsCompleter(sqlLine.getDatabaseConnection().getTableNames(true))
        .complete(lineReader, parsedLine, list);
  }
}

// End TableNameCompleter.java
