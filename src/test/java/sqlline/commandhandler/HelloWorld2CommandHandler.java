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
package sqlline.commandhandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jline.console.completer.Completer;
import sqlline.CommandHandler;
import sqlline.DispatchCallback;
import sqlline.SqlLine;

/**
 * Hello world command handler .
 */
public class HelloWorld2CommandHandler implements CommandHandler {
  private static final String[] NAMES = new String[] {"hello"};
  private final SqlLine sqlLine;

  public HelloWorld2CommandHandler(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  @Override
  public String getName() {
    return "hello";
  }

  @Override
  public List<String> getNames() {
    return Arrays.asList(NAMES);
  }

  @Override
  public String getHelpText() {
    return "help for hello2";
  }

  @Override
  public String matches(String line) {
    if (line == null || line.length() == 0) {
      return null;
    }

    String[] parts = line.split(" ", 2);
    if (parts.length < 1) {
      return null;
    }

    for (String name2 : NAMES) {
      if (name2.startsWith(parts[0])) {
        return name2;
      }
    }

    return null;
  }

  @Override
  public void execute(String line, DispatchCallback dispatchCallback) {
    try {
      sqlLine.output("HELLO WORLD2");
    } catch (Throwable e) {
      dispatchCallback.setToFailure();
      sqlLine.error(e);
      sqlLine.handleException(e);
    }
  }

  @Override
  public List<Completer> getParameterCompleters() {
    return Collections.emptyList();
  }
}

// HelloWorld2CommandHandler.java
