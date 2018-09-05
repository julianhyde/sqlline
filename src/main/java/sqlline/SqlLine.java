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
 *
 * <p>TODO:
 * <ul>
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
  private static final ResourceBundle RESOURCE_BUNDLE =
      ResourceBundle.getBundle(SqlLine.class.getName());

  private static final String SEPARATOR = System.getProperty("line.separator");
  private boolean exit = false;
  private final DatabaseConnections connections = new DatabaseConnections();
  public static final String COMMAND_PREFIX = "!";
  private Set<Driver> drivers = null;
  private final SqlLineOpts opts = new SqlLineOpts(this);
  private String lastProgress = null;
  private final Map<SQLWarning, Date> seenWarnings =
      new HashMap<SQLWarning, Date>();
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
  private final Completer sqlLineCommandCompleter;

  private final Map<String, OutputFormat> formats = map(
      "vertical", (OutputFormat) new VerticalOutputFormat(this),
      "table", new TableOutputFormat(this),
      "csv", new SeparatedValuesOutputFormat(this, ","),
      "tsv", new SeparatedValuesOutputFormat(this, "\t"),
      "xmlattr", new XmlAttributeOutputFormat(this),
      "xmlelements", new XmlElementOutputFormat(this),
      "json", new JsonOutputFormat(this));

  final List<CommandHandler> commandHandlers = new ArrayList<CommandHandler>();

  static final SortedSet<String> KNOWN_DRIVERS = new TreeSet<String>(
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

  static {
    String testClass = "jline.console.ConsoleReader";
    try {
      Class.forName(testClass);
    } catch (Throwable t) {
      String message =
          locStatic(RESOURCE_BUNDLE, System.err, "jline-missing", testClass);
      throw new ExceptionInInitializerError(message);
    }
  }

  static Manifest getManifest() throws IOException {
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
    if (inputStream != null) {
      // If not running from a .jar, pom.properties will not exist, and
      // inputStream is null.
      try {
        properties.load(inputStream);
      } catch (IOException e) {
        handleException(e);
      }
    }

    return loc(
        "app-introduction",
        properties.getProperty("artifactId"),
        properties.getProperty("version"));
  }

  static String getApplicationContactInformation() {
    return getManifestAttribute("Implementation-Vendor");
  }

  String loc(String res, int param) {
    try {
      return MessageFormat.format(
          new ChoiceFormat(RESOURCE_BUNDLE.getString(res)).format(param),
          param);
    } catch (Exception e) {
      return res + ": " + param;
    }
  }

  String loc(String res, Object... params) {
    return locStatic(RESOURCE_BUNDLE, getErrorStream(), res, params);
  }

  static String locStatic(ResourceBundle resourceBundle, PrintStream err,
      String res, Object... params) {
    try {
      return MessageFormat.format(resourceBundle.getString(res), params);
    } catch (Exception e) {
      e.printStackTrace(err);

      try {
        return res + ": " + Arrays.toString(params);
      } catch (Exception e2) {
        return res;
      }
    }
  }

  protected String locElapsedTime(long milliseconds) {
    return loc("time-ms", milliseconds / 1000d);
  }

  /**
   * Starts the program.
   *
   * @param args Arguments specified on the command-line
   * @throws IOException on error
   */
  public static void main(String[] args) throws IOException {
    start(args, null, true);
  }

  /**
   * Starts the program with redirected input.
   *
   * <p>For redirected output, use {@link #setOutputStream} and
   * {@link #setErrorStream}.
   *
   * <p>Exits with 0 on success, 1 on invalid arguments, and 2 on any
   * other error.
   *
   * @param args        same as main()
   * @param inputStream redirected input, or null to use standard input
   * @return Status code to be returned to the operating system
   * @throws IOException on error
   */
  public static Status mainWithInputRedirection(String[] args,
      InputStream inputStream) throws IOException {
    return start(args, inputStream, false);
  }

  public SqlLine() {
    final TableNameCompleter tableCompleter = new TableNameCompleter(this);
    final List<Completer> empty = Collections.emptyList();

    commandHandlers.addAll(Arrays.<CommandHandler>asList(
        new ReflectiveCommandHandler(this, empty, "quit", "done", "exit"),
        new ReflectiveCommandHandler(this,
            new StringsCompleter(getConnectionURLExamples()),
            "connect", "open"),
        new ReflectiveCommandHandler(this, empty, "nickname"),
        new ReflectiveCommandHandler(this, tableCompleter, "describe"),
        new ReflectiveCommandHandler(this, tableCompleter, "indexes"),
        new ReflectiveCommandHandler(this, tableCompleter, "primarykeys"),
        new ReflectiveCommandHandler(this, tableCompleter, "exportedkeys"),
        new ReflectiveCommandHandler(this, empty, "manual"),
        new ReflectiveCommandHandler(this, tableCompleter, "importedkeys"),
        new ReflectiveCommandHandler(this, empty, "procedures"),
        new ReflectiveCommandHandler(this, empty, "tables"),
        new ReflectiveCommandHandler(this, empty, "typeinfo"),
        new ReflectiveCommandHandler(this, empty, "commandhandler"),
        new ReflectiveCommandHandler(this, tableCompleter, "columns"),
        new ReflectiveCommandHandler(this, empty, "reconnect"),
        new ReflectiveCommandHandler(this, tableCompleter, "dropall"),
        new ReflectiveCommandHandler(this, empty, "history"),
        new ReflectiveCommandHandler(this,
            new StringsCompleter(getMetadataMethodNames()), "metadata"),
        new ReflectiveCommandHandler(this, empty, "nativesql"),
        new ReflectiveCommandHandler(this, empty, "dbinfo"),
        new ReflectiveCommandHandler(this, empty, "rehash"),
        new ReflectiveCommandHandler(this, empty, "verbose"),
        new ReflectiveCommandHandler(this, new FileNameCompleter(), "run"),
        new ReflectiveCommandHandler(this, empty, "batch"),
        new ReflectiveCommandHandler(this, empty, "list"),
        new ReflectiveCommandHandler(this, empty, "all"),
        new ReflectiveCommandHandler(this, empty, "go", "#"),
        new ReflectiveCommandHandler(this, new FileNameCompleter(), "script"),
        new ReflectiveCommandHandler(this, new FileNameCompleter(), "record"),
        new ReflectiveCommandHandler(this, empty, "brief"),
        new ReflectiveCommandHandler(this, empty, "close"),
        new ReflectiveCommandHandler(this, empty, "closeall"),
        new ReflectiveCommandHandler(this,
            new StringsCompleter(getIsolationLevels()), "isolation"),
        new ReflectiveCommandHandler(this,
            new StringsCompleter(formats.keySet()), "outputformat"),
        new ReflectiveCommandHandler(this, empty, "autocommit"),
        new ReflectiveCommandHandler(this, empty, "commit"),
        new ReflectiveCommandHandler(this, new FileNameCompleter(),
            "properties"),
        new ReflectiveCommandHandler(this, empty, "rollback"),
        new ReflectiveCommandHandler(this, empty, "help", "?"),
        new ReflectiveCommandHandler(this, opts.optionCompleters(), "set"),
        new ReflectiveCommandHandler(this, empty, "save"),
        new ReflectiveCommandHandler(this, empty, "scan"),
        new ReflectiveCommandHandler(this, empty, "sql"),
        new ReflectiveCommandHandler(this, empty, "call")));

    sqlLineCommandCompleter = new SqlLineCommandCompleter(this);
    reflector = new Reflector(this);
    opts.loadProperties(System.getProperties());

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
   * @return Whether successful
   * @throws IOException on error
   */
  public static Status start(String[] args,
      InputStream inputStream,
      boolean saveHistory) throws IOException {
    SqlLine sqlline = new SqlLine();
    Status status = sqlline.begin(args, inputStream, saveHistory);

    if (!Boolean.getBoolean(SqlLineOpts.PROPERTY_NAME_EXIT)) {
      System.exit(status.ordinal());
    }

    return status;
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

  public List<String> getIsolationLevels() {
    return Arrays.asList(
      "TRANSACTION_NONE",
      "TRANSACTION_READ_COMMITTED",
      "TRANSACTION_READ_UNCOMMITTED",
      "TRANSACTION_REPEATABLE_READ",
      "TRANSACTION_SERIALIZABLE");
  }

  public Set<String> getMetadataMethodNames() {
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

  public List<String> getConnectionURLExamples() {
    return Arrays.asList(
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
      "jdbc:dbaw://<hostname>:8889/<database>");
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
    for (String driverName : KNOWN_DRIVERS) {
      try {
        Class.forName(driverName);
      } catch (Throwable t) {
        // ignore
      }
    }
  }

  /** Parses arguments.
   *
   * @param args Command-line arguments
   * @param callback Status callback
   * @return Whether arguments parsed successfully
   */
  Status initArgs(String[] args, DispatchCallback callback) {
    List<String> commands = new LinkedList<String>();
    List<String> files = new LinkedList<String>();
    String driver = null;
    String user = null;
    String pass = null;
    String url = null;
    String nickname = null;
    String logFile = null;
    String cHandler = null;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--help") || args[i].equals("-h")) {
        return Status.ARGS;
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
            return Status.ARGS;
          }
        }

        continue;
      }

      if (args[i].charAt(0) == '-') {
        if (i == args.length - 1) {
          return Status.ARGS;
        }
        if (args[i].equals("-d")) {
          driver = args[++i];
        } else if (args[i].equals("-ch")) {
          cHandler = args[++i];
        } else if (args[i].equals("-n")) {
          user = args[++i];
        } else if (args[i].equals("-p")) {
          pass = args[++i];
        } else if (args[i].equals("-u")) {
          url = args[++i];
        } else if (args[i].equals("-e")) {
          commands.add(args[++i]);
        } else if (args[i].equals("-f")) {
          getOpts().setRun(args[++i]);
        } else if (args[i].equals("-log")) {
          logFile = args[++i];
        } else if (args[i].equals("-nn")) {
          nickname = args[++i];
        } else {
          return Status.ARGS;
        }
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

    if (nickname != null) {
      dispatch(COMMAND_PREFIX + "nickname " + nickname, new DispatchCallback());
    }

    if (logFile != null) {
      dispatch(COMMAND_PREFIX + "record " + logFile, new DispatchCallback());
    }

    if (cHandler != null) {
      StringBuilder sb = new StringBuilder();
      for (String chElem: cHandler.split(",")) {
        sb.append(chElem).append(" ");
      }
      dispatch(COMMAND_PREFIX + "commandhandler " + sb.toString(),
          new DispatchCallback());
    }

    // now load properties files
    for (String file : files) {
      dispatch(COMMAND_PREFIX + "properties " + file, new DispatchCallback());
    }

    if (commands.size() > 0) {
      // for single command execute, disable color
      opts.setColor(false);
      opts.setHeaderInterval(-1);

      for (String command : commands) {
        debug(loc("executing-command", command));
        dispatch(command, new DispatchCallback());
      }

      exit = true; // execute and exit
    }

    Status status = Status.OK;

    // if a script file was specified, run the file and quit
    if (opts.getRun() != null) {
      dispatch(COMMAND_PREFIX + "run \"" + opts.getRun() + "\"", callback);
      if (callback.isFailure()) {
        status = Status.OTHER;
      }
      dispatch(COMMAND_PREFIX + "quit", new DispatchCallback());
    }

    return status;
  }

  /**
   * Runs SqlLine, accepting input from the given input stream,
   * dispatching it to the appropriate
   * {@link CommandHandler} until the global variable <code>exit</code> is
   * true.
   *
   * <p>Before you invoke this method, you can redirect output by
   * calling {@link #setOutputStream(PrintStream)}
   * and/or {@link #setErrorStream(PrintStream)}.
   */
  public Status begin(String[] args, InputStream inputStream,
      boolean saveHistory) throws IOException {
    try {
      opts.load();
    } catch (Exception e) {
      handleException(e);
    }

    FileHistory fileHistory =
        new FileHistory(new File(opts.getHistoryFile()));

    ConsoleReader reader;
    boolean runningScript = getOpts().getRun() != null;
    if (runningScript) {
      try {
        FileInputStream scriptStream =
            new FileInputStream(getOpts().getRun());
        reader = getConsoleReader(scriptStream, fileHistory);
      } catch (Throwable t) {
        handleException(t);
        commands.quit(null, new DispatchCallback());
        return Status.OTHER;
      }
    } else {
      reader = getConsoleReader(inputStream, fileHistory);
    }

    final DispatchCallback callback = new DispatchCallback();
    Status status = initArgs(args, callback);
    switch (status) {
    case ARGS:
      usage();
      // fall through
    case OTHER:
      return status;
    default:
      break;
    }

    try {
      info(getApplicationTitle());
    } catch (Exception e) {
      handleException(e);
    }

    // basic setup done. From this point on, honor opts value for showing
    // exception
    initComplete = true;
    while (!exit) {
      try {
        // Execute one instruction; terminate on executing a script if
        // there is an error.
        signalHandler.setCallback(callback);
        dispatch(reader.readLine(getPrompt()), callback);
        if (saveHistory) {
          fileHistory.flush();
        }
        if (!callback.isSuccess() && runningScript) {
          commands.quit(null, callback);
          status = Status.OTHER;
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
    if (callback.isFailure()) {
      status = Status.OTHER;
    }
    return status;
  }

  public ConsoleReader getConsoleReader(InputStream inputStream,
      FileHistory fileHistory) throws IOException {
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
      consoleReader = new ConsoleReader(inputStream, System.out);
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
   * @param line The command-line to dispatch
   */
  void dispatch(String line, DispatchCallback callback) {
    if (line == null) {
      // exit
      exit = true;
      return;
    }

    if (line.trim().length() == 0) {
      callback.setStatus(DispatchCallback.Status.SUCCESS);
      return;
    }

    if (isComment(line)) {
      callback.setStatus(DispatchCallback.Status.SUCCESS);
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
      for (CommandHandler commandHandler: commandHandlers) {
        String match = commandHandler.matches(line);
        if (match != null) {
          cmdMap.put(match, commandHandler);
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

    if (line.startsWith(COMMAND_PREFIX)
        && !line.regionMatches(1, "sql", 0, "sql".length())
        && !line.regionMatches(1, "all", 0, "all".length())) {
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
  public void output(String msg) {
    output(msg, true);
  }

  public void info(String msg) {
    if (!opts.getSilent()) {
      output(msg, true, getErrorStream());
    }
  }

  public void info(ColorBuffer msg) {
    if (!opts.getSilent()) {
      output(msg, true, getErrorStream());
    }
  }

  /**
   * Issue the specified error message
   *
   * @param msg the message to issue
   * @return false always
   */
  public boolean error(String msg) {
    output(getColorBuffer().red(msg), true, errorStream);
    return false;
  }

  public boolean error(Throwable t) {
    handleException(t);
    return false;
  }

  public void debug(String msg) {
    if (opts.getVerbose()) {
      output(getColorBuffer().blue(msg), true, errorStream);
    }
  }

  public void output(ColorBuffer msg) {
    output(msg, true);
  }

  public void output(String msg, boolean newline, PrintStream out) {
    output(getColorBuffer(msg), newline, out);
  }

  public void output(ColorBuffer msg, boolean newline) {
    output(msg, newline, getOutputStream());
  }

  public void output(ColorBuffer msg, boolean newline, PrintStream out) {
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
  public void output(String msg, boolean newline) {
    output(getColorBuffer(msg), newline);
  }

  void autocommitStatus(Connection c) throws SQLException {
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
      if (getDatabaseConnection() == null
          || getDatabaseConnection().connection == null) {
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
    DatabaseConnection dbc = getDatabaseConnection();
    if (dbc == null || dbc.getUrl() == null) {
      return "sqlline> ";
    } else if (dbc.getNickname() != null) {
      return getPrompt(connections.getIndex() + ": "
          + dbc.getNickname()) + "> ";
    } else {
      return getPrompt(connections.getIndex() + ": " + dbc.getUrl()) + "> ";
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

  ResultSet getColumns(String table) throws SQLException {
    if (!assertConnection()) {
      return null;
    }

    return getDatabaseConnection().meta.getColumns(
        getDatabaseConnection().meta.getConnection().getCatalog(),
        null,
        table,
        "%");
  }

  ResultSet getTables() throws SQLException {
    if (!assertConnection()) {
      return null;
    }

    return getDatabaseConnection().meta.getTables(
        getDatabaseConnection().meta.getConnection().getCatalog(),
        null,
        "%",
        new String[] {"TABLE"});
  }

  Set<String> getColumnNames(DatabaseMetaData meta) throws SQLException {
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
          final String tableName = columns.getString("TABLE_NAME");
          final String columnName = columns.getString("COLUMN_NAME");
          names.add(tableName);
          names.add(columnName);
          names.add(tableName + "." + columnName);
        }

        progress(index, index);
      } finally {
        columns.close();
      }

      info(loc("done"));

      return names;
    } catch (Throwable t) {
      handleException(t);
      return Collections.emptySet();
    }
  }

  /**
   * Splits the line into an array, tokenizing on space characters.
   *
   * @param line the line to break up
   * @return an array of individual words
   */
  String[] split(String line) {
    return split(line, 0);
  }

  /**
   * Splits the line into an array, tokenizing on space characters,
   * limiting the number of words to read.
   *
   * @param line the line to break up
   * @param limit the limit for number of tokens
   *        to be processed (0 means no limit)
   * @return an array of individual words
   */
  String[] split(String line, int limit) {
    return split(line, " ", limit);
  }

  /**
   * Splits the line into an array of possibly-compound identifiers, observing
   * the database's quoting syntax.
   *
   * <p>For example, on Oracle, which uses double-quote (&quot;) as quote
   * character,</p>
   *
   * <blockquote>!tables "My Schema"."My Table"</blockquote>
   *
   * <p>returns</p>
   *
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
          // nothing
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

    if ((str.length() == 1 && (str.charAt(0) == '\'' || str.charAt(0) == '\"'))
      || ((str.charAt(0) == '"' || str.charAt(0) == '\''
            || str.charAt(str.length() - 1) == '"'
            || str.charAt(str.length() - 1) == '\'')
            && str.charAt(0) != str.charAt(str.length() - 1))) {
      throw new IllegalArgumentException(
          "A quote should be closed for <" + str + ">");
    }
    char prevQuote = 0;
    int index = 0;
    while ((str.charAt(index) == str.charAt(str.length() - index - 1))
        && (str.charAt(index) == '"' || str.charAt(index) == '\'')) {
      // if start and end point to the same element
      if (index == str.length() - index - 1) {
        if (prevQuote == str.charAt(index)) {
          throw new IllegalArgumentException(
              "A non-paired quote may not occur between the same quotes");
        } else {
          break;
        }
      // else if start and end point to neighbour elements
      } else if (index == str.length() - index - 2) {
        index++;
        break;
      }
      prevQuote = str.charAt(index);
      index++;
    }

    return index == 0 ? str : str.substring(index, str.length() - index);
  }

  String[] split(String line, String delim) {
    return split(line, delim, 0);
  }

  public String[] split(String line, String delim, int limit) {
    if (delim.indexOf('\'') != -1 || delim.indexOf('"') != -1) {
      // quotes in delim are not supported yet
      throw new UnsupportedOperationException();
    }
    boolean inQuotes = false;
    int tokenStart = 0;
    int lastProcessedIndex = 0;

    List<String> tokens = new ArrayList<String>();
    for (int i = 0; i < line.length(); i++) {
      if (limit > 0 && tokens.size() == limit) {
        break;
      }
      if (line.charAt(i) == '\'' || line.charAt(i) == '"') {
        if (inQuotes) {
          if (line.charAt(tokenStart) == line.charAt(i)) {
            inQuotes = false;
            tokens.add(line.substring(tokenStart, i + 1));
            lastProcessedIndex = i;
          }
        } else {
          tokenStart = i;
          inQuotes = true;
        }
      } else if (line.regionMatches(i, delim, 0, delim.length())) {
        if (inQuotes) {
          i += delim.length() - 1;
          continue;
        } else if (i > 0 && (
            !line.regionMatches(i - delim.length(), delim, 0, delim.length())
            && line.charAt(i - 1) != '\''
            && line.charAt(i - 1) != '"')) {
          tokens.add(line.substring(tokenStart, i));
          lastProcessedIndex = i;
          i += delim.length() - 1;

        }
      } else if (i > 0
          && line.regionMatches(i - delim.length(), delim, 0, delim.length())) {
        if (inQuotes) {
          continue;
        }
        tokenStart = i;
      }
    }
    if ((lastProcessedIndex != line.length() - 1
            && (limit == 0 || limit > tokens.size()))
        || (lastProcessedIndex == 0 && line.length() == 1)) {
      tokens.add(line.substring(tokenStart, line.length()));
    }
    String[] ret = new String[tokens.size()];
    for (int i = 0; i < tokens.size(); i++) {
      ret[i] = dequote(tokens.get(i));
    }

    return ret;
  }

  static <K, V> Map<K, V> map(K key, V value, Object... obs) {
    final Map<K, V> m = new HashMap<K, V>();
    m.put(key, value);
    for (int i = 0; i < obs.length - 1; i += 2) {
      //noinspection unchecked
      m.put((K) obs[i], (V) obs[i + 1]);
    }
    return m;
  }

  static boolean getMoreResults(Statement stmnt) {
    try {
      return stmnt.getMoreResults();
    } catch (Throwable t) {
      return false;
    }
  }

  static String xmlEncode(String str, String charsCouldBeNotEncoded) {
    if (str == null) {
      return str;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      switch (ch) {
      case '"' :
        // could be skipped for xml attribute in case of single quotes
        // could be skipped for element text
        if (charsCouldBeNotEncoded.indexOf(ch) == -1) {
          sb.append("&quot;");
        } else {
          sb.append(ch);
        }
        break;
      case '<' :
        sb.append("&lt;");
        break;
      case '&' :
        sb.append("&amp;");
        break;
      case '>' :
        // could be skipped for xml attribute and there is no sequence ]]>
        // could be skipped for element text and there is no sequence ]]>
        if ((i > 1 && str.charAt(i - 1) == ']' && str.charAt(i - 1) == ']')
            || charsCouldBeNotEncoded.indexOf(ch) == -1) {
          sb.append("&gt;");
        } else {
          sb.append(ch);
        }
        break;
      case '\'' :
        // could be skipped for xml attribute in case of double quotes
        // could be skipped for element text
        if (charsCouldBeNotEncoded.indexOf(ch) == -1) {
          sb.append("&apos;");
        } else {
          sb.append(ch);
        }
        break;
      default:
        sb.append(ch);
      }
    }
    return sb.toString();
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

    for (StringTokenizer tok = new StringTokenizer(toWrap, " ");
        tok.hasMoreTokens();) {
      String next = tok.nextToken();
      final int x = line.length();
      line.append(line.length() == 0 ? "" : " ").append(next);
      if (line.length() > len) {
        // The line is now too long. Backtrack: remove the last word, start a
        // new line containing just that word.
        line.setLength(x);
        buff.append(line).append(SEPARATOR).append(head);
        line.setLength(0);
        line.append(next);
      }
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
      progress += " " + loc("done") + SEPARATOR;
      lastProgress = null;
    } else {
      lastProgress = progress;
    }

    out.append(progress);

    getOutputStream().print(out.toString());
    getOutputStream().flush();
  }

  ///////////////////////////////
  // Exception handling routines
  ///////////////////////////////

  public void handleException(Throwable e) {
    while (e instanceof InvocationTargetException) {
      e = ((InvocationTargetException) e).getTargetException();
    }

    if (e instanceof SQLException) {
      handleSQLException((SQLException) e);
    } else if (e instanceof WrappedSqlException) {
      handleSQLException((SQLException) e.getCause());
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

  /** Looks for a driver with a particular URL. Returns the name of the class
   * if found, null if not found. */
  String scanForDriver(String url) {
    try {
      // already registered
      Driver driver;
      if ((driver = findRegisteredDriver(url)) != null) {
        return driver.getClass().getCanonicalName();
      }

      // first try known drivers...
      scanDrivers(true);

      if ((driver = findRegisteredDriver(url)) != null) {
        return driver.getClass().getCanonicalName();
      }

      // now really scan...
      scanDrivers(false);

      if ((driver = findRegisteredDriver(url)) != null) {
        return driver.getClass().getCanonicalName();
      }

      return null;
    } catch (Exception e) {
      debug(e.toString());
      return null;
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
        // ignore
      }
    }

    return null;
  }

  Set<Driver> scanDrivers(String line) throws IOException {
    return scanDrivers(false);
  }

  Set<Driver> scanDrivers(boolean knownOnly) throws IOException {
    long start = System.currentTimeMillis();

    Set<String> classNames = new HashSet<String>();

    if (!knownOnly) {
      classNames.addAll(ClassNameCompleter.getClassNames());
    }

    classNames.addAll(KNOWN_DRIVERS);

    Set<Driver> driverClasses = new HashSet<Driver>();

    for (String className : classNames) {
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
        // ignore
      }
    }
    long end = System.currentTimeMillis();
    info("scan complete in " + (end - start) + "ms");
    return driverClasses;
  }

  ///////////////////////////////////////
  // ResultSet output formatting classes
  ///////////////////////////////////////

  int print(ResultSet rs, DispatchCallback callback) throws SQLException {
    String format = opts.getOutputFormat();
    OutputFormat f = formats.get(format);
    if ("csv".equals(format)) {
      final SeparatedValuesOutputFormat csvOutput
        = (SeparatedValuesOutputFormat) f;
      if ((csvOutput.separator == null && opts.getCsvDelimiter() != null)
          || (csvOutput.separator != null
              && !csvOutput.separator.equals(opts.getCsvDelimiter())
              || csvOutput.quoteCharacter != opts.getCsvQuoteCharacter())) {
        f = new SeparatedValuesOutputFormat(this,
            opts.getCsvDelimiter(), opts.getCsvQuoteCharacter());
        formats.put("csv", f);
      }
    }

    if (f == null) {
      error(loc("unknown-format", format, formats.keySet()));
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
        for (String statement : statements) {
          stmnt.addBatch(statement);
        }

        int[] counts = stmnt.executeBatch();
        if (counts == null) {
          counts = new int[0];
        }

        output(
            getColorBuffer()
                .pad(getColorBuffer().bold("COUNT"), 8)
                .append(getColorBuffer().bold("STATEMENT")));

        for (int i = 0; i < counts.length; i++) {
          output(
              getColorBuffer().pad(counts[i] + "", 8)
                  .append(statements.get(i)));
        }
      } finally {
        try {
          stmnt.close();
        } catch (Exception e) {
          // ignore
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
      for (String cmd : cmds) {
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

  void setCompletions() throws SQLException, IOException {
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

  Set<Driver> getDrivers() {
    return drivers;
  }

  void setDrivers(Set<Driver> drivers) {
    this.drivers = drivers;
  }

  public static String getSeparator() {
    return SEPARATOR;
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

  /** Exit status returned to the operating system. OK, ARGS, OTHER
   * correspond to 0, 1, 2. */
  public enum Status {
    OK, ARGS, OTHER
  }
}

// End SqlLine.java
