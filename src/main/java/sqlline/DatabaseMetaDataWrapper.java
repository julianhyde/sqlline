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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * Wrapper around {@link DatabaseMetaData}, to ensure that sqlline
 * does not fail in case the connection's JDBC driver
 * does not implement {@link DatabaseMetaData} correctly.
 */
class DatabaseMetaDataWrapper {
  private static final String METHOD_NOT_SUPPORTED = "Method not supported";

  private final SqlLine sqlLine;
  private final DatabaseMetaData metaData;

  DatabaseMetaDataWrapper(SqlLine sqlLine, DatabaseMetaData metaData) {
    this.metaData = metaData;
    this.sqlLine = sqlLine;
  }

  public boolean allProceduresAreCallable() throws SQLException {
    return getBooleanOrDefault("allProceduresAreCallable", false);
  }

  public boolean allTablesAreSelectable() throws SQLException {
    return getBooleanOrDefault("allTablesAreSelectable", false);
  }

  public String getURL() throws SQLException {
    return getStringOrDefault("getURL", null);
  }

  public String getUserName() throws SQLException {
    return getString("getUserName");
  }

  public boolean isReadOnly() throws SQLException {
    return getBooleanOrDefault("isReadOnly", false);
  }

  public boolean nullsAreSortedHigh() throws SQLException {
    return getBooleanOrDefault("nullsAreSortedHigh", false);
  }

  public boolean nullsAreSortedLow() throws SQLException {
    return getBooleanOrDefault("nullsAreSortedLow", false);
  }

  public boolean nullsAreSortedAtStart() throws SQLException {
    return getBooleanOrDefault("nullsAreSortedAtStart", false);
  }

  public boolean nullsAreSortedAtEnd() throws SQLException {
    return getBooleanOrDefault("nullsAreSortedAtEnd", false);
  }

  /**
   * Retrieves the name of this database product.
   *
   * @return database product name or null
   *         if the method is not supported by the driver.
   * @throws SQLException if a database access error occurs
   */
  public String getDatabaseProductName() throws SQLException {
    return getStringOrDefault("getDatabaseProductName", null);
  }

  public String getDatabaseProductVersion() throws SQLException {
    return getString("getDatabaseProductVersion");
  }

  public String getDriverName() throws SQLException {
    return getString("getDriverName");
  }

  public String getDriverVersion() throws SQLException {
    return getString("getDriverVersion");
  }

  public int getDriverMajorVersion() {
    try {
      return getInt("getDriverMajorVersion");
    } catch (SQLException e) {
      // should not happen as per
      // java.sql.DatabaseMetaData.getDriverMajorVersion
      throw new RuntimeException(e);
    }
  }

  public int getDriverMinorVersion() {
    try {
      return getInt("getDriverMinorVersion");
    } catch (SQLException e) {
      // should not happen as per
      // java.sql.DatabaseMetaData.getDriverMinorVersion
      throw new RuntimeException(e);
    }
  }

  public boolean usesLocalFiles() throws SQLException {
    return getBooleanOrDefault("usesLocalFiles", false);
  }

  public boolean usesLocalFilePerTable() throws SQLException {
    return getBooleanOrDefault("usesLocalFilePerTable", false);
  }

  public boolean supportsMixedCaseIdentifiers() throws SQLException {
    return getBooleanOrDefault("supportsMixedCaseIdentifiers", false);
  }

  public boolean storesUpperCaseIdentifiers() throws SQLException {
    return getBooleanOrDefault("storesUpperCaseIdentifiers", false);
  }

  public boolean storesLowerCaseIdentifiers() throws SQLException {
    return getBooleanOrDefault("storesLowerCaseIdentifiers", false);
  }

  public boolean storesMixedCaseIdentifiers() throws SQLException {
    return getBooleanOrDefault("storesMixedCaseIdentifiers", false);
  }

