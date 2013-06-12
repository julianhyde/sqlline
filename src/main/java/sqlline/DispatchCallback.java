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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Callback.
 */
public class DispatchCallback
{
    private static final Method IS_CLOSED_METHOD =
        getMethod(Statement.class, "isClosed");

    private Status status;
    private Statement statement;

    public DispatchCallback()
    {
        this.status = Status.UNSET;
    }

    /**
     * Sets the sql statement the callback should keep track of so that it can
     * be canceled.
     *
     * @param statement the statement to track
     */
    public void trackSqlQuery(Statement statement)
    {
        this.statement = statement;
        status = Status.RUNNING;
    }

    public void setToSuccess()
    {
        status = Status.SUCCESS;
    }

    public boolean isSuccess()
    {
        return Status.SUCCESS == status;
    }

    public void setToFailure()
    {
        status = Status.FAILURE;
    }

    public boolean isFailure()
    {
        return Status.FAILURE == status;
    }

    public boolean isRunning()
    {
        return Status.RUNNING == status;
    }

    public void setToCancel()
    {
         status = Status.CANCELED;
    }

    public boolean isCanceled()
    {
      return Status.CANCELED == status;
    }

    /**
     * If a statement has been set by {@link #trackSqlQuery(java.sql.Statement)}
     * then call {@link java.sql.Statement#cancel()} on it.
     *
     * @throws SQLException
     */
    public void forceKillSqlQuery() throws SQLException
    {
        // regardless of whether it's necessary to actually call .cancel() set
        // the flag to indicate a cancel was requested so we can message the
        // interactive shell if we want. If there is something to cancel, cancel
        // it.
        setStatus(Status.CANCELED);
        if ((null != statement)
            && (status == Status.RUNNING)
            && (!statementIsClosed(statement)))
        {
            statement.cancel();
        }
    }

    /** Calls {@link Statement#isClosed} via reflection. Before JDK 1.6, the
     * method does not exist, so this method returns false. */
    private static boolean statementIsClosed(Statement statement)
        throws SQLException
    {
        if (IS_CLOSED_METHOD != null) {
            try {
                return (Boolean) IS_CLOSED_METHOD.invoke(statement);
            } catch (IllegalAccessException e) {
                // ignore
            } catch (InvocationTargetException e) {
                // ignore
            }
        }
        return false;
    }

    /** Looks up a method, and returns null if the method does not exist. */
    private static Method getMethod(Class<?> clazz, String name) {
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    enum Status
    {
        UNSET, RUNNING, SUCCESS, FAILURE, CANCELED
    }
}

// End DispatchCallback.java
