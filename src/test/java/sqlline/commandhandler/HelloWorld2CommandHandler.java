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

import java.util.Collections;

import jline.console.completer.Completer;
import sqlline.AbstractCommandHandler;
import sqlline.DispatchCallback;
import sqlline.SqlLine;

/**
 * Hello world command handler test to check possibility to add
 * commands with the same names from different command handlers.
 */
public class HelloWorld2CommandHandler extends AbstractCommandHandler {

  public HelloWorld2CommandHandler(SqlLine sqlLine) {
    super(sqlLine, new String[]{"hello"}, "help for hello2",
        Collections.<Completer>emptyList());
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
}

// HelloWorld2CommandHandler.java
