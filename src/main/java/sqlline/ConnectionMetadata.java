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

/**
 * Provides information about database connection
 * and its metadata without exposing internal API.
 * Intended to be used by {@link PromptHandler} class
 * and its sub-classes.
 */
public final class ConnectionMetadata {

  private final SqlLine sqlLine;

  public ConnectionMetadata(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public int getIndex() {
    return sqlLine.getDatabaseConnections().getIndex();
  }

  public String getDatabaseProductName() {
    DatabaseConnection connection = sqlLine.getDatabaseConnection();
    if (connection != null) {
      try {
        return connection.meta.getDatabaseProductName();
      } catch (Exception e) {
        // ignore
      }
    }
    return null;
  }

  public String getUserName() {
    DatabaseConnection connection = sqlLine.getDatabaseConnection();
    if (connection != null) {
      try {
        return connection.meta.getUserName();
      } catch (Exception e) {
        // ignore
      }
    }
    return null;
  }

  public String getUrl() {
    DatabaseConnection connection = sqlLine.getDatabaseConnection();
    if (connection != null) {
      return connection.getUrl();
    }
    return null;
  }

  public String getCurrentSchema() {
    DatabaseConnection connection = sqlLine.getDatabaseConnection();
    if (connection != null) {
      return connection.getCurrentSchema();
    }
    return null;
  }

}

// End ConnectionMetadata.java
