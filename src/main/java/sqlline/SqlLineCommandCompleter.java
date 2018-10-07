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

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Suggests completions for a command.
 */
class SqlLineCommandCompleter extends AggregateCompleter {
  SqlLineCommandCompleter(SqlLine sqlLine) {
    super(new LinkedList<>());
    List<ArgumentCompleter> completers = new LinkedList<>();

    for (CommandHandler commandHandler : sqlLine.getCommandHandlers()) {
      for (String cmd : commandHandler.getNames()) {
        List<Completer> compl = new LinkedList<>();
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
