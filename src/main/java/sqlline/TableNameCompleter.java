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

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

/**
 * Suggests completions for table names.
 */
class TableNameCompleter implements Completer {
  private SqlLine sqlLine;

  public TableNameCompleter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int complete(String buf, int pos, List<CharSequence> candidates) {
    if (sqlLine.getDatabaseConnection() == null) {
      return -1;
    }

    return new StringsCompleter(
          sqlLine.getDatabaseConnection().getTableNames(true))
        .complete(buf, pos, candidates);
  }
}

// End TableNameCompleter.java
