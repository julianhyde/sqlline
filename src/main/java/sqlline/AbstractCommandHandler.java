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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.NullCompleter;

/**
 * An abstract implementation of CommandHandler.
 */
public abstract class AbstractCommandHandler implements CommandHandler {
  protected final SqlLine sqlLine;
  private final String name;
  private final List<String> names;
  private final String helpText;
  private final List<Completer> parameterCompleters;

  public AbstractCommandHandler(SqlLine sqlLine, String[] names,
      String helpText, List<Completer> completers) {
    this.sqlLine = sqlLine;
    name = names[0];
    this.names = Arrays.asList(names);
    this.helpText = helpText;
    if (completers == null || completers.size() == 0) {
      this.parameterCompleters =
          Collections.singletonList(new NullCompleter());
    } else {
      List<Completer> c = new ArrayList<>(completers);
      c.add(new NullCompleter());
      this.parameterCompleters = c;
    }
  }

  public String getHelpText() {
    return helpText;
  }

  public String getName() {
    return this.name;
  }

  public List<String> getNames() {
    return this.names;
  }

  public String matches(String line) {
    if (line == null || line.length() == 0) {
      return null;
    }

    String[] parts = sqlLine.split(line, 1);
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

  public List<Completer> getParameterCompleters() {
    return parameterCompleters;
  }
}

// End AbstractCommandHandler.java
