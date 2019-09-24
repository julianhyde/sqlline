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

import sun.misc.*; // CHECKSTYLE: IGNORE 0

/**
 * A signal handler for SQLLine which interprets Ctrl+C as a request to cancel
 * the currently executing query. Adapted from
 * <a href="http://www.smotricz.com/kabutz/Issue043.html">TJSN</a>.
 */
public class SunSignalHandler implements SqlLineSignalHandler, SignalHandler {
  private DispatchCallback dispatchCallback;

  public SunSignalHandler() {
    Signal.handle(new Signal("INT"), this);
  }

  public void setCallback(DispatchCallback dispatchCallback) {
    this.dispatchCallback = dispatchCallback;
  }

  public void handle(Signal sig) {
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

// End SunSignalHandler.java
