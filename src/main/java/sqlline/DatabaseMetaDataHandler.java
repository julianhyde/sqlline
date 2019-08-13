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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Invocation handler for {@link DatabaseMetaData}, to ensure that sqlline
 * does not fail in case the connection's JDBC driver
 * does not implement {@link DatabaseMetaData} correctly.
 */
class DatabaseMetaDataHandler implements InvocationHandler {
  private final DatabaseMetaData metaData;

  DatabaseMetaDataHandler(DatabaseMetaData metaData) {
    this.metaData = Objects.requireNonNull(metaData);
  }

  @Override public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    try {
      return method.invoke(metaData, args);
    } catch (Throwable e) {
      final MethodWithDefault methodWithDefault =
          MethodWithDefault.lookup(method);
      if (methodWithDefault != null) {
        return methodWithDefault.defaultValue;
      }
      throw e.getCause() instanceof SQLException ? e.getCause() : e;
    }
  }

  /** Defines all methods of {@link DatabaseMetaData} for which we wish to
   * provide a default value.
   *
   * <p>The name of each enum member corresponds to the method name. We assume
   * that there are no overloaded methods. */
  private enum MethodWithDefault {
    // the list is ordered
    allProceduresAreCallable(false),
    allTablesAreSelectable(false),
    autoCommitFailureClosesAllResultSets(false),
    dataDefinitionCausesTransactionCommit(false),
    deletesAreDetected(false, int.class),
    dataDefinitionIgnoredInTransactions(false),
    doesMaxRowSizeIncludeBlobs(false),
    generatedKeyAlwaysReturned(false),
    getDatabaseProductName(null),
    getIdentifierQuoteString(" "),
    getMaxBinaryLiteralLength(0),
    getMaxCatalogNameLength(0),
    getMaxCharLiteralLength(0),
    getMaxColumnNameLength(0),
    getMaxColumnsInGroupBy(0),
    getMaxColumnsInIndex(0),
    getMaxColumnsInOrderBy(0),
    getMaxColumnsInSelect(0),
    getMaxColumnsInTable(0),
    getMaxConnections(0),
    getMaxCursorNameLength(0),
    getMaxLogicalLobSize(0),
    getMaxIndexLength(0),
    getMaxProcedureNameLength(0),
    getMaxRowSize(0),
    getMaxSchemaNameLength(0),
    getMaxStatementLength(0),
    getMaxStatements(0),
    getMaxTableNameLength(0),
    getMaxTablesInSelect(0),
    getMaxUserNameLength(0),
    getNumericFunctions(""),
    getStringFunctions(""),
    getSQLKeywords(""),
    getSystemFunctions(""),
    getTimeDateFunctions(""),
    getURL(null),
    insertsAreDetected(false, int.class),
    isCatalogAtStart(false),
    isReadOnly(false),
    nullPlusNonNullIsNull(false),
    nullsAreSortedHigh(false),
    nullsAreSortedLow(false),
    nullsAreSortedAtStart(false),
    nullsAreSortedAtEnd(false),
    othersDeletesAreVisible(false, int.class),
    othersInsertsAreVisible(false, int.class),
    othersUpdatesAreVisible(false, int.class),
    ownDeletesAreVisible(false, int.class),
    ownInsertsAreVisible(false, int.class),
    ownUpdatesAreVisible(false, int.class),
    storesLowerCaseIdentifiers(false),
    storesLowerCaseQuotedIdentifiers(false),
    storesMixedCaseIdentifiers(false),
    storesMixedCaseQuotedIdentifiers(false),
    storesUpperCaseIdentifiers(false),
    storesUpperCaseQuotedIdentifiers(false),
    supportsAlterTableWithAddColumn(false),
    supportsAlterTableWithDropColumn(false),
    supportsANSI92EntryLevelSQL(false),
    supportsANSI92FullSQL(false),
    supportsANSI92IntermediateSQL(false),
    supportsBatchUpdates(false),
    supportsCatalogsInDataManipulation(false),
    supportsCatalogsInIndexDefinitions(false),
    supportsCatalogsInPrivilegeDefinitions(false),
    supportsCatalogsInProcedureCalls(false),
    supportsCatalogsInTableDefinitions(false),
    supportsColumnAliasing(false),
    supportsConvert(false),
    supportsCoreSQLGrammar(false),
    supportsCorrelatedSubqueries(false),
    supportsDataDefinitionAndDataManipulationTransactions(false),
    supportsDataManipulationTransactionsOnly(false),
    supportsExtendedSQLGrammar(false),
    supportsGroupBy(false),
    supportsFullOuterJoins(false),
    supportsGetGeneratedKeys(false),
    supportsGroupByBeyondSelect(false),
    supportsGroupByUnrelated(false),
    supportsIntegrityEnhancementFacility(false),
    supportsLikeEscapeClause(false),
    supportsLimitedOuterJoins(false),
    supportsMinimumSQLGrammar(false),
    supportsMixedCaseIdentifiers(false),
    supportsMultipleOpenResults(false),
    supportsMultipleResultSets(false),
    supportsMultipleTransactions(false),
    supportsDifferentTableCorrelationNames(false),
    supportsExpressionsInOrderBy(false),
    supportsMixedCaseQuotedIdentifiers(false),
    supportsNamedParameters(false),
    supportsNonNullableColumns(false),
    supportsOrderByUnrelated(false),
    supportsOuterJoins(false),
    supportsPositionedDelete(false),
    supportsPositionedUpdate(false),
    supportsRefCursors(false),
    supportsResultSetConcurrency(false, int.class, int.class),
    supportsResultSetHoldability(false, int.class),
    supportsResultSetType(false, int.class),
    supportsSavepoints(false),
    supportsSchemasInDataManipulation(false),
    supportsSchemasInIndexDefinitions(false),
    supportsSchemasInPrivilegeDefinitions(false),
    supportsSchemasInProcedureCalls(false),
    supportsSchemasInTableDefinitions(false),
    supportsSelectForUpdate(false),
    supportsStatementPooling(false),
    supportsStoredFunctionsUsingCallSyntax(false),
    supportsStoredProcedures(false),
    supportsSubqueriesInComparisons(false),
    supportsSubqueriesInExists(false),
    supportsSubqueriesInIns(false),
    supportsSubqueriesInQuantifieds(false),
    supportsTableCorrelationNames(false),
    supportsTransactionIsolationLevel(false, int.class),
    supportsTransactions(false),
    supportsUnion(false),
    supportsUnionAll(false),
    updatesAreDetected(false, int.class),
    usesLocalFiles(false),
    usesLocalFilePerTable(false);

    private final Method method;
    /** May be null, which means that null is the default value. */
    private final Object defaultValue;

    private static final Map<String, MethodWithDefault> MAP = new HashMap<>();

    static {
      for (MethodWithDefault methodWithDefault : MethodWithDefault.values()) {
        MAP.put(methodWithDefault.method.getName(), methodWithDefault);
      }
    }

    MethodWithDefault(Object defaultValue, Class... parameterTypes) {
      this.method = Objects.requireNonNull(findMethod(name(), parameterTypes));
      this.defaultValue = defaultValue;
    }

    static MethodWithDefault lookup(Method method) {
      return MAP.get(method.getName());
    }

    /** Looks up a method of {@link DatabaseMetaData}.
     *
     * <p>Throws {@link AssertionError} if method is not found.
     *
     * <p>Note: In future we may allow defining a default for a method that is
     * in some but not all JDK versions. For example, suppose that
     * {@code supportsFoo} is introduced in JDK 13 but we are running JDK 10.
     * This method will not find method {@code supportsFoo}, and a call to it
     * will not occur at runtime. But it is not an error, so  rather than
     * failing, this method should return a dummy method.
     *
     * @param methodName Method name
     * @param parameterTypes Parameter types
     * @return Method, never null
     */
    private static Method findMethod(String methodName,
        Class... parameterTypes) {
      try {
        return DatabaseMetaData.class.getDeclaredMethod(methodName,
            parameterTypes);
      } catch (NoSuchMethodException e) {
        throw new AssertionError("not found: " + methodName
            + Arrays.asList(parameterTypes), e);
      }
    }
  }
}

// End DatabaseMetaDataHandler.java
