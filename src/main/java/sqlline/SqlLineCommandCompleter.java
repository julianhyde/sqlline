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

import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.utils.AttributedString;

/**
 * Suggests completions for a command.
 */
class SqlLineCommandCompleter extends AggregateCompleter {
  SqlLineCommandCompleter(SqlLine sqlLine) {
    super(new LinkedList<>());
    List<Completer> completers = new LinkedList<>();

    for (CommandHandler commandHandler : sqlLine.getCommandHandlers()) {
      for (String cmd : commandHandler.getNames()) {
        List<Completer> compl = new LinkedList<>();
        final List<Completer> parameterCompleters =
            commandHandler.getParameterCompleters();
        if (parameterCompleters.size() == 1
            && parameterCompleters.iterator().next()
                instanceof Completers.RegexCompleter) {
          completers.add(parameterCompleters.iterator().next());
        } else {
          final String commandName = SqlLine.COMMAND_PREFIX + cmd;
          final String helpText = commandHandler.getHelpText();
          final int firstEndOfLineIndex = helpText.indexOf('\n');
          compl.add(
              new StringsCompleter(
                  new SqlLineCandidate(sqlLine,
                      AttributedString.stripAnsi(commandName), commandName,
                      sqlLine.loc("command-name"),
                      firstEndOfLineIndex == -1
                          ? helpText
                          : helpText.substring(0, firstEndOfLineIndex),
                      // there could be whatever else instead helpText
                      // which is the same for commands with all theirs aliases
                      null, helpText, true)));
          compl.addAll(parameterCompleters);
          compl.add(new NullCompleter()); // last param no complete
          completers.add(new ArgumentCompleter(compl));
        }
      }
    }

    getCompleters().addAll(completers);
  }

  /**
   * Sqlline candidate showing description depending on
   * showCompletionDescr's property value.
   */
  static class SqlLineCandidate extends Candidate {
    private final SqlLine sqlLine;

    SqlLineCandidate(SqlLine sqlLine, String value, String displ,
        String group, String descr, String suffix,
        String key, boolean complete) {
      super(value, displ, group, descr, suffix, key, complete);
      this.sqlLine = sqlLine;
    }

    @Override public String descr() {
      if (sqlLine.getOpts().getShowCompletionDescr()) {
        return super.descr();
      }
      return null;
    }
  }
}

// End SqlLineCommandCompleter.java
