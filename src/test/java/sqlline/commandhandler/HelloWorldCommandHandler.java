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
 * Hello world command handler test.
 */
public class HelloWorldCommandHandler extends AbstractCommandHandler {

  public HelloWorldCommandHandler(SqlLine sqlLine) {
    super(sqlLine, new String[]{"hello", "test"}, "help for hello test",
        Collections.<Completer>emptyList());
  }

  @Override
  public void execute(String line, DispatchCallback dispatchCallback) {
    try {
      sqlLine.output("HELLO WORLD");
    } catch (Throwable e) {
      dispatchCallback.setToFailure();
      sqlLine.error(e);
      sqlLine.handleException(e);
    }
  }
}

// HelloWorldCommandHandler.java
