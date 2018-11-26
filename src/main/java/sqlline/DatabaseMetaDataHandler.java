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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * Invocation handler for {@link DatabaseMetaData}, to ensure that sqlline
 * does not fail in case the connection's JDBC driver
 * does not implement {@link DatabaseMetaData} correctly.
 */
class DatabaseMetaDataHandler implements InvocationHandler {
  private static final Map<String, Object> METHODS_DEFAULTS = new HashMap<>();

  static {
    // the list is ordered
    METHODS_DEFAULTS.put("allProceduresAreCallable", false);
    METHODS_DEFAULTS.put("allTablesAreSelectable", false);
    METHODS_DEFAULTS.put("autoCommitFailureClosesAllResultSets", false);
    METHODS_DEFAULTS.put("dataDefinitionCausesTransactionCommit", false);
    METHODS_DEFAULTS.put("deletesAreDetected", false);
    METHODS_DEFAULTS.put("dataDefinitionIgnoredInTransactions", false);
    METHODS_DEFAULTS.put("doesMaxRowSizeIncludeBlobs", false);
    METHODS_DEFAULTS.put("generatedKeyAlwaysReturned", false);
    METHODS_DEFAULTS.put("getDatabaseProductName", null);
    METHODS_DEFAULTS.put("getIdentifierQuoteString", " ");
    METHODS_DEFAULTS.put("getMaxBinaryLiteralLength", 0);
    METHODS_DEFAULTS.put("getMaxCatalogNameLength", 0);
    METHODS_DEFAULTS.put("getMaxCharLiteralLength", 0);
    METHODS_DEFAULTS.put("getMaxColumnNameLength", 0);
    METHODS_DEFAULTS.put("getMaxColumnsInGroupBy", 0);
    METHODS_DEFAULTS.put("getMaxColumnsInIndex", 0);
    METHODS_DEFAULTS.put("getMaxColumnsInOrderBy", 0);
    METHODS_DEFAULTS.put("getMaxColumnsInSelect", 0);
    METHODS_DEFAULTS.put("getMaxColumnsInTable", 0);
    METHODS_DEFAULTS.put("getMaxConnections", 0);
    METHODS_DEFAULTS.put("getMaxCursorNameLength", 0);
    METHODS_DEFAULTS.put("getMaxLogicalLobSize", 0);
    METHODS_DEFAULTS.put("getMaxIndexLength", 0);
    METHODS_DEFAULTS.put("getMaxProcedureNameLength", 0);
    METHODS_DEFAULTS.put("getMaxRowSize", 0);
    METHODS_DEFAULTS.put("getMaxSchemaNameLength", 0);
    METHODS_DEFAULTS.put("getMaxStatementLength", 0);
    METHODS_DEFAULTS.put("getMaxStatements", 0);
    METHODS_DEFAULTS.put("getMaxTableNameLength", 0);
    METHODS_DEFAULTS.put("getMaxTablesInSelect", 0);
    METHODS_DEFAULTS.put("getMaxUserNameLength", 0);
    METHODS_DEFAULTS.put("getNumericFunctions", "");
    METHODS_DEFAULTS.put("getStringFunctions", "");
    METHODS_DEFAULTS.put("getSQLKeywords", "");
    METHODS_DEFAULTS.put("getSystemFunctions", "");
    METHODS_DEFAULTS.put("getTimeDateFunctions", "");
    METHODS_DEFAULTS.put("getURL", null);
    METHODS_DEFAULTS.put("insertsAreDetected", false);
    METHODS_DEFAULTS.put("isCatalogAtStart", false);
    METHODS_DEFAULTS.put("isReadOnly", false);
    METHODS_DEFAULTS.put("nullPlusNonNullIsNull", false);
    METHODS_DEFAULTS.put("nullsAreSortedHigh", false);
    METHODS_DEFAULTS.put("nullsAreSortedLow", false);
    METHODS_DEFAULTS.put("nullsAreSortedAtStart", false);
    METHODS_DEFAULTS.put("nullsAreSortedAtEnd", false);
    METHODS_DEFAULTS.put("othersDeletesAreVisible", false);
    METHODS_DEFAULTS.put("othersInsertsAreVisible", false);
    METHODS_DEFAULTS.put("othersUpdatesAreVisible", false);
    METHODS_DEFAULTS.put("ownDeletesAreVisible", false);
    METHODS_DEFAULTS.put("ownInsertsAreVisible", false);
    METHODS_DEFAULTS.put("ownUpdatesAreVisible", false);
    METHODS_DEFAULTS.put("storesLowerCaseIdentifiers", false);
    METHODS_DEFAULTS.put("storesLowerCaseQuotedIdentifiers", false);
    METHODS_DEFAULTS.put("storesMixedCaseIdentifiers", false);
    METHODS_DEFAULTS.put("storesMixedCaseQuotedIdentifiers", false);
    METHODS_DEFAULTS.put("storesUpperCaseIdentifiers", false);
    METHODS_DEFAULTS.put("storesUpperCaseQuotedIdentifiers", false);
    METHODS_DEFAULTS.put("supportsAlterTableWithAddColumn", false);
    METHODS_DEFAULTS.put("supportsAlterTableWithDropColumn", false);
    METHODS_DEFAULTS.put("supportsANSI92EntryLevelSQL", false);
    METHODS_DEFAULTS.put("supportsANSI92FullSQL", false);
    METHODS_DEFAULTS.put("supportsANSI92IntermediateSQL", false);
    METHODS_DEFAULTS.put("supportsBatchUpdates", false);
    METHODS_DEFAULTS.put("supportsCatalogsInDataManipulation", false);
    METHODS_DEFAULTS.put("supportsCatalogsInIndexDefinitions", false);
    METHODS_DEFAULTS.put("supportsCatalogsInPrivilegeDefinitions", false);
    METHODS_DEFAULTS.put("supportsCatalogsInProcedureCalls", false);
    METHODS_DEFAULTS.put("supportsCatalogsInTableDefinitions", false);
    METHODS_DEFAULTS.put("supportsColumnAliasing", false);
    METHODS_DEFAULTS.put("supportsConvert", false);
    METHODS_DEFAULTS.put("supportsCoreSQLGrammar", false);
    METHODS_DEFAULTS.put("supportsCorrelatedSubqueries", false);
    METHODS_DEFAULTS
        .put("supportsDataDefinitionAndDataManipulationTransactions", false);
    METHODS_DEFAULTS
        .put("supportsDataManipulationTransactionsOnly", false);
    METHODS_DEFAULTS.put("supportsExtendedSQLGrammar", false);
    METHODS_DEFAULTS.put("supportsGroupBy", false);
    METHODS_DEFAULTS.put("supportsFullOuterJoins", false);
    METHODS_DEFAULTS.put("supportsGetGeneratedKeys", false);
    METHODS_DEFAULTS.put("supportsGroupByBeyondSelect", false);
    METHODS_DEFAULTS.put("supportsGroupByUnrelated", false);
    METHODS_DEFAULTS.put("supportsIntegrityEnhancementFacility", false);
    METHODS_DEFAULTS.put("supportsLikeEscapeClause", false);
    METHODS_DEFAULTS.put("supportsLimitedOuterJoins", false);
    METHODS_DEFAULTS.put("supportsMinimumSQLGrammar", false);
    METHODS_DEFAULTS.put("supportsMixedCaseIdentifiers", false);
    METHODS_DEFAULTS.put("supportsMultipleOpenResults", false);
    METHODS_DEFAULTS.put("supportsMultipleResultSets", false);
    METHODS_DEFAULTS.put("supportsMultipleTransactions", false);
    METHODS_DEFAULTS.put("supportsDifferentTableCorrelationNames", false);
    METHODS_DEFAULTS.put("supportsExpressionsInOrderBy", false);
    METHODS_DEFAULTS.put("supportsMixedCaseQuotedIdentifiers", false);
    METHODS_DEFAULTS.put("supportsNamedParameters", false);
    METHODS_DEFAULTS.put("supportsNonNullableColumns", false);
    METHODS_DEFAULTS.put("supportsOrderByUnrelated", false);
    METHODS_DEFAULTS.put("supportsOuterJoins", false);
    METHODS_DEFAULTS.put("supportsPositionedDelete", false);
    METHODS_DEFAULTS.put("supportsPositionedUpdate", false);
    METHODS_DEFAULTS.put("supportsRefCursors", false);
    METHODS_DEFAULTS.put("supportsResultSetConcurrency", false);
    METHODS_DEFAULTS.put("supportsResultSetHoldability", false);
    METHODS_DEFAULTS.put("supportsResultSetType", false);
    METHODS_DEFAULTS.put("supportsSavepoints", false);
    METHODS_DEFAULTS.put("supportsSchemasInDataManipulation", false);
    METHODS_DEFAULTS.put("supportsSchemasInIndexDefinitions", false);
    METHODS_DEFAULTS.put("supportsSchemasInPrivilegeDefinitions", false);
    METHODS_DEFAULTS.put("supportsSchemasInProcedureCalls", false);
    METHODS_DEFAULTS.put("supportsSchemasInTableDefinitions", false);
    METHODS_DEFAULTS.put("supportsSelectForUpdate", false);
    METHODS_DEFAULTS.put("supportsStatementPooling", false);
    METHODS_DEFAULTS.put("supportsStoredFunctionsUsingCallSyntax", false);
    METHODS_DEFAULTS.put("supportsStoredProcedures", false);
    METHODS_DEFAULTS.put("supportsSubqueriesInComparisons", false);
    METHODS_DEFAULTS.put("supportsSubqueriesInExists", false);
    METHODS_DEFAULTS.put("supportsSubqueriesInIns", false);
    METHODS_DEFAULTS.put("supportsSubqueriesInQuantifieds", false);
    METHODS_DEFAULTS.put("supportsTableCorrelationNames", false);
    METHODS_DEFAULTS.put("supportsTransactionIsolationLevel", false);
    METHODS_DEFAULTS.put("supportsTransactions", false);
    METHODS_DEFAULTS.put("supportsUnion", false);
    METHODS_DEFAULTS.put("supportsUnionAll", false);
    METHODS_DEFAULTS.put("updatesAreDetected", false);
    METHODS_DEFAULTS.put("usesLocalFiles", false);
    METHODS_DEFAULTS.put("usesLocalFilePerTable", false);
  }

  private final DatabaseMetaData metaData;

  DatabaseMetaDataHandler(DatabaseMetaData metaData) {
    this.metaData = metaData;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    try {
      return method.invoke(metaData, args);
    } catch (Throwable t) {
      if (METHODS_DEFAULTS.containsKey(method.getName())) {
        return METHODS_DEFAULTS.get(method.getName());
      }
      throw t;
    }
  }
}

// End DatabaseMetaDataHandler.java
