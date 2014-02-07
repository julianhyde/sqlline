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

import jline.console.completer.Completer;

/**
 * A {@link CommandHandler} implementation that
 * uses reflection to determine the method to dispatch the command.
 */
public class ReflectiveCommandHandler extends AbstractCommandHandler {
  public ReflectiveCommandHandler(SqlLine sqlLine, Completer[] completer,
      String... cmds) {
    super(sqlLine, cmds, sqlLine.loc("help-" + cmds[0]), completer);
  }

  public void execute(String line, DispatchCallback callback) {
    try {
      sqlLine.getCommands().getClass()
          .getMethod(getName(), String.class, DispatchCallback.class)
          .invoke(sqlLine.getCommands(), line, callback);
    } catch (Throwable e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }
}

// End ReflectiveCommandHandler.java
