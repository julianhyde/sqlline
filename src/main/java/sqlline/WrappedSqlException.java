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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * A {@link RuntimeException} that wraps a {@link SQLException}.
 *
 * <p>Classes like IncrementalRows which are iterable can then throw this
 * wrapped exception which can be handled as a SQLException
 * would be handled.
 */
class WrappedSqlException extends RuntimeException {

  /** Creates a WrappedSQLException. */
  WrappedSqlException(SQLException ex) {
    super(ex);
  }

  @Override public String getMessage() {
    return super.getMessage();
  }

  @Override public String getLocalizedMessage() {
    return super.getLocalizedMessage();
  }

  @Override public synchronized Throwable getCause() {
    return super.getCause();
  }

  @Override public synchronized Throwable initCause(Throwable cause) {
    return super.initCause(cause);
  }

  @Override public String toString() {
    return super.toString();
  }

  @Override public void printStackTrace() {
    super.printStackTrace();
  }

  @Override public void printStackTrace(PrintStream s) {
    super.printStackTrace(s);
  }

  @Override public void printStackTrace(PrintWriter s) {
    super.printStackTrace(s);
  }

  @Override public synchronized Throwable fillInStackTrace() {
    return super.fillInStackTrace();
  }

  @Override public StackTraceElement[] getStackTrace() {
    return super.getStackTrace();
  }

  @Override public void setStackTrace(StackTraceElement[] stackTrace) {
    super.setStackTrace(stackTrace);
  }
}

// End WrappedSqlException.java
