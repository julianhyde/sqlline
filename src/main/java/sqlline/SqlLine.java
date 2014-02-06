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
import java.lang.reflect.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.util.jar.*;

import jline.*;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;

/**
 * A console SQL shell with command completion.
 * <p>
 * TODO:
 * <ul>
 * <li>User-friendly connection prompts</li>
 * <li>Page results</li>
 * <li>Handle binary data (blob fields)</li>
 * <li>Implement command aliases</li>
 * <li>Stored procedure execution</li>
 * <li>Binding parameters to prepared statements</li>
 * <li>Scripting language</li>
 * <li>XA transactions</li>
 * </ul>
 */
public class SqlLine {
  private static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle(SqlLine.class.getName());

  private static final String separator = System.getProperty("line.separator");
  private boolean exit = false;
  private final DatabaseConnections connections = new DatabaseConnections();
  public static final String COMMAND_PREFIX = "!";
  private Collection<Driver> drivers = null;
  private final SqlLineOpts opts = new SqlLineOpts(this, System.getProperties());
  private String lastProgress = null;
  private final Map<SQLWarning, Date> seenWarnings = new HashMap<SQLWarning, Date>();
  private final Commands commands = new Commands(this);
  private OutputFile scriptOutputFile = null;
  private OutputFile recordOutputFile = null;
  private PrintStream outputStream = new PrintStream(System.out, true);
  private PrintStream errorStream = new PrintStream(System.err, true);
  private ConsoleReader consoleReader;
  private List<String> batch = null;
  private final Reflector reflector;

  // saveDir() is used in various opts that assume it's set. But that means
  // properties starting with "sqlline" are read into props in unspecific
  // order using reflection to find setter methods. Avoid
  // confusion/NullPointer due about order of config by prefixing it.
  public static final String SQLLINE_BASE_DIR = "x.sqlline.basedir";

  static final Object[] EMPTY_OBJ_ARRAY = new Object[0];

  private static boolean initComplete = false;

  private SqlLineSignalHandler signalHandler = null;
  private Completer sqlLineCommandCompleter;

  private final Map<String, OutputFormat> formats = map(
      "vertical", (OutputFormat) new VerticalOutputFormat(this),
      "table", new TableOutputFormat(this),
      "csv", new SeparatedValuesOutputFormat(this, ','),
      "tsv", new SeparatedValuesOutputFormat(this, '\t'),
      "xmlattr", new XmlAttributeOutputFormat(this),
      "xmlelements", new XmlElementOutputFormat(this));

  CommandHandler[] commandHandlers = new CommandHandler[] {
      new ReflectiveCommandHandler(this, new String[] {"quit", "done", "exit"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"connect", "open"},
          new Completer[] {new StringsCompleter(getConnectionURLExamples())}),
      new ReflectiveCommandHandler(this, new String[] {"describe"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"indexes"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"primarykeys"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"exportedkeys"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"manual"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"importedkeys"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"procedures"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"tables"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"typeinfo"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"columns"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"reconnect"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"dropall"},
          new Completer[] {new TableNameCompleter(this)}),
      new ReflectiveCommandHandler(this, new String[] {"history"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"metadata"},
          new Completer[] {
              new StringsCompleter(getMetadataMethodNames())}),
      new ReflectiveCommandHandler(this, new String[] {"nativesql"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"dbinfo"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"rehash"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"verbose"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"run"},
          new Completer[] {new FileNameCompleter()}),
      new ReflectiveCommandHandler(this, new String[] {"batch"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"list"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"all"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"go", "#"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"script"},
          new Completer[] {new FileNameCompleter()}),
      new ReflectiveCommandHandler(this, new String[] {"record"},
          new Completer[] {new FileNameCompleter()}),
      new ReflectiveCommandHandler(this, new String[] {"brief"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"close"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"closeall"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"isolation"},
          new Completer[] {new StringsCompleter(getIsolationLevels())}),
      new ReflectiveCommandHandler(this, new String[] {"outputformat"},
          new Completer[] {
              new StringsCompleter(formats.keySet().toArray(new String[0]))}),
      new ReflectiveCommandHandler(this, new String[] {"autocommit"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"commit"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"properties"},
          new Completer[] {new FileNameCompleter()}),
      new ReflectiveCommandHandler(this, new String[] {"rollback"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"help", "?"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"set"},
          opts.optionCompleters()),
      new ReflectiveCommandHandler(this, new String[] {"save"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"scan"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"sql"},
          null),
      new ReflectiveCommandHandler(this, new String[] {"call"},
          null),
  };

