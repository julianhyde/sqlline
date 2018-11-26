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

import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import org.hsqldb.jdbc.JDBCDatabaseMetaData;
import org.junit.jupiter.api.Test;

import mockit.Expectations;
import mockit.Mocked;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for not supported {@link java.sql.DatabaseMetaData} methods.
 */
public class DatabaseMetaDataHandlerTest {

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

      final DatabaseMetaData wrapper =
          (DatabaseMetaData) Proxy.newProxyInstance(
            DatabaseMetaData.class.getClassLoader(),
            new Class[]{DatabaseMetaData.class},
            new DatabaseMetaDataHandler(meta));

      assertThat(wrapper.supportsAlterTableWithAddColumn(), is(false));
      assertThat(wrapper.isReadOnly(), is(false));
      assertThat(wrapper.supportsTransactionIsolationLevel(4), is(false));
      assertThat(wrapper.supportsANSI92FullSQL(), is(false));
      assertThat(wrapper.storesUpperCaseIdentifiers(), is(false));
      assertThat(wrapper.dataDefinitionIgnoredInTransactions(), is(false));
      assertThat(wrapper.storesMixedCaseQuotedIdentifiers(), is(false));
      assertThat(wrapper.allTablesAreSelectable(), is(false));
      assertThat(wrapper.allProceduresAreCallable(), is(false));
      assertThat(wrapper.nullsAreSortedHigh(), is(false));
      assertThat(wrapper.nullsAreSortedLow(), is(false));
      assertThat(wrapper.nullsAreSortedAtStart(), is(false));
      assertThat(wrapper.nullsAreSortedAtEnd(), is(false));
      assertThat(wrapper.nullPlusNonNullIsNull(), is(false));
      assertThat(wrapper.getMaxBinaryLiteralLength(), is(0));
      assertThat(wrapper.getMaxColumnsInIndex(), is(0));
      assertThat(wrapper.getIdentifierQuoteString(), is(" "));

    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

}

// End DatabaseMetaDataHandlerTest.java
