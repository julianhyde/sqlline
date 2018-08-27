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

import java.util.LinkedList;
import java.util.List;

import jline.console.completer.*;

/**
 * Suggests completions for a command.
 */
class SqlLineCommandCompleter extends AggregateCompleter {
  public SqlLineCommandCompleter(SqlLine sqlLine) {
    List<ArgumentCompleter> completers = new LinkedList<ArgumentCompleter>();

    for (CommandHandler commandHandler : sqlLine.getCommandHandlers()) {
      for (String cmd : commandHandler.getNames()) {
        List<Completer> compl = new LinkedList<Completer>();
        compl.add(new StringsCompleter(SqlLine.COMMAND_PREFIX + cmd));
        compl.addAll(commandHandler.getParameterCompleters());
        compl.add(new NullCompleter()); // last param no complete

        completers.add(new ArgumentCompleter(compl));
      }
    }

    getCompleters().addAll(completers);
  }
}

// End SqlLineCommandCompleter.java
