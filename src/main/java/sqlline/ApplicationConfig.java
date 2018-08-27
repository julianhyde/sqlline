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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

/**
 * Application configuration base class.
 * This class can be extended to allow customizations for:
 * known drivers, output formats, commands, information message.
 * Extended class name should be passed to the Sqlline
 * via -ac / !appconfig commands.
*/
public class ApplicationConfig {

  public static final String DEFAULT_APP_INFO_MESSAGE = "sqlline version ???";

  private Set<String> knownDrivers;
  private Map<String, OutputFormat> outputFormats;
  private List<String> connectionURLExamples;
  private Map<String, CommandHandler> commandHandlers;

  public ApplicationConfig(SqlLine sqlLine) {
    knownDrivers = initDrivers();
    outputFormats = initOutputFormats(sqlLine);
    connectionURLExamples = initConnectionURLExamples();
    commandHandlers = initCommandHandlers(sqlLine);
  }

  public Set<String> getKnownDrivers() {
    return knownDrivers;
  }

  public Map<String, OutputFormat> getFormats() {
    return outputFormats;
  }

  public List<String> getConnectionURLExamples() {
    return connectionURLExamples;
  }

  public Collection<CommandHandler> getCommandHandlers() {
    return commandHandlers.values();
  }

  /**
   * Override this method to return custom information message.
   *
   * @return custom information message
   */
  public String getInfoMessage() throws Exception {
    InputStream inputStream = getClass().
        getResourceAsStream("/META-INF/maven/sqlline/sqlline/pom.properties");
    Properties properties = new Properties();
    properties.put("artifactId", "sqlline");
    properties.put("version", "???");
    if (inputStream != null) {
      // If not running from a .jar, pom.properties will not exist, and
      // inputStream is null.
      properties.load(inputStream);
    }
    return String.format("%s version %s",
        properties.getProperty("artifactId"),
        properties.getProperty("version"));
  }

  /**
   * Override this method to modify set of supported known drivers.
   *
   * @return set of known drivers
   */
  protected Set<String> initDrivers() {
    return new TreeSet<String>(
        Arrays.asList(
            "centura.java.sqlbase.SqlbaseDriver",
            "com.amazonaws.athena.jdbc.AthenaDriver",
            "COM.cloudscape.core.JDBCDriver",
            "com.ddtek.jdbc.db2.DB2Driver",
            "com.ddtek.jdbc.informix.InformixDriver",
            "com.ddtek.jdbc.oracle.OracleDriver",
            "com.ddtek.jdbc.sqlserver.SQLServerDriver",
            "com.ddtek.jdbc.sybase.SybaseDriver",
            "COM.FirstSQL.Dbcp.DbcpDriver",
            "com.ibm.as400.access.AS400JDBCDriver",
            "com.ibm.db2.jcc.DB2Driver",
            "COM.ibm.db2.jdbc.app.DB2Driver",
            "COM.ibm.db2.jdbc.net.DB2Driver",
            "com.imaginary.sql.msql.MsqlDriver",
            "com.inet.tds.TdsDriver",
            "com.informix.jdbc.IfxDriver",
            "com.internetcds.jdbc.tds.Driver",
            "com.internetcds.jdbc.tds.SybaseDriver",
            "com.jnetdirect.jsql.JSQLDriver",
            "com.lucidera.jdbc.LucidDbRmiDriver",
            "com.mckoi.JDBCDriver",
            "com.merant.datadirect.jdbc.db2.DB2Driver",
            "com.merant.datadirect.jdbc.informix.InformixDriver",
            "com.merant.datadirect.jdbc.oracle.OracleDriver",
            "com.merant.datadirect.jdbc.sqlserver.SQLServerDriver",
            "com.merant.datadirect.jdbc.sybase.SybaseDriver",
            "com.microsoft.jdbc.sqlserver.SQLServerDriver",
            "com.mysql.jdbc.DatabaseMetaData",
            "com.mysql.jdbc.Driver",
            "com.mysql.jdbc.NonRegisteringDriver",
            "com.pointbase.jdbc.jdbcDriver",
            "com.pointbase.jdbc.jdbcEmbeddedDriver",
            "com.pointbase.jdbc.jdbcUniversalDriver",
            "com.sap.dbtech.jdbc.DriverSapDB",
            "com.sqlstream.jdbc.Driver",
            "com.sybase.jdbc2.jdbc.SybDriver",
            "com.sybase.jdbc.SybDriver",
            "com.thinweb.tds.Driver",
            "in.co.daffodil.db.jdbc.DaffodilDBDriver",
            "interbase.interclient.Driver",
            "intersolv.jdbc.sequelink.SequeLinkDriver",
            "net.sourceforge.jtds.jdbc.Driver",
            "openlink.jdbc2.Driver",
            "oracle.jdbc.driver.OracleDriver",
            "oracle.jdbc.OracleDriver",
            "oracle.jdbc.pool.OracleDataSource",
            "org.axiondb.jdbc.AxionDriver",
            "org.enhydra.instantdb.jdbc.idbDriver",
            "org.gjt.mm.mysql.Driver",
            "org.hsqldb.jdbcDriver",
            "org.hsql.jdbcDriver",
            "org.luciddb.jdbc.LucidDbClientDriver",
            "org.postgresql.Driver",
            "org.sourceforge.jxdbcon.JXDBConDriver",
            "postgres95.PGDriver",
            "postgresql.Driver",
            "solid.jdbc.SolidDriver",
            "sun.jdbc.odbc.JdbcOdbcDriver",
            "weblogic.jdbc.mssqlserver4.Driver",
            "weblogic.jdbc.pool.Driver"));
  }

