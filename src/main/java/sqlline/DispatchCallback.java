/*
 * Copyright (c) 2007-2009 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package sqlline;

import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public class DispatchCallback
  {

  private Status status;
  private Statement statement = null;

  public DispatchCallback() {
    this.status = Status.UNSET;
  }

  /**
   * Sets the sql statement the callback should keep track of so that it can be canceled.
   * @param statement the statement to track
   */
  public void trackSqlQuery( Statement statement ) {
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

  /**
   * If a statement has been set by {@link #trackSqlQuery(java.sql.Statement)} then call {@link java.sql.Statement#cancel()}
   * on it.
   * @throws SQLException
   */
  public void forceKillSqlQuery() throws SQLException {
    // regardless of whether it's necessary to actually call .cancel() set the flag to indicate a cancel was requested
    // so we can message the interactive shell if we want. If there is something to cancel, cancel it.
    setStatus( Status.CANCELED );
    if ((null != statement) && (status == Status.RUNNING) && (!statement.isClosed())) {
      statement.cancel();
    }
  }

  public Status getStatus()
    {
    return status;
    }

  public void setStatus( Status status )
    {
    this.status = status;
    }

  enum Status{UNSET, RUNNING, SUCCESS, FAILURE, CANCELED}

  }
