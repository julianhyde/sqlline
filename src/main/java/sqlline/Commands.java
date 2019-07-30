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

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jline.reader.History;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import static sqlline.SqlLine.rpad;

/**
 * Collection of available commands.
 */
public class Commands {
  private static final String[] METHODS = {
      "allProceduresAreCallable",
      "allTablesAreSelectable",
      "dataDefinitionCausesTransactionCommit",
      "dataDefinitionIgnoredInTransactions",
      "doesMaxRowSizeIncludeBlobs",
      "getCatalogSeparator",
      "getCatalogTerm",
      "getDatabaseProductName",
      "getDatabaseProductVersion",
      "getDefaultTransactionIsolation",
      "getDriverMajorVersion",
      "getDriverMinorVersion",
      "getDriverName",
      "getDriverVersion",
      "getExtraNameCharacters",
      "getIdentifierQuoteString",
      "getMaxBinaryLiteralLength",
      "getMaxCatalogNameLength",
      "getMaxCharLiteralLength",
      "getMaxColumnNameLength",
      "getMaxColumnsInGroupBy",
      "getMaxColumnsInIndex",
      "getMaxColumnsInOrderBy",
      "getMaxColumnsInSelect",
      "getMaxColumnsInTable",
      "getMaxConnections",
      "getMaxCursorNameLength",
      "getMaxIndexLength",
      "getMaxProcedureNameLength",
      "getMaxRowSize",
      "getMaxSchemaNameLength",
      "getMaxStatementLength",
      "getMaxStatements",
      "getMaxTableNameLength",
      "getMaxTablesInSelect",
      "getMaxUserNameLength",
      "getNumericFunctions",
      "getProcedureTerm",
      "getSchemaTerm",
      "getSearchStringEscape",
      "getSQLKeywords",
      "getStringFunctions",
      "getSystemFunctions",
      "getTimeDateFunctions",
      "getURL",
      "getUserName",
      "isCatalogAtStart",
      "isReadOnly",
      "nullPlusNonNullIsNull",
      "nullsAreSortedAtEnd",
      "nullsAreSortedAtStart",
      "nullsAreSortedHigh",
      "nullsAreSortedLow",
      "storesLowerCaseIdentifiers",
      "storesLowerCaseQuotedIdentifiers",
      "storesMixedCaseIdentifiers",
      "storesMixedCaseQuotedIdentifiers",
      "storesUpperCaseIdentifiers",
      "storesUpperCaseQuotedIdentifiers",
      "supportsAlterTableWithAddColumn",
      "supportsAlterTableWithDropColumn",
      "supportsANSI92EntryLevelSQL",
      "supportsANSI92FullSQL",
      "supportsANSI92IntermediateSQL",
      "supportsBatchUpdates",
      "supportsCatalogsInDataManipulation",
      "supportsCatalogsInIndexDefinitions",
      "supportsCatalogsInPrivilegeDefinitions",
      "supportsCatalogsInProcedureCalls",
      "supportsCatalogsInTableDefinitions",
      "supportsColumnAliasing",
      "supportsConvert",
      "supportsCoreSQLGrammar",
      "supportsCorrelatedSubqueries",
      "supportsDataDefinitionAndDataManipulationTransactions",
      "supportsDataManipulationTransactionsOnly",
      "supportsDifferentTableCorrelationNames",
      "supportsExpressionsInOrderBy",
      "supportsExtendedSQLGrammar",
      "supportsFullOuterJoins",
      "supportsGroupBy",
      "supportsGroupByBeyondSelect",
      "supportsGroupByUnrelated",
      "supportsIntegrityEnhancementFacility",
      "supportsLikeEscapeClause",
      "supportsLimitedOuterJoins",
      "supportsMinimumSQLGrammar",
      "supportsMixedCaseIdentifiers",
      "supportsMixedCaseQuotedIdentifiers",
      "supportsMultipleResultSets",
      "supportsMultipleTransactions",
      "supportsNonNullableColumns",
      "supportsOpenCursorsAcrossCommit",
      "supportsOpenCursorsAcrossRollback",
      "supportsOpenStatementsAcrossCommit",
      "supportsOpenStatementsAcrossRollback",
      "supportsOrderByUnrelated",
      "supportsOuterJoins",
      "supportsPositionedDelete",
      "supportsPositionedUpdate",
      "supportsSchemasInDataManipulation",
      "supportsSchemasInIndexDefinitions",
      "supportsSchemasInPrivilegeDefinitions",
      "supportsSchemasInProcedureCalls",
      "supportsSchemasInTableDefinitions",
      "supportsSelectForUpdate",
      "supportsStoredProcedures",
      "supportsSubqueriesInComparisons",
      "supportsSubqueriesInExists",
      "supportsSubqueriesInIns",
      "supportsSubqueriesInQuantifieds",
      "supportsTableCorrelationNames",
      "supportsTransactions",
      "supportsUnion",
      "supportsUnionAll",
      "usesLocalFilePerTable",
      "usesLocalFiles",
  };

  private static final String CONNECT_PROPERTY = "#CONNECT_PROPERTY#.";
  private final SqlLine sqlLine;