  static final SortedSet<String> KNOWN_DRIVERS = new TreeSet<String>(Arrays.asList(
      new String[] {
          "com.merant.datadirect.jdbc.sqlserver.SQLServerDriver",
          "com.microsoft.jdbc.sqlserver.SQLServerDriver",
          "com.ddtek.jdbc.informix.InformixDriver",
          "org.sourceforge.jxdbcon.JXDBConDriver",
          "com.ddtek.jdbc.oracle.OracleDriver",
          "net.sourceforge.jtds.jdbc.Driver",
          "com.pointbase.jdbc.jdbcDriver",
          "com.internetcds.jdbc.tds.SybaseDriver",
          "org.enhydra.instantdb.jdbc.idbDriver",
          "com.sybase.jdbc2.jdbc.SybDriver",
          "com.ddtek.jdbc.sybase.SybaseDriver",
          "COM.cloudscape.core.JDBCDriver",
          "in.co.daffodil.db.jdbc.DaffodilDBDriver",
          "com.jnetdirect.jsql.JSQLDriver",
          "com.lucidera.jdbc.LucidDbRmiDriver",
          "COM.ibm.db2.jdbc.net.DB2Driver",
          "org.hsqldb.jdbcDriver",
          "com.pointbase.jdbc.jdbcUniversalDriver",
          "com.ddtek.jdbc.sqlserver.SQLServerDriver",
          "com.ddtek.jdbc.db2.DB2Driver",
          "com.merant.datadirect.jdbc.oracle.OracleDriver",
          "oracle.jdbc.OracleDriver",
          "com.informix.jdbc.IfxDriver",
          "com.merant.datadirect.jdbc.informix.InformixDriver",
          "com.ibm.db2.jcc.DB2Driver",
          "com.pointbase.jdbc.jdbcEmbeddedDriver",
          "org.gjt.mm.mysql.Driver",
          "org.postgresql.Driver",
          "com.mysql.jdbc.Driver",
          "oracle.jdbc.driver.OracleDriver",
          "interbase.interclient.Driver",
          "com.mysql.jdbc.NonRegisteringDriver",
          "com.merant.datadirect.jdbc.db2.DB2Driver",
          "com.merant.datadirect.jdbc.sybase.SybaseDriver",
          "com.internetcds.jdbc.tds.Driver",
          "org.hsqldb.jdbcDriver",
          "org.hsql.jdbcDriver",
          "COM.cloudscape.core.JDBCDriver",
          "in.co.daffodil.db.jdbc.DaffodilDBDriver",
          "com.ddtek.jdbc.db2.DB2Driver",
          "interbase.interclient.Driver",
          "com.mysql.jdbc.Driver",
          "com.ddtek.jdbc.oracle.OracleDriver",
          "org.postgresql.Driver",
          "com.pointbase.jdbc.jdbcUniversalDriver",
          "org.sourceforge.jxdbcon.JXDBConDriver",
          "com.ddtek.jdbc.sqlserver.SQLServerDriver",
          "com.jnetdirect.jsql.JSQLDriver",
          "com.microsoft.jdbc.sqlserver.SQLServerDriver",
          "weblogic.jdbc.mssqlserver4.Driver",
          "com.ddtek.jdbc.sybase.SybaseDriver",
          "oracle.jdbc.pool.OracleDataSource",
          "org.axiondb.jdbc.AxionDriver",
          "COM.ibm.db2.jdbc.app.DB2Driver",
          "com.ibm.as400.access.AS400JDBCDriver",
          "COM.FirstSQL.Dbcp.DbcpDriver",
          "COM.ibm.db2.jdbc.net.DB2Driver",
          "org.enhydra.instantdb.jdbc.idbDriver",
          "com.informix.jdbc.IfxDriver",
          "com.microsoft.jdbc.sqlserver.SQLServerDriver",
          "com.imaginary.sql.msql.MsqlDriver",
          "sun.jdbc.odbc.JdbcOdbcDriver",
          "oracle.jdbc.driver.OracleDriver",
          "intersolv.jdbc.sequelink.SequeLinkDriver",
          "openlink.jdbc2.Driver",
          "com.pointbase.jdbc.jdbcUniversalDriver",
          "postgres95.PGDriver",
          "postgresql.Driver",
          "solid.jdbc.SolidDriver",
          "centura.java.sqlbase.SqlbaseDriver",
          "interbase.interclient.Driver",
          "com.mckoi.JDBCDriver",
          "com.inet.tds.TdsDriver",
          "com.microsoft.jdbc.sqlserver.SQLServerDriver",
          "com.thinweb.tds.Driver",
          "weblogic.jdbc.mssqlserver4.Driver",
          "com.mysql.jdbc.DatabaseMetaData",
          "org.gjt.mm.mysql.Driver",
          "com.sap.dbtech.jdbc.DriverSapDB",
          "com.sybase.jdbc2.jdbc.SybDriver",
          "com.sybase.jdbc.SybDriver",
          "com.internetcds.jdbc.tds.Driver",
          "weblogic.jdbc.pool.Driver",
          "com.sqlstream.jdbc.Driver",
          "org.luciddb.jdbc.LucidDbClientDriver",
      }));

  static {
    String testClass = "jline.console.ConsoleReader";
    try {
      Class.forName(testClass);
    } catch (Throwable t) {
      String message = locStatic(resourceBundle, System.err, "jline-missing", testClass);
      throw new ExceptionInInitializerError(message);
    }
  }

  static Manifest getManifest()
      throws IOException {
    URL base = SqlLine.class.getResource("/META-INF/MANIFEST.MF");
    URLConnection c = base.openConnection();
    if (c instanceof JarURLConnection) {
      return ((JarURLConnection) c).getManifest();
    }

    return null;
  }

  static String getManifestAttribute(String name) {
    try {
      Manifest m = getManifest();
      if (m == null) {
        return "??";
      }

      Attributes attrs = m.getAttributes("sqlline");
      if (attrs == null) {
        return "???";
      }

      String val = attrs.getValue(name);
      if (val == null || "".equals(val)) {
        return "????";
      }

      return val;
    } catch (Exception e) {
      e.printStackTrace();
      return "?????";
    }
  }

  String getApplicationTitle() {
    InputStream inputStream =
        getClass().getResourceAsStream(
            "/META-INF/maven/sqlline/sqlline/pom.properties");
    Properties properties = new Properties();
    properties.put("artifactId", "sqlline");
    properties.put("version", "???");
    try {
      properties.load(inputStream);
    } catch (IOException e) {
      handleException(e);
    }

    return loc(
        "app-introduction",
        properties.getProperty("artifactId"),
        properties.getProperty("version"));
  }

  static String getApplicationContactInformation() {
    return getManifestAttribute("Implementation-Vendor");
  }

  String loc(String res) {
    return loc(res, EMPTY_OBJ_ARRAY);
  }

  String loc(String res, int param) {
    try {
      return MessageFormat.format(
          new ChoiceFormat(resourceBundle.getString(res)).format(param),
          param);
    } catch (Exception e) {
      return res + ": " + param;
    }
  }

  String loc(String res, Object param1) {
    return loc(res, new Object[] {param1});
  }

  String loc(String res, Object param1, Object param2) {
    return loc(res, new Object[] {param1, param2});
  }

  String loc(String res, Object[] params) {
    return locStatic(resourceBundle, getErrorStream(), res, params);
  }