  /**
   * Override this method to modify known output formats implementations.
   *
   * @param sqlLine sqlline instance
   * @return map of output formats stored by name
   */
  protected Map<String, OutputFormat> initOutputFormats(SqlLine sqlLine) {
    Map<String, OutputFormat> outputFormats =
        new HashMap<String, OutputFormat>();
    outputFormats.put("vertical", new VerticalOutputFormat(sqlLine));
    outputFormats.put("table", new TableOutputFormat(sqlLine));
    outputFormats.put("csv", new SeparatedValuesOutputFormat(sqlLine, ","));
    outputFormats.put("tsv", new SeparatedValuesOutputFormat(sqlLine, "\t"));
    outputFormats.put("xmlattr", new XmlAttributeOutputFormat(sqlLine));
    outputFormats.put("xmlelements", new XmlElementOutputFormat(sqlLine));
    outputFormats.put("json", new JsonOutputFormat(sqlLine));
    return outputFormats;
  }

  /**
   * Override this method to modify connection url examples.
   *
   * @return list of connection url examples
   */
  protected List<String> initConnectionURLExamples() {
    List<String> examples = new ArrayList<String>();
    examples.add("jdbc:JSQLConnect://<hostname>/database=<database>");
    examples.add("jdbc:cloudscape:<database>;create=true");
    examples.add("jdbc:twtds:sqlserver://<hostname>/<database>");
    examples.add("jdbc:daffodilDB_embedded:<database>;create=true");
    examples.add("jdbc:datadirect:db2://<hostname>:50000;"
        + "databaseName=<database>");
    examples.add("jdbc:inetdae:<hostname>:1433");
    examples.add("jdbc:datadirect:oracle://<hostname>:1521;SID=<database>;"
        + "MaxPooledStatements=0");
    examples.add("jdbc:datadirect:sqlserver://<hostname>:1433;"
        + "SelectMethod=cursor;DatabaseName=<database>");
    examples.add("jdbc:datadirect:sybase://<hostname>:5000");
    examples.add("jdbc:db2://<hostname>/<database>");
    examples.add("jdbc:hsqldb:<database>");
    examples.add("jdbc:idb:<database>.properties");
    examples.add("jdbc:informix-sqli://<hostname>:1526/<database>:"
        + "INFORMIXSERVER=<database>");
    examples.add("jdbc:interbase://<hostname>//<database>.gdb");
    examples.add("jdbc:luciddb:http://<hostname>");
    examples.add("jdbc:microsoft:sqlserver://<hostname>:1433;"
        + "DatabaseName=<database>;SelectMethod=cursor");
    examples.add("jdbc:mysql://<hostname>/<database>?autoReconnect=true");
    examples.add("jdbc:oracle:thin:@<hostname>:1521:<database>");
    examples.add("jdbc:pointbase:<database>,database.home=<database>,"
        + "create=true");
    examples.add("jdbc:postgresql://<hostname>:5432/<database>");
    examples.add("jdbc:postgresql:net//<hostname>/<database>");
    examples.add("jdbc:sybase:Tds:<hostname>:4100/<database>?"
        + "ServiceName=<database>");
    examples.add("jdbc:weblogic:mssqlserver4:<database>@<hostname>:1433");
    examples.add("jdbc:odbc:<database>");
    examples.add("jdbc:sequelink://<hostname>:4003/[Oracle]");
    examples.add("jdbc:sequelink://<hostname>:4004/[Informix];"
        + "Database=<database>");
    examples.add("jdbc:sequelink://<hostname>:4005/[Sybase];"
        + "Database=<database>");
    examples.add("jdbc:sequelink://<hostname>:4006/[SQLServer];"
        + "Database=<database>");
    examples.add("jdbc:sequelink://<hostname>:4011/[ODBC MS Access];"
        + "Database=<database>");
    examples.add("jdbc:openlink://<hostname>/DSN=SQLServerDB/UID=sa/PWD=");
    examples.add("jdbc:solid://<hostname>:<port>/<UID>/<PWD>");
    examples.add("jdbc:dbaw://<hostname>:8889/<database>");
    return examples;
  }

