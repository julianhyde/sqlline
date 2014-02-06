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

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Callback.
 */
public class DispatchCallback {
  private Status status;
  private Statement statement;

  public DispatchCallback() {
    this.status = Status.UNSET;
  }

  /**
   * Sets the sql statement the callback should keep track of so that it can
   * be canceled.
   *
   * @param statement the statement to track
   */
  public void trackSqlQuery(Statement statement) {
    this.statement = statement;
    status = Status.RUNNING;
  }

  public void setToSuccess() {
    status = Status.SUCCESS;
  }

  public boolean isSuccess() {
    return Status.SUCCESS == status;
  }

  public void setToFailure() {
    status = Status.FAILURE;
  }

  public boolean isFailure() {
    return Status.FAILURE == status;
  }

  public boolean isRunning() {
    return Status.RUNNING == status;
  }

  public void setToCancel() {
    status = Status.CANCELED;
  }

  public boolean isCanceled() {
    return Status.CANCELED == status;
  }

  /**
   * If a statement has been set by {@link #trackSqlQuery(java.sql.Statement)}
   * then calls {@link java.sql.Statement#cancel()} on it.
   * As with {@link java.sql.Statement#cancel()}
   * the effect of calling this is dependent on the underlying DBMS and
   * driver.
   *
   * @throws SQLException on database error
   */
  public void forceKillSqlQuery() throws SQLException {
    // regardless of whether it's necessary to actually call .cancel() set
    // the flag to indicate a cancel was requested so we can message the
    // interactive shell if we want. If there is something to cancel, cancel
    // it.
    setStatus(Status.CANCELED);
    if (null != statement) {
      statement.cancel();
    }
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  /** Status of a command. */
  enum Status {
    UNSET, RUNNING, SUCCESS, FAILURE, CANCELED
  }
}

// End DispatchCallback.java