  public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
    return getBooleanOrDefault("supportsMixedCaseQuotedIdentifiers", false);
  }

  public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
    return getBooleanOrDefault("storesUpperCaseQuotedIdentifiers", false);
  }

  public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
    return getBooleanOrDefault("storesLowerCaseQuotedIdentifiers", false);
  }

  public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
    return getBooleanOrDefault("storesMixedCaseQuotedIdentifiers", false);
  }

  public String getIdentifierQuoteString() throws SQLException {
    return getStringOrDefault("getIdentifierQuoteString", " ");
  }

  /**
   * Calls for {@link DatabaseMetaData#getSQLKeywords}.
   *
   * @return Empty string in case it is not supported (e.g. Apache Hive)
   * @throws SQLException if a database access error occurs
   */
  public String getSQLKeywords() throws SQLException {
    return getStringOrDefault("getSQLKeywords", "");
  }

  /**
   * Calls for {@link DatabaseMetaData#getNumericFunctions}.
   *
   * @return Empty string in case it is not supported
   * @throws SQLException if a database access error occurs
   */
  public String getNumericFunctions() throws SQLException {
    return getStringOrDefault("getNumericFunctions", "");
  }

  /**
   * Calls for {@link DatabaseMetaData#getStringFunctions}.
   *
   * @return Empty string in case it is not supported
   * @throws SQLException if a database access error occurs
   */
  public String getStringFunctions() throws SQLException {
    return getStringOrDefault("getStringFunctions", "");
  }

  /**
   * Calls for {@link DatabaseMetaData#getSystemFunctions}.
   *
   * @return Empty string in case it is not supported
   * @throws SQLException if a database access error occurs
   */
  public String getSystemFunctions() throws SQLException {
    return getStringOrDefault("getSystemFunctions", "");
  }

  /**
   * Calls for {@link DatabaseMetaData#getTimeDateFunctions}.
   *
   * @return Empty string in case it is not supported
   * @throws SQLException if a database access error occurs
   */
  public String getTimeDateFunctions() throws SQLException {
    return getStringOrDefault("getTimeDateFunctions", "");
  }

  public String getSearchStringEscape() throws SQLException {
    return getString("getSearchStringEscape");
  }

  public String getExtraNameCharacters() throws SQLException {
    return getString("getExtraNameCharacters");
  }

  public boolean supportsAlterTableWithAddColumn() throws SQLException {
    return getBooleanOrDefault("supportsAlterTableWithAddColumn", false);
  }

  public boolean supportsAlterTableWithDropColumn() throws SQLException {
    return getBooleanOrDefault("supportsAlterTableWithDropColumn", false);
  }

  public boolean supportsColumnAliasing() throws SQLException {
    return getBooleanOrDefault("supportsColumnAliasing", false);
  }

  public boolean nullPlusNonNullIsNull() throws SQLException {
    return getBooleanOrDefault("nullPlusNonNullIsNull", false);
  }

  public boolean supportsConvert() throws SQLException {
    return getBooleanOrDefault("supportsConvert", false);
  }

  public boolean supportsConvert(int fromType, int toType) throws SQLException {
    return getBooleanOrDefault("supportsConvert", false, fromType, toType);
  }

  public boolean supportsTableCorrelationNames() throws SQLException {
    return getBooleanOrDefault("supportsTableCorrelationNames", false);
  }

  public boolean supportsDifferentTableCorrelationNames() throws SQLException {
    return getBooleanOrDefault("supportsDifferentTableCorrelationNames", false);
  }

  public boolean supportsExpressionsInOrderBy() throws SQLException {
    return getBooleanOrDefault("supportsExpressionsInOrderBy", false);
  }

  public boolean supportsOrderByUnrelated() throws SQLException {
    return getBooleanOrDefault("supportsOrderByUnrelated", false);
  }

  public boolean supportsGroupBy() throws SQLException {
    return getBooleanOrDefault("supportsGroupBy", false);
  }

  public boolean supportsGroupByUnrelated() throws SQLException {
    return getBooleanOrDefault("supportsGroupByUnrelated", false);
  }

  public boolean supportsGroupByBeyondSelect() throws SQLException {
    return getBooleanOrDefault("supportsGroupByBeyondSelect", false);
  }

  public boolean supportsLikeEscapeClause() throws SQLException {
    return getBooleanOrDefault("supportsLikeEscapeClause", false);
  }

  public boolean supportsMultipleResultSets() throws SQLException {
    return getBooleanOrDefault("supportsMultipleResultSets", false);
  }

  public boolean supportsMultipleTransactions() throws SQLException {
    return getBooleanOrDefault("supportsMultipleTransactions", false);
  }

  public boolean supportsNonNullableColumns() throws SQLException {
    return getBooleanOrDefault("supportsNonNullableColumns", false);
  }

  public boolean supportsMinimumSQLGrammar() throws SQLException {
    return getBooleanOrDefault("supportsMinimumSQLGrammar", false);
  }

  public boolean supportsCoreSQLGrammar() throws SQLException {
    return getBooleanOrDefault("supportsCoreSQLGrammar", false);
  }

  public boolean supportsExtendedSQLGrammar() throws SQLException {
    return getBooleanOrDefault("supportsExtendedSQLGrammar", false);
  }

  public boolean supportsANSI92EntryLevelSQL() throws SQLException {
    return getBooleanOrDefault("supportsANSI92EntryLevelSQL", false);
  }

  public boolean supportsANSI92IntermediateSQL() throws SQLException {
    return getBooleanOrDefault("supportsANSI92IntermediateSQL", false);
  }

  public boolean supportsANSI92FullSQL() throws SQLException {
    return getBooleanOrDefault("supportsANSI92FullSQL", false);
  }

  public boolean supportsIntegrityEnhancementFacility() throws SQLException {
    return getBooleanOrDefault("supportsIntegrityEnhancementFacility", false);
  }

  public boolean supportsOuterJoins() throws SQLException {
    return getBooleanOrDefault("supportsOuterJoins", false);
  }

  public boolean supportsFullOuterJoins() throws SQLException {
    return getBooleanOrDefault("supportsFullOuterJoins", false);
  }

  public boolean supportsLimitedOuterJoins() throws SQLException {
    return getBooleanOrDefault("supportsLimitedOuterJoins", false);
  }

  public String getSchemaTerm() throws SQLException {
    return getString("getSchemaTerm");
  }

  public String getProcedureTerm() throws SQLException {
    return getString("getProcedureTerm");
  }

  public String getCatalogTerm() throws SQLException {
    return getString("getCatalogTerm");
  }

  public boolean isCatalogAtStart() throws SQLException {
    return getBooleanOrDefault("isCatalogAtStart", false);
  }

  public String getCatalogSeparator() throws SQLException {
    return getString("getCatalogSeparator");
  }

  public boolean supportsSchemasInDataManipulation() throws SQLException {
    return getBooleanOrDefault("supportsSchemasInDataManipulation", false);
  }

  public boolean supportsSchemasInProcedureCalls() throws SQLException {
    return getBooleanOrDefault("supportsSchemasInProcedureCalls", false);
  }

  public boolean supportsSchemasInTableDefinitions() throws SQLException {
    return getBooleanOrDefault("supportsSchemasInTableDefinitions", false);
  }

  public boolean supportsSchemasInIndexDefinitions() throws SQLException {
    return getBooleanOrDefault("supportsSchemasInIndexDefinitions", false);
  }

  public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
    return getBooleanOrDefault("supportsSchemasInPrivilegeDefinitions", false);
  }

  public boolean supportsCatalogsInDataManipulation() throws SQLException {
    return getBooleanOrDefault("supportsCatalogsInDataManipulation", false);
  }

  public boolean supportsCatalogsInProcedureCalls() throws SQLException {
    return getBooleanOrDefault("supportsCatalogsInProcedureCalls", false);
  }

  public boolean supportsCatalogsInTableDefinitions() throws SQLException {
    return getBooleanOrDefault("supportsCatalogsInTableDefinitions", false);
  }

  public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
    return getBooleanOrDefault("supportsCatalogsInIndexDefinitions", false);
  }

  public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
    return getBooleanOrDefault("supportsCatalogsInPrivilegeDefinitions", false);
  }

  public boolean supportsPositionedDelete() throws SQLException {
    return getBooleanOrDefault("supportsPositionedDelete", false);
  }

  public boolean supportsPositionedUpdate() throws SQLException {
    return getBooleanOrDefault("supportsPositionedUpdate", false);
  }

  public boolean supportsSelectForUpdate() throws SQLException {
    return getBooleanOrDefault("supportsSelectForUpdate", false);
  }

  public boolean supportsStoredProcedures() throws SQLException {
    return getBooleanOrDefault("supportsStoredProcedures", false);
  }

  public boolean supportsSubqueriesInComparisons() throws SQLException {
    return getBooleanOrDefault("supportsSubqueriesInComparisons", false);
  }

  public boolean supportsSubqueriesInExists() throws SQLException {
    return getBooleanOrDefault("supportsSubqueriesInExists", false);
  }

  public boolean supportsSubqueriesInIns() throws SQLException {
    return getBooleanOrDefault("supportsSubqueriesInIns", false);
  }

  public boolean supportsSubqueriesInQuantifieds() throws SQLException {
    return getBooleanOrDefault("supportsSubqueriesInQuantifieds", false);
  }

  public boolean supportsCorrelatedSubqueries() throws SQLException {
    return getBooleanOrDefault("supportsCorrelatedSubqueries", false);
  }

  public boolean supportsUnion() throws SQLException {
    return getBooleanOrDefault("supportsUnion", false);
  }

  public boolean supportsUnionAll() throws SQLException {
    return getBooleanOrDefault("supportsUnionAll", false);
  }

  public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
    return getBoolean("supportsOpenCursorsAcrossCommit");
  }

  public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
    return getBoolean("supportsOpenCursorsAcrossRollback");
  }

  public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
    return getBoolean("supportsOpenStatementsAcrossCommit");
  }

  public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
    return getBoolean("supportsOpenStatementsAcrossRollback");
  }

  public int getMaxBinaryLiteralLength() throws SQLException {
    return getIntOrDefault("getMaxBinaryLiteralLength", 0);
  }

  public int getMaxCharLiteralLength() throws SQLException {
    return getIntOrDefault("getMaxCharLiteralLength", 0);
  }

  public int getMaxColumnNameLength() throws SQLException {
    return getIntOrDefault("getMaxColumnNameLength", 0);
  }

  public int getMaxColumnsInGroupBy() throws SQLException {
    return getIntOrDefault("getMaxColumnsInGroupBy", 0);
  }

  public int getMaxColumnsInIndex() throws SQLException {
    return getIntOrDefault("getMaxColumnsInIndex", 0);
  }

  public int getMaxColumnsInOrderBy() throws SQLException {
    return getIntOrDefault("getMaxColumnsInOrderBy", 0);
  }

  public int getMaxColumnsInSelect() throws SQLException {
    return getIntOrDefault("getMaxColumnsInSelect", 0);
  }

  public int getMaxColumnsInTable() throws SQLException {
    return getIntOrDefault("getMaxColumnsInTable", 0);
  }

  public int getMaxConnections() throws SQLException {
    return getIntOrDefault("getMaxConnections", 0);
  }

  public int getMaxCursorNameLength() throws SQLException {
    return getIntOrDefault("getMaxCursorNameLength", 0);
  }

  public int getMaxIndexLength() throws SQLException {
    return getIntOrDefault("getMaxIndexLength", 0);
  }

  public int getMaxSchemaNameLength() throws SQLException {
    return getIntOrDefault("getMaxSchemaNameLength", 0);
  }

  public int getMaxProcedureNameLength() throws SQLException {
    return getIntOrDefault("getMaxProcedureNameLength", 0);
  }

  public int getMaxCatalogNameLength() throws SQLException {
    return getIntOrDefault("getMaxCatalogNameLength", 0);
  }

  public int getMaxRowSize() throws SQLException {
    return getIntOrDefault("getMaxRowSize", 0);
  }

  public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
    return getBooleanOrDefault("doesMaxRowSizeIncludeBlobs", false);
  }

  public int getMaxStatementLength() throws SQLException {
    return getIntOrDefault("getMaxStatementLength", 0);
  }

  public int getMaxStatements() throws SQLException {
    return getIntOrDefault("getMaxStatements", 0);
  }

  public int getMaxTableNameLength() throws SQLException {
    return getIntOrDefault("getMaxTableNameLength", 0);
  }

  public int getMaxTablesInSelect() throws SQLException {
    return getIntOrDefault("getMaxTablesInSelect", 0);
  }

  public int getMaxUserNameLength() throws SQLException {
    return getIntOrDefault("getMaxUserNameLength", 0);
  }

  public int getDefaultTransactionIsolation() throws SQLException {
    return getInt("getDefaultTransactionIsolation");
  }

  public boolean supportsTransactions() throws SQLException {
    return getBooleanOrDefault("supportsTransactions", false);
  }

  public boolean supportsTransactionIsolationLevel(int level)
      throws SQLException {
    return getBooleanOrDefault("supportsTransactionIsolationLevel", false,
        level);
  }

  public boolean supportsDataDefinitionAndDataManipulationTransactions()
      throws SQLException {
    return getBooleanOrDefault(
        "supportsDataDefinitionAndDataManipulationTransactions", false);
  }

  public boolean supportsDataManipulationTransactionsOnly()
      throws SQLException {
    return getBooleanOrDefault("supportsDataManipulationTransactionsOnly",
        false);
  }

  public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
    return getBooleanOrDefault("dataDefinitionCausesTransactionCommit", false);
  }

  public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
    return getBooleanOrDefault("dataDefinitionIgnoredInTransactions", false);
  }

  public ResultSet getProcedures(String catalog, String schemaPattern,
      String procedureNamePattern) throws SQLException {
    return getResultSet("getProcedures", catalog, schemaPattern,
        procedureNamePattern);
  }

  public ResultSet getProcedureColumns(String catalog, String schemaPattern,
      String procedureNamePattern, String columnNamePattern)
      throws SQLException {
    return getResultSet("getProcedureColumns", catalog, schemaPattern,
        procedureNamePattern, columnNamePattern);
  }

  public ResultSet getTables(String catalog, String schemaPattern,
      String tableNamePattern, String[] types) throws SQLException {
    return getResultSet("getTables", catalog, schemaPattern, tableNamePattern,
        types);
  }

  public ResultSet getSchemas() throws SQLException {
    return getResultSet("getSchemas");
  }

  public ResultSet getCatalogs() throws SQLException {
    return getResultSet("getCatalogs");
  }

  public ResultSet getTableTypes() throws SQLException {
    return getResultSet("getTableTypes");
  }

  public ResultSet getColumns(String catalog, String schemaPattern,
      String tableNamePattern, String columnNamePattern) throws SQLException {
    return getResultSet("getColumns", catalog, schemaPattern, tableNamePattern,
        columnNamePattern);
  }

  public ResultSet getColumnPrivileges(String catalog, String schema,
      String table, String columnNamePattern) throws SQLException {
    return metaData.getColumnPrivileges(catalog, schema, table,
        columnNamePattern);
  }

  public ResultSet getTablePrivileges(String catalog, String schemaPattern,
      String tableNamePattern) throws SQLException {
    return getResultSet("getTablePrivileges", catalog, schemaPattern,
        tableNamePattern);
  }

  public ResultSet getBestRowIdentifier(String catalog, String schema,
      String table, int scope, boolean nullable) throws SQLException {
    return getResultSet("getBestRowIdentifier", catalog, schema, table, scope,
        nullable);
  }

  public ResultSet getVersionColumns(String catalog, String schema,
      String table) throws SQLException {
    return getResultSet("getVersionColumns", catalog, schema, table);
  }

  public ResultSet getPrimaryKeys(String catalog, String schema, String table)
      throws SQLException {
    return getResultSet("getPrimaryKeys", catalog, schema, table);
  }

  public ResultSet getImportedKeys(String catalog, String schema, String table)
      throws SQLException {
    return getResultSet("getImportedKeys", catalog, schema, table);
  }

  public ResultSet getExportedKeys(String catalog, String schema, String table)
      throws SQLException {
    return getResultSet("getExportedKeys", catalog, schema, table);
  }

  public ResultSet getCrossReference(String parentCatalog, String parentSchema,
      String parentTable, String foreignCatalog, String foreignSchema,
      String foreignTable) throws SQLException {
    return getResultSet("getCrossReference", parentCatalog, parentSchema,
        parentTable, foreignCatalog, foreignSchema, foreignTable);
  }

  public ResultSet getTypeInfo() throws SQLException {
    return getResultSet("getTypeInfo");
  }

  public ResultSet getIndexInfo(String catalog, String schema, String table,
      boolean unique, boolean approximate) throws SQLException {
    return getResultSet("getIndexInfo", catalog, schema, table, unique,
        approximate);
  }

  public boolean supportsResultSetType(int type) throws SQLException {
    return getBooleanOrDefault("supportsResultSetType", false, type);
  }

  public boolean supportsResultSetConcurrency(int type, int concurrency)
      throws SQLException {
    return getBooleanOrDefault("supportsResultSetConcurrency", false, type,
        concurrency);
  }

  public boolean ownUpdatesAreVisible(int type) throws SQLException {
    return getBooleanOrDefault("ownUpdatesAreVisible", false, type);
  }

  public boolean ownDeletesAreVisible(int type) throws SQLException {
    return getBooleanOrDefault("ownDeletesAreVisible", false, type);
  }

  public boolean ownInsertsAreVisible(int type) throws SQLException {
    return getBooleanOrDefault("ownInsertsAreVisible", false, type);
  }

  public boolean othersUpdatesAreVisible(int type) throws SQLException {
    return getBooleanOrDefault("othersUpdatesAreVisible", false, type);
  }

  public boolean othersDeletesAreVisible(int type) throws SQLException {
    return getBooleanOrDefault("othersDeletesAreVisible", false, type);
  }

  public boolean othersInsertsAreVisible(int type) throws SQLException {
    return getBooleanOrDefault("othersInsertsAreVisible", false, type);
  }

  public boolean updatesAreDetected(int type) throws SQLException {
    return getBooleanOrDefault("updatesAreDetected", false, type);
  }

  public boolean deletesAreDetected(int type) throws SQLException {
    return getBooleanOrDefault("deletesAreDetected", false, type);
  }

  public boolean insertsAreDetected(int type) throws SQLException {
    return getBooleanOrDefault("insertsAreDetected", false, type);
  }

  public boolean supportsBatchUpdates() throws SQLException {
    return getBooleanOrDefault("supportsBatchUpdates", false);
  }

  public ResultSet getUDTs(String catalog, String schemaPattern,
      String typeNamePattern, int[] types) throws SQLException {
    return getResultSet("getUDTs", catalog, schemaPattern, typeNamePattern,
        types);
  }

  public Connection getConnection() throws SQLException {
    return (Connection) get("getConnection");
  }

  public boolean supportsSavepoints() throws SQLException {
    return getBooleanOrDefault("supportsSavepoints", false);
  }

  public boolean supportsNamedParameters() throws SQLException {
    return getBooleanOrDefault("supportsNamedParameters", false);
  }

  public boolean supportsMultipleOpenResults() throws SQLException {
    return getBooleanOrDefault("supportsMultipleOpenResults", false);
  }

  public boolean supportsGetGeneratedKeys() throws SQLException {
    return getBooleanOrDefault("supportsGetGeneratedKeys", false);
  }

  public ResultSet getSuperTypes(String catalog, String schemaPattern,
      String typeNamePattern) throws SQLException {
    return getResultSet("getSuperTypes", catalog, schemaPattern,
        typeNamePattern);
  }

  public ResultSet getSuperTables(String catalog, String schemaPattern,
      String tableNamePattern) throws SQLException {
    return getResultSet("getSuperTables", catalog, schemaPattern,
        tableNamePattern);
  }

  public ResultSet getAttributes(String catalog, String schemaPattern,
      String typeNamePattern, String attributeNamePattern)
      throws SQLException {
    return getResultSet("getAttributes", catalog, schemaPattern,
        typeNamePattern, attributeNamePattern);
  }

  public boolean supportsResultSetHoldability(int holdability)
      throws SQLException {
    return getBooleanOrDefault("supportsResultSetHoldability", false,
        holdability);
  }

  public int getResultSetHoldability() throws SQLException {
    return getInt("getResultSetHoldability");
  }

  public int getDatabaseMajorVersion() throws SQLException {
    return getInt("getDatabaseMajorVersion");
  }

  public int getDatabaseMinorVersion() throws SQLException {
    return getInt("getDatabaseMinorVersion");
  }

  public int getJDBCMajorVersion() throws SQLException {
    return getInt("getJDBCMajorVersion");
  }

  public int getJDBCMinorVersion() throws SQLException {
    return getInt("getJDBCMinorVersion");
  }

  public int getSQLStateType() throws SQLException {
    return getInt("getSQLStateType");
  }

  public boolean locatorsUpdateCopy() throws SQLException {
    return getBoolean("locatorsUpdateCopy");
  }

  public boolean supportsStatementPooling() throws SQLException {
    return getBooleanOrDefault("supportsStatementPooling", false);
  }

  public RowIdLifetime getRowIdLifetime() throws SQLException {
    return (RowIdLifetime) get("getRowIdLifetime");
  }

  public ResultSet getSchemas(String catalog, String schemaPattern)
      throws SQLException {
    return getResultSet("getSchemas", catalog, schemaPattern);
  }

  public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
    return getBooleanOrDefault("supportsStoredFunctionsUsingCallSyntax", false);
  }

  public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
    return getBooleanOrDefault("autoCommitFailureClosesAllResultSets", false);
  }

  public ResultSet getClientInfoProperties() throws SQLException {
    return getResultSet("getClientInfoProperties");
  }

  public ResultSet getFunctions(String catalog, String schemaPattern,
      String functionNamePattern) throws SQLException {
    return getResultSet("getFunctions", catalog, schemaPattern,
        functionNamePattern);
  }

  public ResultSet getFunctionColumns(String catalog, String schemaPattern,
      String functionNamePattern, String columnNamePattern)
      throws SQLException {
    return getResultSet("getFunctionColumns", catalog, schemaPattern,
        functionNamePattern, columnNamePattern);
  }

  public ResultSet getPseudoColumns(String catalog, String schemaPattern,
      String tableNamePattern, String columnNamePattern) throws SQLException {
    return getResultSet("getPseudoColumns", catalog, schemaPattern,
        tableNamePattern, columnNamePattern);
  }

  public boolean generatedKeyAlwaysReturned() throws SQLException {
    return getBooleanOrDefault("generatedKeyAlwaysReturned", false);
  }

  public long getMaxLogicalLobSize() throws SQLException {
    return getIntOrDefault("getMaxLogicalLobSize", 0);
  }

  public boolean supportsRefCursors() throws SQLException {
    return getBooleanOrDefault("supportsRefCursors", false);
  }

  private ResultSet getResultSet(final String methodName, final Object... args)
      throws SQLException {
    return (ResultSet) get(methodName, args);
  }

  private boolean getBoolean(final String methodName) throws SQLException {
    return (boolean) get(methodName);
  }

  private boolean getBooleanOrDefault(String methodName, boolean defaultValue,
      final Object... args) throws SQLException {
    return (boolean) getOrDefault(methodName, defaultValue, args);
  }

  private int getInt(final String methodName) throws SQLException {
    return (int) get(methodName);
  }

  private int getIntOrDefault(String methodName, int defaultValue)
      throws SQLException {
    return (int) getOrDefault(methodName, defaultValue);
  }

  private String getString(final String methodName) throws SQLException {
    return (String) get(methodName);
  }

  private String getStringOrDefault(String methodName, String defaultValue)
      throws SQLException {
    return (String) getOrDefault(methodName, defaultValue);
  }

  private Object getOrDefault(String methodName, Object defaultValue,
      Object... args) throws SQLException {
    if (sqlLine.getOpts().getStrictJdbc()) {
      return get(methodName, args);
    }
    try {
      return sqlLine.getReflector().invoke(metaData, methodName, args);
    } catch (Exception e) {
      Throwable t = e.getCause() == null ? e : e.getCause();
      if (t instanceof SQLFeatureNotSupportedException
          || METHOD_NOT_SUPPORTED.equalsIgnoreCase(t.getMessage())) {
        return defaultValue;
      } else {
        throw t instanceof SQLException
            ? (SQLException) t
            : new SQLException(t);
      }
    }
  }

  private Object get(final String methodName, Object... args)
      throws SQLException {
    try {
      return sqlLine.getReflector().invoke(metaData, methodName, args);
    } catch (Exception e) {
      Throwable t = e.getCause() == null ? e : e.getCause();
      throw t instanceof SQLException
          ? (SQLException) t
          : new SQLException(t);
    }
  }
}

// End DatabaseMetaDataWrapper.java