  static String locStatic(ResourceBundle resourceBundle, PrintStream err, String res, Object... params) {
    try {
      return MessageFormat.format(resourceBundle.getString(res), params);
    } catch (Exception e) {
      e.printStackTrace(err);

      try {
        return res + ": " + Arrays.asList(params);
      } catch (Exception e2) {
        return res;
      }
    }
  }

  protected String locElapsedTime(long milliseconds) {
    return loc("time-ms", new Object[] {milliseconds / 1000d});
  }

  /**
   * Starts the program.
   */
  public static void main(String[] args)
      throws IOException {
    start(args, null, true);
  }

  /**
   * Starts the program with redirected input. For redirected output,
   * System.setOut and System.setErr can be used, but System.setIn will not
   * work.
   *
   * @param args        same as main()
   * @param inputStream redirected input, or null to use standard input
   */
  public static boolean mainWithInputRedirection(
      String[] args,
      InputStream inputStream)
      throws IOException {
    return start(args, inputStream, false);
  }

  public SqlLine() {
    sqlLineCommandCompleter = new SqlLineCommandCompleter(this);
    reflector = new Reflector(this);

    // attempt to dynamically load signal handler
    try {
      Class handlerClass = Class.forName("sqlline.SunSignalHandler");
      signalHandler = (SqlLineSignalHandler) handlerClass.newInstance();
    } catch (Throwable t) {
      handleException(t);
    }
  }

  /**
   * Backwards compatibility method to allow
   * {@link #mainWithInputRedirection(String[], java.io.InputStream)} proxied
   * calls to keep method signature but add in new behavior of not saving
   * queries.
   *
   * @param args        args[] passed in directly from {@link #main(String[])}
   * @param inputStream Stream to read sql commands from (stdin or a file) or
   *                    null for an interactive shell
   * @param saveHistory whether or not the commands issued will be saved to
   *                    sqlline's history file
   * @throws IOException
   */
  public static boolean start(
      String[] args,
      InputStream inputStream,
      boolean saveHistory) throws IOException {
    SqlLine sqlline = new SqlLine();
    boolean retVal = sqlline.begin(args, inputStream, saveHistory);

    // exit the system: useful for Hypersonic and other
    // badly-behaving systems
    if (!Boolean.getBoolean(SqlLineOpts.PROPERTY_NAME_EXIT)) {
      System.exit(retVal ? 1 : 0);
    }

    return retVal;
  }

  DatabaseConnection getDatabaseConnection() {
    return connections.current();
  }

  Connection getConnection() {
    if (getDatabaseConnections().current() == null) {
      throw new IllegalArgumentException(loc("no-current-connection"));
    }
    if (getDatabaseConnections().current().connection == null) {
      throw new IllegalArgumentException(loc("no-current-connection"));
    }
    return getDatabaseConnections().current().connection;
  }

  DatabaseMetaData getDatabaseMetaData() {
    if (getDatabaseConnections().current() == null) {
      throw new IllegalArgumentException(loc("no-current-connection"));
    }
    if (getDatabaseConnections().current().getDatabaseMetaData() == null) {
      throw new IllegalArgumentException(loc("no-current-connection"));
    }
    return connections.current().getDatabaseMetaData();
  }

  public String[] getIsolationLevels() {
    return new String[] {
        "TRANSACTION_NONE",
        "TRANSACTION_READ_COMMITTED",
        "TRANSACTION_READ_UNCOMMITTED",
        "TRANSACTION_REPEATABLE_READ",
        "TRANSACTION_SERIALIZABLE",
    };
  }

  public String[] getMetadataMethodNames() {
    try {
      TreeSet<String> mnames = new TreeSet<String>();
      Method[] m = DatabaseMetaData.class.getDeclaredMethods();
      for (int i = 0; m != null && i < m.length; i++) {
        mnames.add(m[i].getName());
      }

      return mnames.toArray(new String[0]);
    } catch (Throwable t) {
      return new String[0];
    }
  }

  public String[] getConnectionURLExamples() {
    return new String[] {
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
        "jdbc:informix-sqli://<hostname>:1526/<database>:INFORMIXSERVER"
            + "=<database>",
        "jdbc:interbase://<hostname>//<database>.gdb",
        "jdbc:luciddb:http://<hostname>",
        "jdbc:microsoft:sqlserver://<hostname>:1433;"
            + "DatabaseName=<database>;SelectMethod=cursor",
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
        "jdbc:sequelink://<hostname>:4011/[ODBC MS Access];"
            + "Database=<database>",
        "jdbc:openlink://<hostname>/DSN=SQLServerDB/UID=sa/PWD=",
        "jdbc:solid://<hostname>:<port>/<UID>/<PWD>",
        "jdbc:dbaw://<hostname>:8889/<database>",
    };
  }

  /**
   * Entry point to creating a {@link ColorBuffer} with color
   * enabled or disabled depending on the value of {@link SqlLineOpts#getColor}.
   */
  ColorBuffer getColorBuffer() {
    return new ColorBuffer(getOpts().getColor());
  }

  /**
   * Entry point to creating a {@link ColorBuffer} with color enabled or
   * disabled depending on the calue of {@link SqlLineOpts#getColor}.
   */
  ColorBuffer getColorBuffer(String msg) {
    return new ColorBuffer(msg, getOpts().getColor());
  }

  /**
   * Walk through all the known drivers and try to register them.
   */
  void registerKnownDrivers() {
    for (Iterator<String> i = KNOWN_DRIVERS.iterator(); i.hasNext();) {
      try {
        Class.forName(i.next());
      } catch (Throwable t) {
      }
    }
  }

  boolean initArgs(String[] args) {
    List<String> commands = new LinkedList<String>();
    List<String> files = new LinkedList<String>();
    String driver = null, user = null, pass = null, url = null;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--help") || args[i].equals("-h")) {
        usage();
        return false;
      }

