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
package org.hsqldb.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Custom implementation of {@link java.sql.DatabaseMetaData} to reproduce issue
 * [<a href="https://github.com/julianhyde/sqlline/issues/295">SQLLINE-295</a>].
 */
public class CustomDatabaseMetadata extends JDBCDatabaseMetaData {
  public CustomDatabaseMetadata(JDBCConnection c) throws SQLException {
    super(c);
  }

  @Override public ResultSet getPrimaryKeys(String catalog, String schema,
      String table) throws SQLException {
    throw new SQLException("SQL Exception");
  }
}

// End CustomDatabaseMetadata.java
