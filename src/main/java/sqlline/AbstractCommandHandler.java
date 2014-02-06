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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;

/**
 * An abstract implementation of CommandHandler.
 */
public abstract class AbstractCommandHandler implements CommandHandler {
  protected final SqlLine sqlLine;
  private final String name;
  private final String[] names;
  private final String helpText;
  private Completer[] parameterCompleters = new Completer[0];

  public AbstractCommandHandler(SqlLine sqlLine, String[] names,
      String helpText, Completer[] completers) {
    this.sqlLine = sqlLine;
    name = names[0];
    this.names = names;
    this.helpText = helpText;
    if (completers == null || completers.length == 0) {
      this.parameterCompleters = new Completer[] { new NullCompleter() };
    } else {
      List<Completer> c = new LinkedList<Completer>(Arrays.asList(completers));
      c.add(new NullCompleter());
      this.parameterCompleters = c.toArray(new Completer[0]);
    }
  }

  public String getHelpText() {
    return helpText;
  }

  public String getName() {
    return this.name;
  }

  public String[] getNames() {
    return this.names;
  }

  public String matches(String line) {
    if (line == null || line.length() == 0) {
      return null;
    }

    String[] parts = sqlLine.split(line);
    if (parts == null || parts.length == 0) {
      return null;
    }

    for (String name2 : names) {
      if (name2.startsWith(parts[0])) {
        return name2;
      }
    }

    return null;
  }

  public void setParameterCompleters(Completer[] parameterCompleters) {
    this.parameterCompleters = parameterCompleters;
  }

  public Completer[] getParameterCompleters() {
    return parameterCompleters;
  }
}

// End AbstractCommandHandler.java
