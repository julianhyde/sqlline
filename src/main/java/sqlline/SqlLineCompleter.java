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

/**
 * Completer for SQLLine. It dispatches to sub-completers based on the
 * current arguments.
 */
class SqlLineCompleter
    implements Completer {
  private SqlLine sqlLine;

  public SqlLineCompleter(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int complete(String buf, int pos, List<CharSequence> candidates) {
    if (buf != null
        && buf.startsWith(SqlLine.COMMAND_PREFIX)
        && !buf.startsWith(SqlLine.COMMAND_PREFIX + "all")
        && !buf.startsWith(SqlLine.COMMAND_PREFIX + "sql")) {
      return sqlLine.getCommandCompleter().complete(buf, pos, candidates);
    } else {
      if (sqlLine.getDatabaseConnection() != null
          && sqlLine.getDatabaseConnection().getSqlCompleter() != null) {
        return sqlLine.getDatabaseConnection().getSqlCompleter()
            .complete(buf, pos, candidates);
      } else {
        return -1;
      }
    }
  }
}

// End SqlLineCompleter.java