  Commands(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public void metadata(String line, DispatchCallback callback) {
    sqlLine.debug(line);

    String[] parts = sqlLine.split(line);
    if (parts == null || parts.length == 0) {
      dbinfo("", callback);
      return;
    }

    if (parts.length == 1) {
      sqlLine.error("Usage: metadata <methodname> <params...>");
      callback.setToFailure();
      return;
    }

    List<Object> params = new LinkedList<>(Arrays.asList(parts));
    params.remove(0);
    params.remove(0);
    sqlLine.debug(params.toString());
    metadata(parts[1], params, callback);
  }

  public void metadata(
      String cmd, List<Object> argList, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    try {
      Set<String> methodNames = new TreeSet<>();
      Set<String> methodNamesUpper = new TreeSet<>();
      Class currentClass = sqlLine.getConnection().getMetaData().getClass();
      Object res = null;
      do {
        for (Method method : currentClass.getDeclaredMethods()) {
          final int modifiers = method.getModifiers();
          if (!Modifier.isPublic(modifiers)
              || Modifier.isStatic(modifiers)) {
            continue;
          }
          methodNames.add(method.getName());
          methodNamesUpper.add(method.getName().toUpperCase(Locale.ROOT));
          if (methodNamesUpper.contains(cmd.toUpperCase(Locale.ROOT))) {
            try {
              res = sqlLine.getReflector().invoke(
                  sqlLine.getDatabaseMetaData(),
                  sqlLine.getDatabaseMetaData().getClass(), cmd, argList);
            } catch (Exception e) {
              sqlLine.handleException(e);
              callback.setToFailure();
              return;
            }
            if (res != null) {
              break;
            }
          }
        }
        currentClass = currentClass.getSuperclass();
      } while (res == null
          && DatabaseMetaData.class.isAssignableFrom(currentClass));

      if (!methodNamesUpper.contains(cmd.toUpperCase(Locale.ROOT))) {
        sqlLine.error(sqlLine.loc("no-such-method", cmd));
        sqlLine.error(sqlLine.loc("possible-methods"));
        for (String methodName : methodNames) {
          sqlLine.error("   " + methodName);
        }
        callback.setToFailure();
        return;
      }

      if (res instanceof ResultSet) {
        try (ResultSet rs = (ResultSet) res) {
          sqlLine.print(rs, callback);
        }
      } else if (res != null) {
        sqlLine.output(res.toString());
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
    callback.setToSuccess();
  }

  public void history(String line, DispatchCallback callback) {
    try {
      String argsLine = line.substring("history".length());
      org.jline.builtins.Commands.history(
          sqlLine.getLineReader(),
          sqlLine.getOutputStream(),
          sqlLine.getErrorStream(),
          argsLine.isEmpty()
              ? new String[]{sqlLine.getOpts().getHistoryFlags()}
              : sqlLine.split(argsLine, " "));
    } catch (Exception e) {
      callback.setToFailure();
    }
    callback.setToSuccess();
  }

  public void rerun(String line, DispatchCallback callback) {
    String[] cmd = sqlLine.split(line);
    History history = sqlLine.getLineReader().getHistory();
    int size = history.size();
    if (cmd.length > 2 || (cmd.length == 2 && !cmd[1].matches("-?\\d+"))) {
      if (size == 0) {
        sqlLine.error("Usage: rerun <offset>, history should not be empty");
      } else {
        sqlLine.error("Usage: rerun <offset>, available range of offset is -"
            + (size - 1) + ".." + size);
      }
      callback.setToFailure();
      return;
    }

    int offset = cmd.length == 1 ? -1 : Integer.parseInt(cmd[1]);
    if (size < offset || size - 1 < -offset || offset == 0) {
      if (offset == 0) {
        sqlLine.error(
            "Usage: rerun <offset>, offset should be positive or negative");
      }
      if (size == 0) {
        sqlLine.error("Usage: rerun <offset>, history should not be empty");
      } else {
        sqlLine.error("Usage: rerun <offset>, available range of offset is -"
            + (size - 1) + ".." + size);
      }
      callback.setToFailure();
      return;
    }

    sqlLine.dispatch(calculateCommand(offset, new HashSet<>()), callback);
  }

  private String calculateCommand(int currentOffset, Set<Integer> offsets) {
    if (!offsets.add(currentOffset)) {
      throw new IllegalArgumentException(
          "Cycled rerun of commands from history " + offsets);
    }

    History history = sqlLine.getLineReader().getHistory();
    Iterator<History.Entry> iterator = currentOffset > 0
        ? history.iterator(currentOffset - 1)
        : history.reverseIterator(history.size() - 1 + currentOffset);
    String command = iterator.next().line();
    if (command.trim().startsWith("!/") || command.startsWith("!rerun")) {
      String[] cmd = sqlLine.split(command);
      if (cmd.length > 2 || (cmd.length == 2 && !cmd[1].matches("-?\\d+"))) {
        return command;
      }
      int offset = cmd.length == 1 ? -1 : Integer.parseInt(cmd[1]);
      if (history.size() < offset || history.size() - 1 < -offset) {
        return command;
      }
      return calculateCommand(offset, offsets);
    }
    return command;
  }

  String arg1(String line, String paramName) {
    return arg1(line, paramName, null);
  }

  String arg1(String line, String paramName, String def) {
    String[] ret = sqlLine.split(line);

    if (ret == null || ret.length != 2) {
      if (def != null) {
        return def;
      }
      throw new IllegalArgumentException(
          sqlLine.loc("arg-usage",
              ret == null || ret.length == 0 ? "" : ret[0], paramName));
    }
    return ret[1];
  }

  /**
   * Constructs a list of string parameters for a metadata call.
   *
   * <p>The number of items is equal to the number of arguments once
   * the command line (the {@code line} parameter) has been parsed,
   * typically three (catalog, schema, table name).
   *
   * <p>Parses the command line, and assumes that the the first word is
   * a compound identifier. If the compound identifier has fewer parts
   * than required, fills from the right.
   *
   * <p>The result is a mutable list of strings.
   *
   * @param line          Command line
   * @param paramName     Name of parameter being read from command line
   * @param defaultValues Default values for each component of parameter
   * @return Mutable list of strings
   */
  private List<Object> buildMetadataArgs(
      String line,
      String paramName,
      String[] defaultValues) {
    final List<Object> list = new ArrayList<>();
    final String[][] ret = sqlLine.splitCompound(line);
    String[] compound;
    if (ret == null || ret.length != 2) {
      if (defaultValues[defaultValues.length - 1] == null) {
        throw new IllegalArgumentException(
            sqlLine.loc("arg-usage",
                ret == null || ret.length == 0 ? "" : ret[0][0], paramName));
      }
      compound = new String[0];
    } else {
      compound = ret[1];
    }
    if (compound.length <= defaultValues.length) {
      list.addAll(
          Arrays.asList(defaultValues).subList(
              0, defaultValues.length - compound.length));
      list.addAll(Arrays.asList(compound));
    } else {
      list.addAll(
          Arrays.asList(compound).subList(0, defaultValues.length));
    }
    return list;
  }

  public void indexes(String line, DispatchCallback callback) throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    args.add(Boolean.FALSE);
    args.add(Boolean.TRUE);
    metadata("getIndexInfo", args, callback);
  }

  public void primarykeys(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    metadata("getPrimaryKeys", args, callback);
  }

  public void exportedkeys(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    metadata("getExportedKeys", args, callback);
  }

  public void importedkeys(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    metadata("getImportedKeys", args, callback);
  }

  public void procedures(String line, DispatchCallback callback)
      throws Exception {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args =
        buildMetadataArgs(line, "procedure name pattern", strings);
    metadata("getProcedures", args, callback);
  }

  public void tables(String line, DispatchCallback callback)
      throws SQLException {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    args.add(null);
    metadata("getTables", args, callback);
  }

  public void schemas(String line, DispatchCallback callback) {
    metadata("getSchemas", Collections.emptyList(), callback);
  }

  public void typeinfo(String line, DispatchCallback callback) {
    metadata("getTypeInfo", Collections.emptyList(), callback);
  }

  public void nativesql(String sql, DispatchCallback callback)
      throws Exception {
    if (sql.startsWith(SqlLine.COMMAND_PREFIX)) {
      sql = sql.substring(1);
    }

    if (sql.startsWith("native")) {
      sql = sql.substring("native".length() + 1);
    }

    String nat = sqlLine.getConnection().nativeSQL(sql);
    sqlLine.output(nat);
    callback.setToSuccess();
  }

  public void columns(String line, DispatchCallback callback)
      throws SQLException {
    String[] strings = {sqlLine.getConnection().getCatalog(), null, "%"};
    List<Object> args = buildMetadataArgs(line, "table name", strings);
    args.add("%");
    metadata("getColumns", args, callback);
  }

  public void dropall(String line, DispatchCallback callback) {
    String[] parts = sqlLine.split(line);
    if (parts.length > 2) {
      sqlLine.error("Usage: !dropall [schema_pattern]");
      callback.setToFailure();
      return;
    }
    DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
    if (databaseConnection == null || databaseConnection.getUrl() == null) {
      sqlLine.error(sqlLine.loc("no-current-connection"));
      callback.setToFailure();
      return;
    }
    try {
      String question = sqlLine.loc("really-drop-all");
      final int userResponse =
          getUserAnswer(question, 'y', 'n', 'Y', 'N');
      if (userResponse != 'y' && userResponse != 'Y') {
        sqlLine.error("abort-drop-all");
        callback.setToFailure();
        return;
      }

      List<String> cmds = new LinkedList<>();
      final char openQuote = sqlLine.getDialect().getOpenQuote();
      final char closeQuote = sqlLine.getDialect().getOpenQuote();
      try (ResultSet rs =
               sqlLine.getTables(parts.length > 1 ? parts[1] : null)) {
        while (rs.next()) {
          cmds.add("DROP TABLE " + openQuote + rs.getString("TABLE_SCHEM")
              + closeQuote + "." + openQuote
              + rs.getString("TABLE_NAME")  + closeQuote + ";");
        }
      }

      // run as a batch
      if (sqlLine.runCommands(cmds, callback) == cmds.size()) {
        callback.setToSuccess();
      } else {
        callback.setToFailure();
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  int getUserAnswer(String question, int... allowedAnswers)
      throws IOException {
    final Set<Integer> allowedAnswerSet =
        IntStream.of(allowedAnswers).boxed().collect(Collectors.toSet());
    final Terminal terminal = sqlLine.getLineReader().getTerminal();
    final PrintWriter writer = terminal.writer();
    writer.write(question);
    writer.write('\n');
    writer.flush();
    int c;
    // The logic to prevent reaction of SqlLineParser here
    do {
      c = terminal.reader().read(100);
    } while (c != -1 && !allowedAnswerSet.contains(c));
    return c;
  }

  public void reconnect(String line, DispatchCallback callback) {
    DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
    if (databaseConnection == null || databaseConnection.getUrl() == null) {
      sqlLine.error(sqlLine.loc("no-current-connection"));
      callback.setToFailure();
      return;
    }

    sqlLine.info(sqlLine.loc("reconnecting", databaseConnection.getUrl()));
    try {
      databaseConnection.reconnect();
    } catch (Exception e) {
      sqlLine.error(e);
      callback.setToFailure();
      return;
    }

    callback.setToSuccess();
  }

  public void scan(String line, DispatchCallback callback) {
    final Map<String, Driver> driverNames = new TreeMap<>();

    if (sqlLine.getDrivers() == null) {
      sqlLine.setDrivers(sqlLine.scanDrivers());
    }

    sqlLine.info(
        sqlLine.loc("drivers-found-count", sqlLine.getDrivers().size()));

    // unique the list

    for (Driver driver : sqlLine.getDrivers()) {
      driverNames.put(driver.getClass().getName(), driver);
    }

    final String header = rpad(sqlLine.loc("compliant"), 10)
        + rpad(sqlLine.loc("jdbc-version"), 8)
        + sqlLine.loc("driver-class");
    sqlLine.output(new AttributedString(header, AttributedStyle.BOLD));

    for (Map.Entry<String, Driver> driverEntry : driverNames.entrySet()) {
      final String name = driverEntry.getKey();
      try {
        final Class<?> klass = Class.forName(name);
        Driver driver = (Driver) klass.getConstructor().newInstance();
        final String msg = rpad(driver.jdbcCompliant() ? "yes" : "no", 10)
            + rpad(driver.getMajorVersion() + "." + driver.getMinorVersion(), 8)
            + name;
        if (driver.jdbcCompliant()) {
          sqlLine.output(msg);
        } else {
          sqlLine.output(new AttributedString(msg, AttributedStyles.RED));
        }
      } catch (Throwable t) {
        // error with driver
        sqlLine.output(new AttributedString(name, AttributedStyles.RED));
      }
    }

    callback.setToSuccess();
  }

  public void save(String line, DispatchCallback callback)
      throws IOException {
    sqlLine.info(
        sqlLine.loc("saving-options", sqlLine.getOpts().getPropertiesFile()));
    sqlLine.getOpts().save();
    callback.setToSuccess();
  }

  public void load(String line, DispatchCallback callback) throws IOException {
    sqlLine.getOpts().load();
    sqlLine.info(
        sqlLine.loc("loaded-options",
            sqlLine.getOpts().getPropertiesFile()));
    callback.setToSuccess();
  }

  public void config(String line, DispatchCallback callback) {
    try {
      Properties props = sqlLine.getOpts().toProperties();
      Set<String> keys = new TreeSet<>(asMap(props).keySet());
      for (String key : keys) {
        sqlLine.outputProperty(
            key.substring(SqlLineOpts.PROPERTY_PREFIX.length()),
            props.getProperty(key));
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
      return;
    }

    callback.setToSuccess();
  }

  public void set(String line, DispatchCallback callback) {
    if (line == null || line.trim().equals("set")
        || line.length() == 0) {
      config(null, callback);
      return;
    }

    final String[] parts = sqlLine.split(line);
    if (parts.length > 3) {
      sqlLine.error("Usage: set [all | <property name> [<value>]]");
      callback.setToFailure();
      return;
    }

    String propertyName = parts[1].toLowerCase(Locale.ROOT);

    if ("all".equals(propertyName)) {
      config(null, callback);
      return;
    }

    if (!sqlLine.getOpts().hasProperty(propertyName)) {
      sqlLine.error(sqlLine.loc("no-specified-prop", propertyName));
      callback.setToFailure();
      return;
    }

    if (parts.length == 2) {
      try {
        sqlLine.outputProperty(propertyName,
            sqlLine.getOpts().get(propertyName));
        callback.setToSuccess();
      } catch (Exception e) {
        sqlLine.error(e);
        callback.setToFailure();
      }
    } else {
      setProperty(propertyName, parts[2], null, callback);
    }
  }

  public void reset(String line, DispatchCallback callback) {
    String[] split = sqlLine.split(line, 2,
        "Usage: reset (all | <property name>)");
    if (split == null) {
      callback.setToFailure();
      return;
    }

    String propertyName = split[1].toLowerCase(Locale.ROOT);

    if ("all".equals(propertyName)) {
      sqlLine.setOpts(new SqlLineOpts(sqlLine));
      sqlLine.output(sqlLine.loc("reset-all-props"));
      // no need to auto save, since its off by default
      callback.setToSuccess();
      return;
    }

    if (!sqlLine.getOpts().hasProperty(propertyName)) {
      sqlLine.error(sqlLine.loc("no-specified-prop", propertyName));
      callback.setToFailure();
      return;
    }

    try {
      String defaultValue = new SqlLineOpts(sqlLine).get(propertyName);
      setProperty(propertyName, defaultValue, "reset-prop", callback);
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  private void setProperty(String key, String value, String res,
      DispatchCallback callback) {
    boolean success = sqlLine.getOpts().set(key, value, false);
    if (success) {
      if (sqlLine.getOpts().getAutoSave()) {
        try {
          sqlLine.getOpts().save();
        } catch (Exception saveException) {
          // ignore
        }
      }
      if (res != null) {
        sqlLine.output(sqlLine.loc(res, key, value));
      }
      callback.setToSuccess();
    } else {
      callback.setToFailure();
    }
  }

  private void reportResult(String action, long start, long end) {
    if (sqlLine.getOpts().getShowElapsedTime()) {
      sqlLine.info(action + " " + sqlLine.locElapsedTime(end - start));
    } else {
      sqlLine.info(action);
    }
  }

  public void commit(String line, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }
    if (!(sqlLine.assertAutoCommit())) {
      callback.setToFailure();
      return;
    }

    try {
      long start = System.currentTimeMillis();
      sqlLine.getDatabaseConnection().connection.commit();
      long end = System.currentTimeMillis();
      sqlLine.showWarnings();
      reportResult(sqlLine.loc("commit-complete"), start, end);

      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void rollback(String line, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }
    if (!(sqlLine.assertAutoCommit())) {
      callback.setToFailure();
      return;
    }

    try {
      long start = System.currentTimeMillis();
      sqlLine.getDatabaseConnection().connection.rollback();
      long end = System.currentTimeMillis();
      sqlLine.showWarnings();
      reportResult(sqlLine.loc("rollback-complete"), start, end);
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void autocommit(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    if (line.endsWith("on")) {
      sqlLine.getDatabaseConnection().connection.setAutoCommit(true);
    } else if (line.endsWith("off")) {
      sqlLine.getDatabaseConnection().connection.setAutoCommit(false);
    }

    sqlLine.showWarnings();
    sqlLine.autocommitStatus(sqlLine.getDatabaseConnection().connection);
    callback.setToSuccess();
  }

  public void readonly(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    if (line.endsWith("on")) {
      sqlLine.getDatabaseConnection().connection.setReadOnly(true);
    } else if (line.endsWith("off")) {
      sqlLine.getDatabaseConnection().connection.setReadOnly(false);
    }

    sqlLine.showWarnings();
    sqlLine.readonlyStatus(sqlLine.getDatabaseConnection().connection);
    callback.setToSuccess();
  }

  public void dbinfo(String line, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    sqlLine.showWarnings();
    int padlen = 50;

    for (String method : METHODS) {
      try {
        final String s =
            String.valueOf(sqlLine.getReflector()
                .invoke(sqlLine.getDatabaseMetaData(), method));
        sqlLine.output(
            new AttributedStringBuilder()
                .append(rpad(method, padlen))
                .append(s)
                .toAttributedString());
      } catch (Exception e) {
        sqlLine.handleException(e);
      }
    }

    callback.setToSuccess();
  }

  public void verbose(String line, DispatchCallback callback) {
    sqlLine.info("verbose: on");
    set("set verbose true", callback);
  }

  public void outputformat(String line, DispatchCallback callback) {
    try {
      String[] lines = sqlLine.split(line);
      sqlLine.getOpts().setOutputFormat(lines[1]);
      if ("csv".equals(lines[1])) {
        if (lines.length > 2) {
          sqlLine.getOpts().set(BuiltInProperty.CSV_DELIMITER, lines[2]);
        }
        if (lines.length > 3) {
          sqlLine.getOpts().setCsvQuoteCharacter(lines[3]);
        }
      } else if ("table".equals(lines[1])) {
        if (lines.length > 2) {
          sqlLine.getOpts().set(BuiltInProperty.MAX_COLUMN_WIDTH, lines[2]);
        }
      }
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
    }
  }

  public void brief(String line, DispatchCallback callback) {
    sqlLine.info("verbose: off");
    set("set verbose false", callback);
  }

  public void isolation(String line, DispatchCallback callback)
      throws SQLException {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    final int i;
    line = line.toUpperCase(Locale.ROOT);
    if (line.endsWith(SqlLineProperty.DEFAULT.toUpperCase(Locale.ROOT))) {
      i = sqlLine.getDatabaseMetaData().getDefaultTransactionIsolation();
    } else if (line.endsWith("TRANSACTION_NONE")) {
      i = Connection.TRANSACTION_NONE;
    } else if (line.endsWith("TRANSACTION_READ_COMMITTED")) {
      i = Connection.TRANSACTION_READ_COMMITTED;
    } else if (line.endsWith("TRANSACTION_READ_UNCOMMITTED")) {
      i = Connection.TRANSACTION_READ_UNCOMMITTED;
    } else if (line.endsWith("TRANSACTION_REPEATABLE_READ")) {
      i = Connection.TRANSACTION_REPEATABLE_READ;
    } else if (line.endsWith("TRANSACTION_SERIALIZABLE")) {
      i = Connection.TRANSACTION_SERIALIZABLE;
    } else {
      callback.setToFailure();
      sqlLine.error("Usage: isolation <TRANSACTION_NONE "
          + "| TRANSACTION_READ_COMMITTED "
          + "| TRANSACTION_READ_UNCOMMITTED "
          + "| TRANSACTION_REPEATABLE_READ "
          + "| TRANSACTION_SERIALIZABLE "
          + "| DEFAULT>");
      return;
    }

    if (!sqlLine.getDatabaseMetaData().supportsTransactionIsolationLevel(i)) {
      callback.setToFailure();
      final int defaultTransactionIsolation =
          sqlLine.getDatabaseMetaData().getDefaultTransactionIsolation();
      sqlLine.error(
          sqlLine.loc("isolation-level-not-supported",
              getTransactionIsolationName(i),
              getTransactionIsolationName(defaultTransactionIsolation)));
      return;
    }

    Connection connection = sqlLine.getDatabaseConnection().getConnection();
    connection.setTransactionIsolation(i);

    sqlLine.debug(
        sqlLine.loc("isolation-status", getTransactionIsolationName(i)));
    callback.setToSuccess();
  }

  private String getTransactionIsolationName(int i) {
    switch (i) {
    case Connection.TRANSACTION_NONE:
      return "TRANSACTION_NONE";
    case Connection.TRANSACTION_READ_COMMITTED:
      return "TRANSACTION_READ_COMMITTED";
    case Connection.TRANSACTION_READ_UNCOMMITTED:
      return "TRANSACTION_READ_UNCOMMITTED";
    case Connection.TRANSACTION_REPEATABLE_READ:
      return "TRANSACTION_REPEATABLE_READ";
    case Connection.TRANSACTION_SERIALIZABLE:
      return "TRANSACTION_SERIALIZABLE";
    default:
      return "UNKNOWN";
    }
  }

  public void batch(String line, DispatchCallback callback) {
    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    if (sqlLine.getBatch() == null) {
      sqlLine.setBatch(new LinkedList<>());
      sqlLine.info(sqlLine.loc("batch-start"));
      callback.setToSuccess();
    } else {
      sqlLine.info(sqlLine.loc("running-batch"));
      try {
        sqlLine.runBatch(sqlLine.getBatch());
        callback.setToSuccess();
      } catch (Exception e) {
        callback.setToFailure();
        sqlLine.error(e);
      } finally {
        sqlLine.setBatch(null);
      }
    }
  }

  public void sql(String line, DispatchCallback callback) {
    execute(line, false, callback);
  }

  public void call(String line, DispatchCallback callback) {
    execute(line, true, callback);
  }

  private void execute(String line, boolean call, DispatchCallback callback) {
    if (line == null || line.length() == 0) {
      callback.setStatus(DispatchCallback.Status.FAILURE);
      return;
    }

    if (!sqlLine.assertConnection()) {
      callback.setToFailure();
      return;
    }

    String fullLine = line;
    if (fullLine.startsWith(SqlLine.COMMAND_PREFIX)) {
      fullLine = fullLine.substring(1);
    }

    final String prefix = call ? "call" : "sql";
    if (fullLine.startsWith(prefix)) {
      fullLine = fullLine.substring(prefix.length());
    }

    final StringBuilder sql2execute = new StringBuilder();
    for (String sqlItem : fullLine.split(";")) {
      sql2execute.append(sqlItem).append(";");
      if (sqlLine.isOneLineComment(sql2execute.toString())
          || isSqlContinuationRequired(sql2execute.toString())) {
        continue;
      }
      final String sql = skipLast(flush(sql2execute));
      executeSingleQuery(sql, call, callback);
    }
    if (!callback.isFailure()) {
      callback.setToSuccess();
    }
  }

  /** Returns the contents of a {@link StringBuilder} and clears the builder. */
  static String flush(StringBuilder buf) {
    final String s = buf.toString();
    buf.setLength(0);
    return s;
  }

  /** Returns a string with the last character removed. */
  private static String skipLast(String s) {
    return s.substring(0, s.length() - 1);
  }

  private void executeSingleQuery(String sql, boolean call,
      DispatchCallback callback) {
    // batch statements?
    if (sqlLine.getBatch() != null) {
      sqlLine.getBatch().add(sql);
      return;
    }

    try {
      Statement stmnt = null;
      boolean hasResults;

      try {
        final long start = System.currentTimeMillis();
        if (sqlLine.getOpts().getCompiledConfirmPattern().matcher(sql).find()
            && sqlLine.getOpts().getConfirm()) {
          final String question = sqlLine.loc("really-perform-action");
          final int userResponse =
              getUserAnswer(question, 'y', 'n', 'Y', 'N');
          if (userResponse != 'y' && userResponse != 'Y') {
            sqlLine.error(sqlLine.loc("abort-action"));
            callback.setToFailure();
            return;
          }
        }
        if (call) {
          stmnt = sqlLine.getDatabaseConnection().connection.prepareCall(sql);
          callback.trackSqlQuery(stmnt);
          hasResults = ((CallableStatement) stmnt).execute();
        } else {
          stmnt = sqlLine.createStatement();
          callback.trackSqlQuery(stmnt);
          hasResults = stmnt.execute(sql);
        }

        sqlLine.showWarnings();
        sqlLine.showWarnings(stmnt.getWarnings());

        if (hasResults) {
          do {
            try (ResultSet rs = stmnt.getResultSet()) {
              int count = sqlLine.print(rs, callback);
              long end = System.currentTimeMillis();

              reportResult(sqlLine.loc("rows-selected", count), start, end);
            }
          } while (SqlLine.getMoreResults(stmnt));
        } else {
          int count = stmnt.getUpdateCount();
          long end = System.currentTimeMillis();
          reportResult(sqlLine.loc("rows-affected", count), start, end);
        }
      } finally {
        if (stmnt != null) {
          sqlLine.showWarnings(stmnt.getWarnings());
          stmnt.close();
        }
      }
    } catch (UserInterruptException uie) {
      // CTRL-C'd out of the command. Note it, but don't call it an
      // error.
      callback.setStatus(DispatchCallback.Status.CANCELED);
      sqlLine.output(sqlLine.loc("command-canceled"));
      return;
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }

    sqlLine.showWarnings();
  }

  public void quit(String line, DispatchCallback callback) {
    sqlLine.setExit(true);
    close(null, callback);
  }

  /**
   * Closes all connections.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void closeall(String line, DispatchCallback callback) {
    close(null, callback);
    if (callback.isSuccess()) {
      while (callback.isSuccess()) {
        close(null, callback);
      }
      // the last "close" will set it to fail so reset it to success.
      callback.setToSuccess();
    }
    // probably a holdover of the old boolean returns.
    callback.setToFailure();
  }

  /**
   * Closes the current connection.
   * Closes the current file writer.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void close(String line, DispatchCallback callback) {
    // close file writer
    if (sqlLine.getRecordOutputFile() != null) {
      // instead of line could be any string
      stopRecording(line, callback);
    }

    DatabaseConnection databaseConnection = sqlLine.getDatabaseConnection();
    if (databaseConnection == null) {
      callback.setToFailure();
      return;
    }

    try {
      Connection connection = databaseConnection.getConnection();
      if (connection != null && !connection.isClosed()) {
        sqlLine.debug(
            sqlLine.loc("closing", connection.getClass().getName()));
        connection.close();
      } else {
        sqlLine.debug(sqlLine.loc("already-closed"));
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
      return;
    }

    sqlLine.getDatabaseConnections().remove();
    callback.setToSuccess();
  }

  /**
   * Connects to the database defined in the specified properties file.
   *
   * @param line Command line
   * @param callback Callback for command status
   * @throws Exception on error
   */
  public void properties(String line, DispatchCallback callback)
      throws Exception {
    String example = "";
    example += "Usage: properties <properties file>" + SqlLine.getSeparator();

    String[] parts = sqlLine.split(line);
    if (parts.length < 2) {
      callback.setToFailure();
      sqlLine.error(example);
      return;
    }

    int successes = 0;

    for (int i = 1; i < parts.length; i++) {
      Properties props = new Properties();
      props.load(new FileInputStream(parts[i]));
      connect(props, callback);
      if (callback.isSuccess()) {
        successes++;
        String nickname = getProperty(props, "nickname", "ConnectionNickname");
        if (nickname != null) {
          sqlLine.getDatabaseConnection().setNickname(nickname);
        }
      }
    }

    if (successes != parts.length - 1) {
      callback.setToFailure();
    } else {
      callback.setToSuccess();
    }
  }

  public void connect(String line, DispatchCallback callback) {
    String example =
        "Usage: connect [-p property value]* <url> [username] [password] [driver]"
        + SqlLine.getSeparator();
    String[] parts = sqlLine.split(line);
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    Properties connectProps = new Properties();
    int offset = 1;
    for (int i = 1; i < parts.length; i++) {
      if ("-p".equals(parts[i])) {
        if (parts.length - i > 2) {
          connectProps.setProperty(parts[i + 1], parts[i + 2]);
          i = i + 2;
          offset += 3;
        } else {
          callback.setToFailure();
          sqlLine.error(example);
          return;
        }
      }
    }

    String url = parts.length < offset + 1 ? null : parts[offset];
    String user = parts.length < offset + 2 ? null : parts[offset + 1];
    String pass = parts.length < offset + 3 ? null : parts[offset + 2];
    String driver = parts.length < offset + 4 ? null : parts[offset + 3];
    Properties props = new Properties();
    if (url != null) {
      props.setProperty("url", url);
    }
    if (driver != null) {
      props.setProperty("driver", driver);
    }
    if (user != null) {
      props.setProperty("user", user);
    }
    if (pass != null) {
      props.setProperty("password", pass);
    }
    if (!connectProps.isEmpty()) {
      props.put(CONNECT_PROPERTY, connectProps);
    }
    connect(props, callback);
  }

  public void nickname(String line, DispatchCallback callback) {
    String example = "Usage: nickname <nickname for current connection>"
        + SqlLine.getSeparator();

    String[] parts = sqlLine.split(line);
    if (parts == null) {
      callback.setToFailure();
      sqlLine.error(example);
      return;
    }

    String nickname = parts.length < 2 ? null : parts[1];
    if (nickname != null) {
      DatabaseConnection current = sqlLine.getDatabaseConnection();
      if (current != null) {
        current.setNickname(nickname);
        callback.setToSuccess();
      } else {
        sqlLine.error("nickname command requires active connection");
      }
    } else {
      sqlLine.error(example);
    }
  }

  private String getProperty(Properties props, String... keys) {
    for (String key : keys) {
      String val = props.getProperty(key);
      if (val != null) {
        return val;
      }
    }

    for (String key : asMap(props).keySet()) {
      for (String key1 : keys) {
        if (key.endsWith(key1)) {
          return props.getProperty(key);
        }
      }
    }

    return null;
  }

  public void connect(Properties props, DispatchCallback callback) {
    String url = getProperty(props,
        "url",
        "javax.jdo.option.ConnectionURL",
        "ConnectionURL");
    String driver = getProperty(props,
        "driver",
        "javax.jdo.option.ConnectionDriverName",
        "ConnectionDriverName");
    String username = getProperty(props,
        "user",
        "javax.jdo.option.ConnectionUserName",
        "ConnectionUserName");
    String password = getProperty(props,
        "password",
        "javax.jdo.option.ConnectionPassword",
        "ConnectionPassword");
    Properties info = (Properties) props.get(CONNECT_PROPERTY);
    if (info != null) {
      url = url == null ? info.getProperty("url") : url;
      driver = driver == null ? info.getProperty("driver") : driver;
      username = username == null ? info.getProperty("user") : username;
      password = password == null ? info.getProperty("password") : password;
    }
    final String connectInteractionMode =
        sqlLine.getOpts().get(BuiltInProperty.CONNECT_INTERACTION_MODE);
    if (isBlank(username)
        && isBlank(password)
        && "useNPTogetherOrEmpty".equals(connectInteractionMode)) {
      username = password = "";
    }
    if (url == null || url.length() == 0) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("no-url"));
      return;
    }
    if (driver == null || driver.length() == 0) {
      if (sqlLine.scanForDriver(url) == null) {
        callback.setToFailure();
        sqlLine.error(sqlLine.loc("no-driver", url));
        return;
      }
    } else {
      try {
        Class.forName(driver);
      } catch (ClassNotFoundException cnfe) {
        String specifiedDriver = driver;
        if ((driver = sqlLine.scanForDriver(url)) == null) {
          callback.setToFailure();
          sqlLine.error(sqlLine.loc("no-specified-driver", specifiedDriver));
          return;
        }
        sqlLine.info(
            sqlLine.loc("no-specified-driver-use-existing", specifiedDriver,
                driver));
      }
    }

    sqlLine.debug("Connecting to " + url);
    if (!"notAskCredentials".equals(connectInteractionMode)) {
      if (username == null) {
        username = readUsername(url);
        username = isBlank(username) ? null : username;
      }
      if (password == null) {
        password = readPassword(url);
        password = isBlank(password) ? null : password;
      }
    }
    DatabaseConnection connection =
        new DatabaseConnection(sqlLine, driver, url, username, password, info);
    try {
      sqlLine.getDatabaseConnections().setConnection(connection);
      sqlLine.getDatabaseConnection().getConnection();
      callback.setToSuccess();
    } catch (Exception e) {
      connection.close();
      sqlLine.getDatabaseConnections().removeConnection(connection);
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /** Returns whether a string is null or empty. */
  private boolean isBlank(String s) {
    return s == null || s.isEmpty();
  }

  String readUsername(String url) {
    return sqlLine.withPrompting(() -> sqlLine.getLineReader()
        .readLine("Enter username for " + url + ": "));
  }

  String readPassword(String url) {
    return sqlLine.withPrompting(() -> sqlLine.getLineReader()
        .readLine("Enter password for " + url + ": ",
            null, new MaskingCallbackImpl('*'), null));
  }

  public void rehash(String line, DispatchCallback callback) {
    try {
      if (!sqlLine.assertConnection()) {
        callback.setToFailure();
      }

      if (sqlLine.getDatabaseConnection() != null) {
        sqlLine.getDatabaseConnection().setCompletions(false);
      }

      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /**
   * Lists the current connections.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void list(String line, DispatchCallback callback) {
    int index = 0;
    DatabaseConnections databaseConnections = sqlLine.getDatabaseConnections();
    sqlLine.info(
        sqlLine.loc("active-connections", databaseConnections.size()));

    for (DatabaseConnection databaseConnection : databaseConnections) {
      boolean closed;
      try {
        closed = databaseConnection.connection.isClosed();
      } catch (Exception e) {
        closed = true;
      }

      sqlLine.output(rpad(" #" + index++ + "", 5)
          + rpad(closed ? sqlLine.loc("closed") : sqlLine.loc("open"), 9)
          + rpad(databaseConnection.getNickname(), 20)
          + " " + databaseConnection.getUrl());
    }

    callback.setToSuccess();
  }

  public void all(String line, DispatchCallback callback) {
    int index = sqlLine.getDatabaseConnections().getIndex();
    boolean success = true;
    for (int i = 0; i < sqlLine.getDatabaseConnections().size(); i++) {
      sqlLine.getDatabaseConnections().setIndex(i);
      sqlLine.output(
          sqlLine.loc("executing-con", sqlLine.getDatabaseConnection()));

      // ### FIXME:  this is broken for multi-line SQL
      sql(line.substring("all ".length()), callback);
      success = callback.isSuccess() && success;
    }

    // restore index
    sqlLine.getDatabaseConnections().setIndex(index);
    if (success) {
      callback.setToSuccess();
    } else {
      callback.setToFailure();
    }
  }

  public void go(String line, DispatchCallback callback) {
    String[] parts = sqlLine.split(line, 2, "Usage: go <connection index>");
    if (parts == null) {
      callback.setToFailure();
      return;
    }

    boolean isNumber;
    int index = Integer.MIN_VALUE;
    try {
      index = Integer.parseInt(parts[1]);
      isNumber = true;
    } catch (Exception e) {
      isNumber = false;
    }
    if (!isNumber || !sqlLine.getDatabaseConnections().setIndex(index)) {
      sqlLine.error(sqlLine.loc("invalid-connection", parts[1]));
      list("", callback); // list the current connections
      callback.setToFailure();
      return;
    }

    callback.setToSuccess();
  }

  /**
   * Starts or stops saving a script to a file.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void script(String line, DispatchCallback callback) {
    if (sqlLine.getScriptOutputFile() == null) {
      startScript(line, callback);
    } else {
      stopScript(line, callback);
    }
  }

  /**
   * Stop writing to the script file and close the script.
   */
  private void stopScript(String line, DispatchCallback callback) {
    try {
      sqlLine.getScriptOutputFile().close();
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    sqlLine.output(sqlLine.loc("script-closed", sqlLine.getScriptOutputFile()));
    sqlLine.setScriptOutputFile(null);
    callback.setToSuccess();
  }

  /**
   * Start writing to the specified script file.
   */
  private void startScript(String line, DispatchCallback callback) {
    OutputFile outFile = sqlLine.getScriptOutputFile();
    if (outFile != null) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("script-already-running", outFile));
      return;
    }

    String filename;
    if (line.length() == "script".length()
        || (filename =
            sqlLine.dequote(line.substring("script".length() + 1))) == null) {
      sqlLine.error("Usage: script <file name>");
      callback.setToFailure();
      return;
    }

    try {
      outFile = new OutputFile(expand(filename));
      sqlLine.setScriptOutputFile(outFile);
      sqlLine.output(sqlLine.loc("script-started", outFile));
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /**
   * Runs a script from the specified file.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void run(String line, DispatchCallback callback) {
    String filename;
    if (line.length() == "run".length()
        || (filename =
            sqlLine.dequote(line.substring("run".length() + 1))) == null) {
      sqlLine.error("Usage: run <file name>");
      callback.setToFailure();
      return;
    }
    List<String> cmds = new LinkedList<>();

    try {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(
              new FileInputStream(expand(filename)), StandardCharsets.UTF_8))) {
        // ### NOTE: fix for sf.net bug 879427
        final StringBuilder cmd = new StringBuilder();
        boolean needsContinuation;
        for (;;) {
          final String scriptLine = reader.readLine();
          if (scriptLine == null) {
            break;
          }
          // we're continuing an existing command
          cmd.append(" \n");
          cmd.append(scriptLine);

          needsContinuation = isSqlContinuationRequired(cmd.toString());
          if (!needsContinuation && !cmd.toString().trim().isEmpty()) {
            cmds.add(maybeTrim(flush(cmd)));
          }
        }

        if (SqlLineParser.isSql(sqlLine, cmd.toString(),
            Parser.ParseContext.ACCEPT_LINE)) {
          // ### REVIEW: oops, somebody left the last command
          // unterminated; should we fix it for them or complain?
          // For now be nice and fix it.
          cmd.append(";");
          cmds.add(cmd.toString());
        }
      }

      // success only if all the commands were successful
      if (sqlLine.runCommands(cmds, callback) == cmds.size()) {
        callback.setToSuccess();
      } else {
        callback.setToFailure();
      }
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  /** Returns a line, trimmed if the
   * {@link BuiltInProperty#TRIM_SCRIPTS options require trimming}. */
  private String maybeTrim(String line) {
    return sqlLine.getOpts().getTrimScripts() ? line.trim() : line;
  }

  /** Expands "~" to the home directory.
   *
   * @param filename File name
   * @return Expanded file name
   */
  public static String expand(String filename) {
    if (filename.startsWith("~" + File.separator)) {
      try {
        String home = System.getProperty("user.home");
        if (home != null) {
          return home + filename.substring(1);
        }
      } catch (SecurityException e) {
        // ignore
      }
    }
    return filename;
  }

  /**
   * Starts or stops saving all output to a file.
   *
   * @param line Command line
   * @param callback Callback for command status
   */
  public void record(String line, DispatchCallback callback) {
    if (sqlLine.getRecordOutputFile() == null) {
      startRecording(line, callback);
    } else {
      stopRecording(line, callback);
    }
  }

  public void commandhandler(String line, DispatchCallback callback) {
    String[] cmd = sqlLine.split(line);
    if (cmd.length < 2) {
      sqlLine.error("Usage: commandhandler "
          + "<commandHandler class name> [<commandHandler class name>]*");
      callback.setToFailure();
      return;
    }

    final List<CommandHandler> commandHandlers =
        new ArrayList<CommandHandler>(sqlLine.getCommandHandlers());
    final Set<String> existingNames = new HashSet<>();
    for (CommandHandler existingCommandHandler : commandHandlers) {
      existingNames.addAll(existingCommandHandler.getNames());
    }

    int commandHandlerUpdateCount = 0;
    for (int i = 1; i < cmd.length; i++) {
      try {
        @SuppressWarnings("unchecked")
        Class<CommandHandler> commandHandlerClass =
            (Class<CommandHandler>) Class.forName(cmd[i]);
        final Constructor<CommandHandler> constructor =
            commandHandlerClass.getConstructor(SqlLine.class);
        CommandHandler commandHandler = constructor.newInstance(sqlLine);
        if (intersects(existingNames, commandHandler.getNames())) {
          sqlLine.error("Could not add command handler " + cmd[i] + " as one "
              + "of commands " + commandHandler.getNames() + " is already present");
        } else {
          commandHandlers.add(commandHandler);
          existingNames.addAll(commandHandler.getNames());
          ++commandHandlerUpdateCount;
        }
      } catch (Exception e) {
        sqlLine.error(e);
        callback.setToFailure();
      }
    }

    if (commandHandlerUpdateCount > 0) {
      sqlLine.updateCommandHandlers(commandHandlers);
    }

    if (!callback.isFailure()) {
      callback.setToSuccess();
    }
  }

  private static <E> boolean intersects(Collection<E> c1, Collection<E> c2) {
    for (E e : c2) {
      if (c1.contains(e)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Stop writing output to the record file.
   */
  private void stopRecording(String line, DispatchCallback callback) {
    try {
      sqlLine.getRecordOutputFile().close();
    } catch (Exception e) {
      sqlLine.handleException(e);
    }

    sqlLine.output(sqlLine.loc("record-closed", sqlLine.getRecordOutputFile()));
    sqlLine.setRecordOutputFile(null);
    callback.setToSuccess();
  }

  /**
   * Start writing to the specified record file.
   */
  private void startRecording(String line, DispatchCallback callback) {
    OutputFile outputFile = sqlLine.getRecordOutputFile();
    if (outputFile != null) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("record-already-running", outputFile));
      return;
    }
    String[] cmd = sqlLine.split(line);
    if (cmd.length != 2) {
      sqlLine.error("Usage: record <file name>");
      callback.setToFailure();
      return;
    }

    final String filename = cmd[1];

    try {
      outputFile = new OutputFile(expand(filename));
      sqlLine.setRecordOutputFile(outputFile);
      sqlLine.output(sqlLine.loc("record-started", outputFile));
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error(e);
    }
  }

  public void describe(String line, DispatchCallback callback)
      throws SQLException {
    String[][] cmd = sqlLine.splitCompound(line);
    if (cmd.length != 2) {
      sqlLine.error("Usage: describe <table name>");
      callback.setToFailure();
      return;
    }

    if (cmd[1].length == 1
        && cmd[1][0] != null
        && cmd[1][0].equalsIgnoreCase("tables")) {
      tables("tables", callback);
    } else {
      columns(line, callback);
    }
  }

  public void help(String line, DispatchCallback callback) {
    String[] parts = sqlLine.split(line);
    String cmd = parts.length > 1 ? parts[1] : "";
    TreeSet<String> clist = new TreeSet<>();

    for (CommandHandler commandHandler : sqlLine.getCommandHandlers()) {
      if (cmd.length() == 0
          || commandHandler.getNames().contains(cmd)) {
        String help = commandHandler.getHelpText();
        help = sqlLine.wrap(help, 60, 20);
        if (cmd.equals("set")) {
          help += sqlLine.loc("variables");
        }
        clist.add(rpad(SqlLine.COMMAND_PREFIX + commandHandler.getName(), 20)
            + help);
      }
    }

    for (String c : clist) {
      sqlLine.output(c);
    }

    if (cmd.length() == 0) {
      sqlLine.output(sqlLine.loc("variables"));
      sqlLine.output("");
      sqlLine.output(
          sqlLine.loc("comments", SqlLine.getApplicationContactInformation()));
    }

    callback.setToSuccess();
  }

  public void manual(String line, DispatchCallback callback)
      throws IOException {
    InputStream in = SqlLine.class.getResourceAsStream("manual.txt");
    if (in == null) {
      callback.setToFailure();
      sqlLine.error(sqlLine.loc("no-manual"));
      return;
    }

    // Workaround for windows because of
    // https://github.com/jline/jline3/issues/304
    if (System.getProperty("os.name")
        .toLowerCase(Locale.ROOT).contains("windows")) {
      sillyLess(in);
    } else {
      try {
        org.jline.builtins.Commands.less(sqlLine.getLineReader().getTerminal(),
            in, sqlLine.getOutputStream(), sqlLine.getErrorStream(),
            null, new String[]{});
      } catch (Exception e) {
        callback.setToFailure();
        sqlLine.error(e);
        return;
      }
    }

    callback.setToSuccess();
  }

  private void sillyLess(InputStream in) throws IOException {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    String man;
    int index = 0;
    while ((man = reader.readLine()) != null) {
      index++;
      sqlLine.output(man);

      // silly little pager
      if (index % (sqlLine.getOpts().getMaxHeight() - 1) == 0) {
        final int userInput =
            getUserAnswer(sqlLine.loc("enter-for-more"), 'q', 13);
        if (userInput == -1 || userInput == 'q') {
          sqlLine.getLineReader().getTerminal().writer().write('\n');
          break;
        }
      }
    }

    reader.close();
  }

  public void appconfig(String line, DispatchCallback callback) {
    String example =
        "Usage: appconfig <class name for application configuration>"
        + SqlLine.getSeparator();

    String[] parts = sqlLine.split(line);
    if (parts == null || parts.length != 2) {
      callback.setToFailure();
      sqlLine.error(example);
      return;
    }

    try {
      Application appConfig = (Application) Class.forName(parts[1])
          .getConstructor().newInstance();
      sqlLine.setAppConfig(appConfig);
      callback.setToSuccess();
    } catch (Exception e) {
      callback.setToFailure();
      sqlLine.error("Could not initialize " + parts[1]);
    }
  }

  public void prompthandler(String line, DispatchCallback callback) {
    String example =
        "Usage: prompthandler <prompt handler class name>"
          + SqlLine.getSeparator();

    String[] parts = sqlLine.split(line);
    if (parts == null || parts.length != 2) {
      callback.setToFailure();
      sqlLine.error(example);
      return;
    }

    String className = parts[1];

    PromptHandler promptHandler;
    if ("default".equalsIgnoreCase(className)) {
      promptHandler = new PromptHandler(sqlLine);
    } else {
      try {
        promptHandler = (PromptHandler) Class.forName(className)
          .getConstructor(SqlLine.class).newInstance(sqlLine);
      } catch (Exception e) {
        callback.setToFailure();
        sqlLine.error("Could not initialize " + className);
        return;
      }
    }

    sqlLine.updatePromptHandler(promptHandler);
    callback.setToSuccess();
  }

  static Map<String, String> asMap(Properties properties) {
    //noinspection unchecked
    return (Map) properties;
  }

  private boolean isSqlContinuationRequired(String sql) {
    if (sqlLine.getLineReader() == null) {
      return false;
    }
    return SqlLineParser.SqlParserState.OK
        != ((SqlLineParser) sqlLine.getLineReader().getParser())
            .parseState(sql, sql.length(), Parser.ParseContext.ACCEPT_LINE)
            .getState();
  }

  /**
   * Callback that masks input while reading a password.
   */
  private static class MaskingCallbackImpl implements MaskingCallback {
    private final Character mask;

    MaskingCallbackImpl(Character mask) {
      this.mask = Objects.requireNonNull(mask);
    }

    @Override public String display(String line) {
      if (mask.equals(LineReaderImpl.NULL_MASK)) {
        return "";
      } else {
        final StringBuilder sb = new StringBuilder(line.length());
        for (char c : line.toCharArray()) {
          if (c == '\n') {
            sb.append(c);
          } else {
            sb.append((char) mask);
          }
        }
        return sb.toString();
      }
    }

    @Override public String history(String line) {
      return null;
    }
  }
}

// End Commands.java