      // -- arguments are treated as properties
      if (args[i].startsWith("--")) {
        String[] parts = split(args[i].substring(2), "=");
        debug(loc("setting-prop", Arrays.asList(parts)));
        if (parts.length > 0) {
          boolean ret;

          if (parts.length >= 2) {
            ret = opts.set(parts[0], parts[1], true);
          } else {
            ret = opts.set(parts[0], "true", true);
          }

          if (!ret) {
            return false;
          }
        }

        continue;
      }

      if (args[i].equals("-d")) {
        driver = args[i++ + 1];
      } else if (args[i].equals("-n")) {
        user = args[i++ + 1];
      } else if (args[i].equals("-p")) {
        pass = args[i++ + 1];
      } else if (args[i].equals("-u")) {
        url = args[i++ + 1];
      } else if (args[i].equals("-e")) {
        commands.add(args[i++ + 1]);
      } else {
        files.add(args[i]);
      }
    }

    if (url != null) {
      String com =
          COMMAND_PREFIX + "connect "
              + url + " "
              + (user == null || user.length() == 0 ? "''" : user) + " "
              + (pass == null || pass.length() == 0 ? "''" : pass) + " "
              + (driver == null ? "" : driver);
      debug("issuing: " + com);
      dispatch(com, new DispatchCallback());
    }

    // now load properties files
    for (Iterator<String> i = files.iterator(); i.hasNext();) {
      dispatch(COMMAND_PREFIX + "properties " + i.next(),
          new DispatchCallback());
    }

    if (commands.size() > 0) {
      // for single command execute, disable color
      opts.setColor(false);
      opts.setHeaderInterval(-1);

      for (Iterator<String> i = commands.iterator(); i.hasNext();) {
        String command = i.next();
        debug(loc("executing-command", command));
        dispatch(command, new DispatchCallback());
      }

      exit = true; // execute and exit
    }

    // if a script file was specified, run the file and quit
    if (opts.getRun() != null) {
      dispatch(
          COMMAND_PREFIX + "run " + opts.getRun(),
          new DispatchCallback());
      dispatch(COMMAND_PREFIX + "quit", new DispatchCallback());
    }

    return true;
  }

  /**
   * Start accepting input from stdin, and dispatch it to the appropriate
   * {@link CommandHandler} until the global variable <code>exit</code> is
   * true.
   */
  boolean begin(String[] args, InputStream inputStream, boolean saveHistory)
      throws IOException {
    try {
      opts.load();
    } catch (Exception e) {
      handleException(e);
    }

    FileHistory fileHistory =
        new FileHistory(new File(opts.getHistoryFile()));
    ConsoleReader reader = getConsoleReader(inputStream, fileHistory);
    if (!initArgs(args)) {
      usage();
      return false;
    }

    try {
      info(getApplicationTitle());
    } catch (Exception e) {
      handleException(e);
    }

    // basic setup done. From this point on, honor opts value for showing
    // exception
    initComplete = true;
    DispatchCallback callback = new DispatchCallback();
    while (!exit) {
      try {
        signalHandler.setCallback(callback);
        dispatch(reader.readLine(getPrompt()), callback);
        if (saveHistory) {
          fileHistory.flush();
        }
      } catch (EOFException eof) {
        // CTRL-D
        commands.quit(null, callback);
      } catch (UserInterruptException ioe) {
        // CTRL-C
        try {
          callback.forceKillSqlQuery();
          callback.setToCancel();
          output(loc("command-canceled"));
        } catch (SQLException sqle) {
          handleException(sqle);
        }
      } catch (Throwable t) {
        handleException(t);
        callback.setToFailure();
      }
    }
    // ### NOTE jvs 10-Aug-2004:  Clean up any outstanding
    // connections automatically.
    // nothing is done with the callback beyond
    commands.closeall(null, new DispatchCallback());
    return callback.isSuccess();
  }

  public ConsoleReader getConsoleReader(
      InputStream inputStream, FileHistory fileHistory)
      throws IOException {
    Terminal terminal = TerminalFactory.create();
    try {
      terminal.init();
    } catch (Exception e) {
      // For backwards compatibility with code that used to use this lib
      // and expected only IOExceptions, convert back to that. We can't
      // use IOException(Throwable) constructor, which is only JDK 1.6 and
      // later.
      final IOException ioException = new IOException(e.toString());
      ioException.initCause(e);
      throw ioException;
    }
    if (inputStream != null) {
      // ### NOTE:  fix for sf.net bug 879425.
      consoleReader =
          new ConsoleReader(
              inputStream,
              System.out);
    } else {
      consoleReader = new ConsoleReader();
    }

    consoleReader.addCompleter(new SqlLineCompleter(this));
    consoleReader.setHistory(fileHistory);
    consoleReader.setHandleUserInterrupt(true); // CTRL-C handling
    consoleReader.setExpandEvents(false);

    return consoleReader;
  }

  void usage() {
    output(loc("cmd-usage"));
  }

  /**
   * Dispatch the specified line to the appropriate {@link CommandHandler}.
   *
   * @param line
   *          the command-line to dispatch
   */
  void dispatch(String line, DispatchCallback callback) {
    if (line == null) {
      // exit
      exit = true;
      return;
    }

    if (line.trim().length() == 0) {
      return;
    }

    if (isComment(line)) {
      return;
    }

    line = line.trim();

    // save it to the current script, if any
    if (scriptOutputFile != null) {
      scriptOutputFile.addLine(line);
    }

    if (isHelpRequest(line)) {
      line = COMMAND_PREFIX + "help";
    }

    if (line.startsWith(COMMAND_PREFIX)) {
      Map<String, CommandHandler> cmdMap =
          new TreeMap<String, CommandHandler>();
      line = line.substring(1);
      for (int i = 0; i < commandHandlers.length; i++) {
        String match = commandHandlers[i].matches(line);
        if (match != null) {
          cmdMap.put(match, commandHandlers[i]);
        }
      }

      if (cmdMap.size() == 0) {
        callback.setStatus(DispatchCallback.Status.FAILURE);
        error(loc("unknown-command", line));
      } else if (cmdMap.size() > 1) {
        callback.setStatus(DispatchCallback.Status.FAILURE);
        error(
            loc(
                "multiple-matches",
                cmdMap.keySet().toString()));
      } else {
        callback.setStatus(DispatchCallback.Status.RUNNING);
        cmdMap.values().iterator().next().execute(line, callback);
      }
    } else {
      callback.setStatus(DispatchCallback.Status.RUNNING);
      commands.sql(line, callback);
    }
  }

  /**
   * Test whether a line requires a continuation.
   *
   * @param line the line to be tested
   * @return true if continuation required
   */
  boolean needsContinuation(String line) {
    if (null == line) {
      // happens when CTRL-C used to exit a malformed.
      return false;
    }

    if (isHelpRequest(line)) {
      return false;
    }

    if (line.startsWith(COMMAND_PREFIX)) {
      return false;
    }

    if (isComment(line)) {
      return false;
    }

    String trimmed = line.trim();

    if (trimmed.length() == 0) {
      return false;
    }

    return !trimmed.endsWith(";");
  }

  /**
   * Test whether a line is a help request other than !help.
   *
   * @param line the line to be tested
   * @return true if a help request
   */
  boolean isHelpRequest(String line) {
    return line.equals("?") || line.equalsIgnoreCase("help");
  }

  /**
   * Test whether a line is a comment.
   *
   * @param line the line to be tested
   * @return true if a comment
   */
  boolean isComment(String line) {
    // SQL92 comment prefix is "--"
    // sqlline also supports shell-style "#" prefix
    return line.startsWith("#") || line.startsWith("--");
  }

  /**
   * Print the specified message to the console
   *
   * @param msg the message to print
   */
  void output(String msg) {
    output(msg, true);
  }

  void info(String msg) {
    if (!opts.getSilent()) {
      output(msg, true, System.err);
    }
  }

  void info(ColorBuffer msg) {
    if (!opts.getSilent()) {
      output(msg, true, System.err);
    }
  }

  /**
   * Issue the specified error message
   *
   * @param msg the message to issue
   * @return false always
   */
  boolean error(String msg) {
    output(getColorBuffer().red(msg), true, System.err);
    return false;
  }

  boolean error(Throwable t) {
    handleException(t);
    return false;
  }

  void debug(String msg) {
    if (opts.getVerbose()) {
      output(getColorBuffer().blue(msg), true, System.err);
    }
  }

  void output(ColorBuffer msg) {
    output(msg, true);
  }

  void output(String msg, boolean newline, PrintStream out) {
    output(getColorBuffer(msg), newline, out);
  }

  void output(ColorBuffer msg, boolean newline) {
    output(msg, newline, System.out);
  }

  void output(ColorBuffer msg, boolean newline, PrintStream out) {
    if (newline) {
      out.println(msg.getColor());
    } else {
      out.print(msg.getColor());
    }

    if (recordOutputFile == null) {
      return;
    }

    // only write to the record file if we are writing a line ...
    // otherwise we might get garbage from backspaces and such.
    if (newline) {
      recordOutputFile.addLine(msg.getMono()); // always just write mono
    }
  }

  /**
   * Print the specified message to the console
   *
   * @param msg     the message to print
   * @param newline if false, do not append a newline
   */
  void output(String msg, boolean newline) {
    output(getColorBuffer(msg), newline);
  }

  void autocommitStatus(Connection c)
      throws SQLException {
    debug(loc("autocommit-status", c.getAutoCommit() + ""));
  }

  /**
   * Ensure that autocommit is on for the current connection
   *
   * @return true if autocommit is set
   */
  boolean assertAutoCommit() {
    if (!assertConnection()) {
      return false;
    }

    try {
      if (getDatabaseConnection().connection.getAutoCommit()) {
        return error(loc("autocommit-needs-off"));
      }
    } catch (Exception e) {
      return error(e);
    }

    return true;
  }

  /**
   * Assert that we have an active, living connection. Print an error message
   * if we do not.
   *
   * @return true if there is a current, active connection
   */
  boolean assertConnection() {
    try {
      if (getDatabaseConnection() == null || getDatabaseConnection().connection == null) {
        return error(loc("no-current-connection"));
      }

      if (getDatabaseConnection().connection.isClosed()) {
        return error(loc("connection-is-closed"));
      }
    } catch (SQLException sqle) {
      return error(loc("no-current-connection"));
    }

    return true;
  }

  /**
   * Print out any warnings that exist for the current connection.
   */
  void showWarnings() {
    if (getDatabaseConnection().connection == null) {
      return;
    }

    if (!opts.getShowWarnings()) {
      return;
    }

    try {
      showWarnings(getDatabaseConnection().connection.getWarnings());
    } catch (Exception e) {
      handleException(e);
    }
  }

  /**
   * Print the specified warning on the console, as well as any warnings that
   * are returned from {@link SQLWarning#getNextWarning}.
   *
   * @param warn the {@link SQLWarning} to print
   */
  void showWarnings(SQLWarning warn) {
    if (warn == null) {
      return;
    }

    if (seenWarnings.get(warn) == null) {
      // don't re-display warnings we have already seen
      seenWarnings.put(warn, new java.util.Date());
      handleSQLException(warn);
    }

    SQLWarning next = warn.getNextWarning();
    if (next != warn) {
      showWarnings(next);
    }
  }

  String getPrompt() {
    if (getDatabaseConnection() == null || getDatabaseConnection().getUrl() == null) {
      return "sqlline> ";
    } else {
      return getPrompt(connections.getIndex() + ": " + getDatabaseConnection().getUrl()) + "> ";
    }
  }

  static String getPrompt(String url) {
    if (url == null || url.length() == 0) {
      url = "sqlline";
    }

    if (url.contains(";")) {
      url = url.substring(0, url.indexOf(";"));
    }
    if (url.contains("?")) {
      url = url.substring(0, url.indexOf("?"));
    }

    if (url.length() > 45) {
      url = url.substring(0, 45);
    }

    return url;
  }

  /**
   * Try to obtain the current size of the specified {@link ResultSet} by
   * jumping to the last row and getting the row number.
   *
   * @param rs the {@link ResultSet} to get the size for
   * @return the size, or -1 if it could not be obtained
   */
  int getSize(ResultSet rs) {
    try {
      if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY) {
        return -1;
      }

      rs.last();
      int total = rs.getRow();
      rs.beforeFirst();
      return total;
    } catch (SQLException sqle) {
      return -1;
    } catch (AbstractMethodError ame) {
      // JDBC 1 driver error
      return -1;
    }
  }

  ResultSet getColumns(String table)
      throws SQLException {
    if (!assertConnection()) {
      return null;
    }

    return getDatabaseConnection().meta.getColumns(
        getDatabaseConnection().meta.getConnection().getCatalog(),
        null,
        table,
        "%");
  }

  ResultSet getTables()
      throws SQLException {
    if (!assertConnection()) {
      return null;
    }

    return getDatabaseConnection().meta.getTables(
        getDatabaseConnection().meta.getConnection().getCatalog(),
        null,
        "%",
        new String[] {"TABLE"});
  }

  String[] getColumnNames(DatabaseMetaData meta)
      throws SQLException {
    Set<String> names = new HashSet<String>();

    info(loc("building-tables"));

    try {
      ResultSet columns = getColumns("%");

      try {
        int total = getSize(columns);
        int index = 0;

        while (columns.next()) {
          // add the following strings:
          // 1. column name
          // 2. table name
          // 3. tablename.columnname

          progress(index++, total);
          String name = columns.getString("TABLE_NAME");
          names.add(name);
          names.add(columns.getString("COLUMN_NAME"));
          names.add(
              columns.getString("TABLE_NAME") + "."
                  + columns.getString("COLUMN_NAME"));
        }

        progress(index, index);
      } finally {
        columns.close();
      }

      info(loc("done"));

      return names.toArray(new String[0]);
    } catch (Throwable t) {
      handleException(t);
      return new String[0];
    }
  }


  /**
   * Split the line into an array by tokenizing on space characters
   *
   * @param line the line to break up
   * @return an array of individual words
   */
  String[] split(String line) {
    return split(line, " ");
  }

  /**
   * Splits the line into an array of possibly-compound identifiers, observing
   * the database's quoting syntax.
   * <p/>
   * <p>For example, on Oracle, which uses double-quote (&quot;) as quote
   * character,
   * <p/>
   * <blockquote>!tables "My Schema"."My Table"</blockquote>
   * <p/>
   * returns
   * <p/>
   * <blockquote>{ {"!tables"}, {"My Schema", "My Table"} }</blockquote>
   *
   * @param line the line to break up
   * @return an array of compound words
   */
  public String[][] splitCompound(String line) {
    final DatabaseConnection databaseConnection = getDatabaseConnection();
    final Quoting quoting;
    if (databaseConnection == null) {
      quoting = Quoting.DEFAULT;
    } else {
      quoting = databaseConnection.quoting;
    }

    int state = SPACE;
    int idStart = -1;
    final char[] chars = line.toCharArray();
    int n = chars.length;

    // Trim off trailing semicolon and/or whitespace
    while (n > 0
        && (Character.isWhitespace(chars[n - 1])
        || chars[n - 1] == ';')) {
      --n;
    }

    final List<String[]> words = new ArrayList<String[]>();
    final List<String> current = new ArrayList<String>();
    for (int i = 0; i < n;) {
      char c = chars[i];
      switch (state) {
      case SPACE:
      case DOT_SPACE:
        ++i;
        if (Character.isWhitespace(c)) {
          ;
        } else if (c == '.') {
          state = DOT_SPACE;
        } else if (c == quoting.start) {
          if (state == SPACE) {
            if (current.size() > 0) {
              words.add(
                  current.toArray(new String[current.size()]));
              current.clear();
            }
          }
          state = QUOTED;
          idStart = i;
        } else {
          if (state == SPACE) {
            if (current.size() > 0) {
              words.add(
                  current.toArray(new String[current.size()]));
              current.clear();
            }
          }
          state = UNQUOTED;
          idStart = i - 1;
        }
        break;
      case QUOTED:
        ++i;
        if (c == quoting.end) {
          if (i < n
              && chars[i] == quoting.end) {
            // Repeated quote character inside a quoted identifier.
            // Eliminate one of the repeats, and we remain inside a
            // quoted identifier.
            System.arraycopy(chars, i, chars, i - 1, n - i);
            --n;
          } else {
            state = SPACE;
            final String word =
                new String(chars, idStart, i - idStart - 1);
            current.add(word);
          }
        }
        break;
      case UNQUOTED:
        // We are in an unquoted identifier. Whitespace or dot ends
        // the identifier, anything else extends it.
        ++i;
        if (Character.isWhitespace(c) || c == '.') {
          String word = new String(chars, idStart, i - idStart - 1);
          if (word.equalsIgnoreCase("NULL")) {
            word = null;
          } else if (quoting.upper) {
            word = word.toUpperCase();
          }
          current.add(word);
          state = c == '.' ? DOT_SPACE : SPACE;
        }
        break;
      default:
        throw new AssertionError("unexpected state " + state);
      }
    }

    switch (state) {
    case SPACE:
    case DOT_SPACE:
      break;
    case QUOTED:
    case UNQUOTED:
      // In the middle of a quoted string. Be lenient, and complete the
      // word.
      String word = new String(chars, idStart, n - idStart);
      if (state == UNQUOTED) {
        if (word.equalsIgnoreCase("NULL")) {
          word = null;
        } else if (quoting.upper) {
          word = word.toUpperCase();
        }
      }
      current.add(word);
      break;
    default:
      throw new AssertionError("unexpected state " + state);
    }

    if (current.size() > 0) {
      words.add(current.toArray(new String[current.size()]));
    }

    return words.toArray(new String[words.size()][]);
  }

  /**
   * In a region of whitespace.
   */
  private static final int SPACE = 0;

  /**
   * In a region of whitespace that contains a dot.
   */
  private static final int DOT_SPACE = 1;

  /**
   * Inside a quoted identifier.
   */
  private static final int QUOTED = 2;

  /**
   * Inside an unquoted identifier.
   */
  private static final int UNQUOTED = 3;

  String dequote(String str) {
    if (str == null) {
      return null;
    }

    while (str.startsWith("'") && str.endsWith("'")
        || str.startsWith("\"") && str.endsWith("\"")) {
      str = str.substring(1, str.length() - 1);
    }

    return str;
  }

  String[] split(String line, String delim) {
    StringTokenizer tok = new StringTokenizer(line, delim);
    String[] ret = new String[tok.countTokens()];
    int index = 0;
    while (tok.hasMoreTokens()) {
      String t = tok.nextToken();

      t = dequote(t);

      ret[index++] = t;
    }

    return ret;
  }

  static <K, V> Map<K, V> map(K key, V value, Object... obs) {
    Map<K, V> m = new HashMap<K, V>();
    m.put(key, value);
    for (int i = 0; i < obs.length - 1; i += 2) {
      //noinspection unchecked
      m.put((K) obs[i], (V) obs[i + 1]);
    }

    return Collections.unmodifiableMap(m);
  }

  static boolean getMoreResults(Statement stmnt) {
    try {
      return stmnt.getMoreResults();
    } catch (Throwable t) {
      return false;
    }
  }

  static String xmlattrencode(String str) {
    str = replace(str, "\"", "&quot;");
    str = replace(str, "<", "&lt;");
    return str;
  }

  static String replace(String source, String from, String to) {
    if (source == null) {
      return null;
    }

    if (from.equals(to)) {
      return source;
    }

    StringBuilder replaced = new StringBuilder();

    int index;
    while ((index = source.indexOf(from)) != -1) {
      replaced.append(source.substring(0, index));
      replaced.append(to);
      source = source.substring(index + from.length());
    }

    replaced.append(source);

    return replaced.toString();
  }

  /**
   * Split the line based on spaces, asserting that the number of words is
   * correct.
   *
   * @param line      the line to split
   * @param assertLen the number of words to assure
   * @param usage     the message to output if there are an incorrect number of
   *                  words.
   * @return the split lines, or null if the assertion failed.
   */
  String[] split(String line, int assertLen, String usage) {
    String[] ret = split(line);

    if (ret.length != assertLen) {
      error(usage);
      return null;
    }

    return ret;
  }

  /**
   * Wrap the specified string by breaking on space characters.
   *
   * @param toWrap the string to wrap
   * @param len    the maximum length of any line
   * @param start  the number of spaces to pad at the beginning of a line
   * @return the wrapped string
   */
  String wrap(String toWrap, int len, int start) {
    StringBuilder buff = new StringBuilder();
    StringBuilder line = new StringBuilder();

    char[] head = new char[start];
    Arrays.fill(head, ' ');

    for (
        StringTokenizer tok = new StringTokenizer(toWrap, " ");
        tok.hasMoreTokens();) {
      String next = tok.nextToken();
      if (line.length() + next.length() > len) {
        buff.append(line).append(separator).append(head);
        line.setLength(0);
      }

      line.append(line.length() == 0 ? "" : " ").append(next);
    }

    buff.append(line);

    return buff.toString();
  }

  /**
   * Output a progress indicator to the console.
   *
   * @param cur the current progress
   * @param max the maximum progress, or -1 if unknown
   */
  void progress(int cur, int max) {
    StringBuilder out = new StringBuilder();

    if (lastProgress != null) {
      char[] back = new char[lastProgress.length()];
      Arrays.fill(back, '\b');
      out.append(back);
    }

    String progress =
        cur + "/" + (max == -1 ? "?" : "" + max) + " "
            + (max == -1 ? "(??%)"
            : "(" + cur * 100 / (max == 0 ? 1 : max) + "%)");

    if (cur >= max && max != -1) {
      progress += " " + loc("done") + separator;
      lastProgress = null;
    } else {
      lastProgress = progress;
    }

    out.append(progress);

    System.out.print(out.toString());
    System.out.flush();
  }

  ///////////////////////////////
  // Exception handling routines
  ///////////////////////////////

  void handleException(Throwable e) {
    while (e instanceof InvocationTargetException) {
      e = ((InvocationTargetException) e).getTargetException();
    }

    if (e instanceof SQLException) {
      handleSQLException((SQLException) e);
    } else if (!initComplete && !opts.getVerbose()) {
      // all init errors must be verbose
      if (e.getMessage() == null) {
        error(e.getClass().getName());
      } else {
        error(e.getMessage());
      }
    } else {
      e.printStackTrace(System.err);
    }
  }

  void handleSQLException(SQLException e) {
    // all init errors must be verbose
    final boolean showWarnings = !initComplete || opts.getShowWarnings();
    final boolean verbose = !initComplete || opts.getVerbose();
    final boolean showNested = !initComplete || opts.getShowNestedErrs();

    if (e instanceof SQLWarning && !showWarnings) {
      return;
    }

    String type = e instanceof SQLWarning ? loc("Warning") : loc("Error");

    error(
        loc(e instanceof SQLWarning ? "Warning" : "Error",
            new Object[] {
                e.getMessage() == null ? "" : e.getMessage().trim(),
                e.getSQLState() == null ? "" : e.getSQLState().trim(),
                e.getErrorCode()}));

    if (verbose) {
      e.printStackTrace();
    }

    if (!showNested) {
      return;
    }

    for (SQLException nested = e.getNextException();
        nested != null && nested != e;
        nested = nested.getNextException()) {
      handleSQLException(nested);
    }
  }

  boolean scanForDriver(String url) {
    try {
      // already registered
      if (findRegisteredDriver(url) != null) {
        return true;
      }

      // first try known drivers...
      scanDrivers(true);

      if (findRegisteredDriver(url) != null) {
        return true;
      }

      // now really scan...
      scanDrivers(false);

      if (findRegisteredDriver(url) != null) {
        return true;
      }

      return false;
    } catch (Exception e) {
      debug(e.toString());
      return false;
    }
  }

  private Driver findRegisteredDriver(String url) {
    for (Enumeration<Driver> drivers = DriverManager.getDrivers();
        drivers.hasMoreElements();) {
      Driver driver = drivers.nextElement();
      try {
        if (driver.acceptsURL(url)) {
          return driver;
        }
      } catch (Exception e) {
      }
    }

    return null;
  }

  Driver[] scanDrivers(String line) throws IOException {
    return scanDrivers(false);
  }

  Driver[] scanDrivers(boolean knownOnly) throws IOException {
    long start = System.currentTimeMillis();

    Set<String> classNames = new HashSet<String>();

    if (!knownOnly) {
      classNames.addAll(
          Arrays.asList(
              ClassNameCompleter.getClassNames()));
    }

    classNames.addAll(KNOWN_DRIVERS);

    Set<Driver> driverClasses = new HashSet<Driver>();

    for (Iterator<String> i = classNames.iterator(); i.hasNext();) {
      String className = i.next();

      if (!className.toLowerCase().contains("driver")) {
        continue;
      }

      try {
        Class c =
            Class.forName(className, false,
                Thread.currentThread().getContextClassLoader());
        if (!Driver.class.isAssignableFrom(c)) {
          continue;
        }

        if (Modifier.isAbstract(c.getModifiers())) {
          continue;
        }

        // now instantiate and initialize it
        driverClasses.add((Driver) c.newInstance());
      } catch (Throwable t) {
      }
    }
    long end = System.currentTimeMillis();
    info("scan complete in " + (end - start) + "ms");
    return driverClasses.toArray(new Driver[driverClasses.size()]);
  }

  ///////////////////////////////////////
  // ResultSet output formatting classes
  ///////////////////////////////////////

  int print(ResultSet rs, DispatchCallback callback) throws SQLException {
    String format = opts.getOutputFormat();
    OutputFormat f = (OutputFormat) formats.get(format);

    if (f == null) {
      error(loc("unknown-format", new Object[] {
          format, formats.keySet()}));
      f = new TableOutputFormat(this);
    }

    Rows rows;
    if (opts.getIncremental()) {
      rows = new IncrementalRows(this, rs, callback);
    } else {
      rows = new BufferedRows(this, rs);
    }

    return f.print(rows);
  }

  Statement createStatement() throws SQLException {
    Statement stmnt = getDatabaseConnection().connection.createStatement();
    if (opts.timeout > -1) {
      stmnt.setQueryTimeout(opts.timeout);
    }
    if (opts.rowLimit != 0) {
      stmnt.setMaxRows(opts.rowLimit);
    }

    return stmnt;
  }

  void runBatch(List<String> statements) {
    try {
      Statement stmnt = createStatement();
      try {
        for (Iterator<String> i = statements.iterator(); i.hasNext();) {
          stmnt.addBatch(i.next());
        }

        int[] counts = stmnt.executeBatch();

        output(
            getColorBuffer()
                .pad(getColorBuffer().bold("COUNT"), 8)
                .append(getColorBuffer().bold("STATEMENT")));

        for (int i = 0; counts != null && i < counts.length; i++) {
          output(
              getColorBuffer().pad(counts[i] + "", 8)
                  .append(statements.get(i)));
        }
      } finally {
        try {
          stmnt.close();
        } catch (Exception e) {
        }
      }
    } catch (Exception e) {
      handleException(e);
    }
  }

  public int runCommands(List<String> cmds, DispatchCallback callback) {
    int successCount = 0;

    try {
      int index = 1;
      int size = cmds.size();
      for (Iterator<String> i = cmds.iterator(); i.hasNext();) {
        String cmd = i.next();
        info(getColorBuffer().pad(index++ + "/" + size, 13).append(cmd));
        dispatch(cmd, callback);
        boolean success = callback.isSuccess();
        // if we do not force script execution, abort
        // when a failure occurs.
        if (!success && !opts.getForce()) {
          error(loc("abort-on-error", cmd));
          return successCount;
        }
        successCount += success ? 1 : 0;
      }
    } catch (Exception e) {
      handleException(e);
    }

    return successCount;
  }

  void setCompletions()
      throws SQLException, IOException {
    if (getDatabaseConnection() != null) {
      getDatabaseConnection().setCompletions(opts.getFastConnect());
    }
  }

  public SqlLineOpts getOpts() {
    return opts;
  }

  DatabaseConnections getDatabaseConnections() {
    return connections;
  }

  public boolean isExit() {
    return exit;
  }

  public void setExit(boolean exit) {
    this.exit = exit;
  }

  Collection<Driver> getDrivers() {
    return drivers;
  }

  void setDrivers(Collection<Driver> drivers) {
    this.drivers = drivers;
  }

  public static String getSeparator() {
    return separator;
  }

  Commands getCommands() {
    return commands;
  }

  OutputFile getScriptOutputFile() {
    return scriptOutputFile;
  }

  void setScriptOutputFile(OutputFile script) {
    this.scriptOutputFile = script;
  }

  OutputFile getRecordOutputFile() {
    return recordOutputFile;
  }

  void setRecordOutputFile(OutputFile record) {
    this.recordOutputFile = record;
  }

  public void setOutputStream(PrintStream outputStream) {
    this.outputStream = new PrintStream(outputStream, true);
  }

  PrintStream getOutputStream() {
    return outputStream;
  }

  public void setErrorStream(PrintStream errorStream) {
    this.errorStream = new PrintStream(errorStream, true);
  }

  PrintStream getErrorStream() {
    return errorStream;
  }

  ConsoleReader getConsoleReader() {
    return consoleReader;
  }

  void setConsoleReader(ConsoleReader reader) {
    this.consoleReader = reader;
  }

  List<String> getBatch() {
    return batch;
  }

  void setBatch(List<String> batch) {
    this.batch = batch;
  }

  public Reflector getReflector() {
    return reflector;
  }

  public Completer getCommandCompleter() {
    return sqlLineCommandCompleter;
  }
}

// End SqlLine.java
