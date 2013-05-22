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

import sun.misc.*;


/**
 * A signal handler for SqlLine which interprets Ctrl+C as a request to cancel
 * the currently executing query. Adapted from <a
 * href="http://www.smotricz.com/kabutz/Issue043.html">TJSN</a>.
 */
class SunSignalHandler
    implements SqlLineSignalHandler,
        SignalHandler
{
    //~ Instance fields --------------------------------------------------------

    private Statement stmt = null;

    //~ Constructors -----------------------------------------------------------

    SunSignalHandler()
    {
        Signal.handle(new Signal("INT"), this);
    }

    //~ Methods ----------------------------------------------------------------

    // implement SqlLineSignalHandler
    public void setStmt(Statement stmt)
    {
        this.stmt = stmt;
    }

    // implement sun.misc.SignalHandler
    public void handle(Signal sig)
    {
        try {
            synchronized (this) {
                if (stmt != null) {
                    stmt.cancel();
                    stmt = null;
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException( ex ) ;
        }
    }
}

// End SunSignalHandler.java
