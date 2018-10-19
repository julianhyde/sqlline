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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Defines the configuration of a SQLLine application.
 *
 * <p>This class can be extended to allow customizations for:
 * known drivers, output formats, commands,
 * information message, session options.
 *
 * <p>You can pass the name of the sub-class to SQLLine
 * via the {@code -ac} command-line parameter or {@code !appconfig} command.
 *
 * <p>Use {@code !appconfig sqlline.Application} to reset
 * SQLLine application configuration to default at runtime.
*/
public class Application {

  public static final String DEFAULT_APP_INFO_MESSAGE = "sqlline version ???";

  private static final String[] DRIVERS = {
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
      "weblogic.jdbc.pool.Driver",
  };

  private static final SortedSet<String> DEFAULT_DRIVERS =
      Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(DRIVERS)));

  private static final String[] CONNECTION_URLS = {
      "jdbc:JSQLConnect://<hostname>/database=<database>",
      "jdbc:cloudscape:<database>;create=true",
      "jdbc:twtds:sqlserver://<hostname>/<database>",
      "jdbc:daffodilDB_embedded:<database>;create=true",
      "jdbc:datadirect:db2://<hostname>:50000;databaseName=<database>",
      "jdbc:inetdae:<hostname>:1433",
      "jdbc:datadirect:oracle://<hostname>:1521;SID=<database>;"
          + "MaxPooledStatements=0",
      "jdbc:datadirect:sqlserver://<hostname>:1433;SelectMethod=cursor;"
          + "DatabaseName=<database>",
      "jdbc:datadirect:sybase://<hostname>:5000",
      "jdbc:db2://<hostname>/<database>",
      "jdbc:hsqldb:<database>",
      "jdbc:idb:<database>.properties",
      "jdbc:informix-sqli://<hostname>:1526/<database>:INFORMIXSERVER=<database>",
      "jdbc:interbase://<hostname>//<database>.gdb",
      "jdbc:luciddb:http://<hostname>",
      "jdbc:microsoft:sqlserver://<hostname>:1433;DatabaseName=<database>;"
          + "SelectMethod=cursor",
      "jdbc:mysql://<hostname>/<database>?autoReconnect=true",
      "jdbc:oracle:thin:@<hostname>:1521:<database>",
      "jdbc:pointbase:<database>,database.home=<database>,create=true",
      "jdbc:postgresql://<hostname>:5432/<database>",
      "jdbc:postgresql:net//<hostname>/<database>",
      "jdbc:sybase:Tds:<hostname>:4100/<database>?ServiceName=<database>",
      "jdbc:weblogic:mssqlserver4:<database>@<hostname>:1433",
      "jdbc:odbc:<database>",
      "jdbc:sequelink://<hostname>:4003/[Oracle]",
      "jdbc:sequelink://<hostname>:4004/[Informix];Database=<database>",
      "jdbc:sequelink://<hostname>:4005/[Sybase];Database=<database>",
      "jdbc:sequelink://<hostname>:4006/[SQLServer];Database=<database>",
      "jdbc:sequelink://<hostname>:4011/[ODBC MS Access];Database=<database>",
      "jdbc:openlink://<hostname>/DSN=SQLServerDB/UID=sa/PWD=",
      "jdbc:solid://<hostname>:<port>/<UID>/<PWD>",
      "jdbc:dbaw://<hostname>:8889/<database>",
  };

  private static final List<String> DEFAULT_CONNECTION_URL_EXAMPLES =
      Collections.unmodifiableList(Arrays.asList(CONNECTION_URLS));

  /** Creates an Application. */
  public Application() {
  }

  /**
   * Returns the information message, by default "sqlline version x.x".
   *
   * <p>Override this method to return a custom information message.
   *
   * @return custom information message
   * @see #DEFAULT_APP_INFO_MESSAGE
   */
  public String getInfoMessage() {
    return getVersion();
  }

  public String getVersion() {
    final String path = "/META-INF/maven/sqlline/sqlline/pom.properties";
    InputStream inputStream = getClass().getResourceAsStream(path);
    Properties properties = new Properties();
    properties.put("artifactId", "sqlline");
    properties.put("version", "???");
    if (inputStream != null) {
      // If not running from a .jar, pom.properties will not exist, and
      // inputStream is null.
      try {
        properties.load(inputStream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return String.format(Locale.ROOT, "%s version %s",
        properties.getProperty("artifactId"),
        properties.getProperty("version"));
  }

  /**
   * Returns the set of known JDBC drivers.
   *
   * <p>Override this method to modify set of supported known drivers.
   *
   * @return Collection of known drivers
   *
   * @see #DEFAULT_DRIVERS
   */
  public Collection<String> initDrivers() {
    return DEFAULT_DRIVERS;
  }

  /**
   * Override this method to modify known output formats implementations.
   *
   * <p>If method is not overridden, current state of formats will be
   * reset to default ({@code super.getOutputFormats(sqlLine)}).
   *
   * <p>To update / leave current state, override this method
   * and use {@code sqlLine.getOutputFormats()}.
   *
   * <p>When overriding output formats outputformat command
   * should be re-initialized unless default commands handlers are used.
   *
   * @param sqlLine SQLLine instance
   *
   * @return Map of output formats by name
   */
  public Map<String, OutputFormat> getOutputFormats(SqlLine sqlLine) {
    final Map<String, OutputFormat> outputFormats = new HashMap<>();
    outputFormats.put("vertical", new VerticalOutputFormat(sqlLine));
    outputFormats.put("table", new TableOutputFormat(sqlLine));
    outputFormats.put("csv", new SeparatedValuesOutputFormat(sqlLine, ","));
    outputFormats.put("tsv", new SeparatedValuesOutputFormat(sqlLine, "\t"));
    XmlAttributeOutputFormat xmlAttrs = new XmlAttributeOutputFormat(sqlLine);
    // leave "xmlattr" name for backward compatibility,
    // "xmlattrs" should be used instead
    outputFormats.put("xmlattr", xmlAttrs);
    outputFormats.put("xmlattrs", xmlAttrs);
    outputFormats.put("xmlelements", new XmlElementOutputFormat(sqlLine));
    outputFormats.put("json", new JsonOutputFormat(sqlLine));
    return Collections.unmodifiableMap(outputFormats);
  }

  /**
   * Override this method to modify connection url examples.
   *
   * <p>When overriding connection url examples, connect / open command
   * should be re-initialized unless default commands handlers are used.
   *
   * @return Collection of connection url examples
   */
  public Collection<String> getConnectionUrlExamples() {
    return DEFAULT_CONNECTION_URL_EXAMPLES;
  }

  /**
   * Override this method to modify supported commands.
   *
   * <p>If method is not overridden, current state of commands will be
   * reset to default ({@code super.getCommandHandlers(sqlLine)}).
   *
   * <p>To update / leave current state, override this method
   * and use {@code sqlLine.getCommandHandlers()}.
   *
   * @param sqlLine SQLLine instance
   *
   * @return Collection of command handlers
   */
  public Collection<CommandHandler> getCommandHandlers(SqlLine sqlLine) {
    TableNameCompleter tableCompleter = new TableNameCompleter(sqlLine);
    List<Completer> empty = Collections.emptyList();
    final Map<String, OutputFormat> outputFormats = getOutputFormats(sqlLine);
    final CommandHandler[] handlers = {
        new ReflectiveCommandHandler(sqlLine, empty, "quit", "done", "exit"),
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(getConnectionUrlExamples()), "connect",
            "open"),
        new ReflectiveCommandHandler(sqlLine, empty, "nickname"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "describe"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "indexes"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "primarykeys"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "exportedkeys"),
        new ReflectiveCommandHandler(sqlLine, empty, "manual"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "importedkeys"),
        new ReflectiveCommandHandler(sqlLine, empty, "procedures"),
        new ReflectiveCommandHandler(sqlLine, empty, "tables"),
        new ReflectiveCommandHandler(sqlLine, empty, "typeinfo"),
        new ReflectiveCommandHandler(sqlLine, empty, "commandhandler"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "columns"),
        new ReflectiveCommandHandler(sqlLine, empty, "reconnect"),
        new ReflectiveCommandHandler(sqlLine, tableCompleter, "dropall"),
        new ReflectiveCommandHandler(sqlLine, empty, "history"),
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(getMetadataMethodNames()), "metadata"),
        new ReflectiveCommandHandler(sqlLine, empty, "nativesql"),
        new ReflectiveCommandHandler(sqlLine, empty, "dbinfo"),
        new ReflectiveCommandHandler(sqlLine, empty, "rehash"),
        new ReflectiveCommandHandler(sqlLine, empty, "verbose"),
        new ReflectiveCommandHandler(sqlLine, new FileNameCompleter(), "run"),
        new ReflectiveCommandHandler(sqlLine, empty, "batch"),
        new ReflectiveCommandHandler(sqlLine, empty, "list"),
        new ReflectiveCommandHandler(sqlLine, empty, "all"),
        new ReflectiveCommandHandler(sqlLine, empty, "go", "#"),
        new ReflectiveCommandHandler(sqlLine, new FileNameCompleter(),
            "script"),
        new ReflectiveCommandHandler(sqlLine, new FileNameCompleter(),
            "record"),
        new ReflectiveCommandHandler(sqlLine, empty, "brief"),
        new ReflectiveCommandHandler(sqlLine, empty, "close"),
        new ReflectiveCommandHandler(sqlLine, empty, "closeall"),
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(getIsolationLevels()), "isolation"),
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(outputFormats.keySet()), "outputformat"),
        new ReflectiveCommandHandler(sqlLine, empty, "autocommit"),
        new ReflectiveCommandHandler(sqlLine, empty, "commit"),
        new ReflectiveCommandHandler(sqlLine, new FileNameCompleter(),
            "properties"),
        new ReflectiveCommandHandler(sqlLine, empty, "rollback"),
        new ReflectiveCommandHandler(sqlLine, empty, "help", "?"),
        new ReflectiveCommandHandler(sqlLine,
            getOpts(sqlLine).optionCompleters(), "set"),
        new ReflectiveCommandHandler(sqlLine,
            getOpts(sqlLine).optionCompleters(), "reset"),
        new ReflectiveCommandHandler(sqlLine, empty, "save"),
        new ReflectiveCommandHandler(sqlLine, empty, "scan"),
        new ReflectiveCommandHandler(sqlLine, empty, "sql"),
        new ReflectiveCommandHandler(sqlLine, empty, "call"),
        new ReflectiveCommandHandler(sqlLine, empty, "appconfig"),
        new ReflectiveCommandHandler(sqlLine, empty, "rerun", "/"),
    };
    return Collections.unmodifiableList(Arrays.asList(handlers));
  }

  /**
   * Override this method to modify session options.
   *
   * <p>If method is not overridden, current state of options will be
   * reset to default ({@code super.getOpts(sqlLine)}).
   *
   * <p>To update / leave current state, override this method
   * and use {@code sqlLine.getOpts()}.
   *
   * @param sqlLine SQLLine instance
   *
   * @return SQLLine session options
   */
  public SqlLineOpts getOpts(SqlLine sqlLine) {
    return new SqlLineOpts(sqlLine);
  }

  private Set<String> getMetadataMethodNames() {
    try {
      TreeSet<String> methodNames = new TreeSet<>();
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

// End Application.java
