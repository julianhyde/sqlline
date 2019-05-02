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

import java.sql.*;

import org.jline.terminal.Terminal;

/**
 * A signal handler for SQLLine that interprets Ctrl-C as a request to cancel
 * the currently executing query.
 */
public class SqlLineSignalHandler implements Terminal.SignalHandler {
  private DispatchCallback dispatchCallback;

  /**
   * Sets the dispatch callback to be alerted by signals.
   *
   * @param dispatchCallback statement affected
   */
  public void setCallback(DispatchCallback dispatchCallback) {
    this.dispatchCallback = dispatchCallback;
  }

  public void handle(Terminal.Signal sig) {
    try {
      synchronized (this) {
        if (dispatchCallback != null) {
          dispatchCallback.forceKillSqlQuery();
          dispatchCallback.setToCancel();
        }
      }
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}

// End SqlLineSignalHandler.java