  /**
   * Override this method to modify supported commands.
   *
   * @param sqlLine sqlline instance
   * @return map of command handlers stored by command name
   */
  protected Map<String, CommandHandler> initCommandHandlers(SqlLine sqlLine) {
    TableNameCompleter tableCompleter = new TableNameCompleter(sqlLine);
    List<Completer> empty = Collections.emptyList();
    Map<String, CommandHandler> commandHandlers =
        new HashMap<String, CommandHandler>();
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "quit", "done", "exit"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(getConnectionURLExamples()),
            "connect", "open"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "nickname"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "describe"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "indexes"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "primarykeys"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "exportedkeys"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "manual"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "importedkeys"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "procedures"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "tables"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "typeinfo"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "columns"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "reconnect"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "dropall"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "history"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
        new StringsCompleter(getMetadataMethodNames()), "metadata"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "nativesql"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "dbinfo"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "rehash"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "verbose"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, new FileNameCompleter(), "run"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "batch"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "list"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "all"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "go", "#"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
            new FileNameCompleter(), "script"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
            new FileNameCompleter(), "record"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "brief"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "close"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
        new StringsCompleter(getIsolationLevels()), "isolation"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
        new StringsCompleter(getFormats().keySet()), "outputformat"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "autocommit"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "commit"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
            new FileNameCompleter(), "properties"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "rollback"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "help", "?"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine,
            sqlLine.getOpts().optionCompleters(), "set"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "save"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "scan"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "sql"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "call"));
    addCommandHandler(commandHandlers,
        new ReflectiveCommandHandler(sqlLine, empty, "appconfig"));
    return commandHandlers;
  }

  private void addCommandHandler(Map<String, CommandHandler> commandHandlers,
                                 CommandHandler commandHandler) {
    commandHandlers.put(commandHandler.getName(), commandHandler);
  }

  private Set<String> getMetadataMethodNames() {
    try {
      TreeSet<String> methodNames = new TreeSet<String>();
      for (Method method : DatabaseMetaData.class.getDeclaredMethods()) {
        methodNames.add(method.getName());
      }

      return methodNames;
    } catch (Throwable t) {
      return Collections.emptySet();
    }
  }

  private List<String> getIsolationLevels() {
    return Arrays.asList(
        "TRANSACTION_NONE",
        "TRANSACTION_READ_COMMITTED",
        "TRANSACTION_READ_UNCOMMITTED",
        "TRANSACTION_REPEATABLE_READ",
        "TRANSACTION_SERIALIZABLE");
  }

}
