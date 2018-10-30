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
import java.sql.SQLFeatureNotSupportedException;

import org.hsqldb.jdbc.JDBCDatabaseMetaData;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test cases for not supported {@link java.sql.DatabaseMetaData} methods.
 */
@RunWith(JMockit.class)
public class DatabaseMetaDataWrapperTest {

  @Test
  public void testExecutionWithNotSupportedMethods(
      @Mocked final JDBCDatabaseMetaData meta) {
    try {
      new Expectations() {
        {
          // methods returning boolean
          meta.supportsAlterTableWithAddColumn();
          result = new SQLException("Method not supported");

          meta.isReadOnly();
          result = new SQLException("Method not supported");

          meta.supportsTransactionIsolationLevel(4);
          result = new SQLException("Method not supported");

          meta.supportsANSI92FullSQL();
          result = new SQLException("Method not supported");

          meta.storesUpperCaseIdentifiers();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.dataDefinitionIgnoredInTransactions();
          result = new SQLException("Method not supported");

          meta.storesMixedCaseQuotedIdentifiers();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.allTablesAreSelectable();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.allProceduresAreCallable();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.nullsAreSortedHigh();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.nullsAreSortedLow();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.nullsAreSortedAtStart();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.nullsAreSortedAtEnd();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.nullPlusNonNullIsNull();
          result = new SQLFeatureNotSupportedException("Method not supported");

          //methods returning int
          meta.getMaxBinaryLiteralLength();
          result = new SQLFeatureNotSupportedException("Method not supported");

          meta.getMaxColumnsInIndex();
          result = new SQLException("Method not supported");

          //methods returning string
          meta.getIdentifierQuoteString();
          result = new SQLException("Method not supported");
        }
      };

      final DatabaseMetaDataWrapper wrapper =
          new DatabaseMetaDataWrapper(new SqlLine(), meta);

      assertFalse(wrapper.supportsAlterTableWithAddColumn());
      assertFalse(wrapper.isReadOnly());
      assertFalse(wrapper.supportsTransactionIsolationLevel(4));
      assertFalse(wrapper.supportsANSI92FullSQL());
      assertFalse(wrapper.storesUpperCaseIdentifiers());
      assertFalse(wrapper.dataDefinitionIgnoredInTransactions());
      assertFalse(wrapper.storesMixedCaseQuotedIdentifiers());
      assertFalse(wrapper.allTablesAreSelectable());
      assertFalse(wrapper.allProceduresAreCallable());
      assertFalse(wrapper.nullsAreSortedHigh());
      assertFalse(wrapper.nullsAreSortedLow());
      assertFalse(wrapper.nullsAreSortedAtStart());
      assertFalse(wrapper.nullsAreSortedAtEnd());
      assertFalse(wrapper.nullPlusNonNullIsNull());
      assertEquals(wrapper.getMaxBinaryLiteralLength(), 0);
      assertEquals(wrapper.getMaxColumnsInIndex(), 0);
      assertEquals(wrapper.getIdentifierQuoteString(), " ");

    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }
}

// End DatabaseMetaDataWrapperTest.java
