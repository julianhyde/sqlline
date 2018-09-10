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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Session options.
 */
public class SqlLineOpts implements Completer {
  public static final String PROPERTY_PREFIX = "sqlline.";
  public static final String PROPERTY_NAME_EXIT =
      PROPERTY_PREFIX + "system.exit";
  public static final String DEFAULT = "default";
  private static final int DEFAULT_MAX_WIDTH = 80;
  private static final int DEFAULT_MAX_HEIGHT = 80;
  public static final Date TEST_DATE = new Date();
  public static final String DEFAULT_TRANSACTION_ISOLATION =
      "TRANSACTION_REPEATABLE_READ";
  private SqlLine sqlLine;
  private boolean autoSave = false;
  private boolean silent = false;
  private boolean color = false;
  private String csvDelimiter = ",";
  private char csvQuoteCharacter = '\'';
  private boolean showHeader = true;
  private int headerInterval = 100;
  private boolean fastConnect = true;
  private boolean autoCommit = true;
  private boolean verbose = false;
  private boolean force = false;
  private boolean incremental = true;
  private boolean showElapsedTime = true;
  private boolean showWarnings = true;
  private boolean showNestedErrs = false;
  private String numberFormat = DEFAULT;
  private String dateFormat = DEFAULT;
  private String timeFormat = DEFAULT;
  private String nullValue = DEFAULT;
  private String timestampFormat = DEFAULT;
  private int maxWidth = DEFAULT_MAX_WIDTH;
  private int maxHeight = DEFAULT_MAX_HEIGHT;
  private int maxColumnWidth = 15;
  int rowLimit = 0;
  int timeout = -1;
  private String isolation = DEFAULT_TRANSACTION_ISOLATION;
  private String outputFormat = "table";
  private boolean trimScripts = true;
  private File rcFile = new File(saveDir(), "sqlline.properties");
  private String historyFile =
      new File(saveDir(), "history").getAbsolutePath();
  private String runFile;

  public SqlLineOpts(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public SqlLineOpts(SqlLine sqlLine, Properties props) {
    this(sqlLine);
    loadProperties(props);
  }

  public List<Completer> optionCompleters() {
    return Collections.singletonList(this);
  }

  public List<String> possibleSettingValues() {
    return Arrays.asList("yes", "no");
  }

  /**
   * The save directory if HOME/.sqlline/ on UNIX, and HOME/sqlline/ on
   * Windows.
   *
   * @return save directory
   */
  public File saveDir() {
    String dir = System.getProperty("sqlline.rcfile");
    if (dir != null && dir.length() > 0) {
      return new File(dir);
    }

    String baseDir = System.getProperty(SqlLine.SQLLINE_BASE_DIR);
    if (baseDir != null && baseDir.length() > 0) {
      File saveDir = new File(baseDir).getAbsoluteFile();
      saveDir.mkdirs();
      return saveDir;
    }

    File f =
        new File(
            System.getProperty("user.home"),
            ((System.getProperty("os.name").toLowerCase()
                .indexOf("windows") != -1) ? "" : ".") + "sqlline")
            .getAbsoluteFile();
    try {
      f.mkdirs();
    } catch (Exception e) {
      // ignore
    }

    return f;
  }

  @Override
  public void complete(
      LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
    try {
      new StringsCompleter(propertyNames())
          .complete(lineReader, parsedLine, list);
    } catch (Throwable t) {
      return;
    }
  }

  public void save() throws IOException {
    OutputStream out = new FileOutputStream(rcFile);
    save(out);
    out.close();
  }

  public void save(OutputStream out) {
    try {
      Properties props = toProperties();

      // don't save maxwidth: it is automatically set based on
      // the terminal configuration
      props.remove(PROPERTY_PREFIX + "maxwidth");

      props.store(out, sqlLine.getApplicationTitle());
    } catch (Exception e) {
      sqlLine.handleException(e);
    }
  }

  Set<String> propertyNames() {
    final TreeSet<String> set = new TreeSet<>();
    for (String s : propertyNamesMixed()) {
      set.add(s.toLowerCase());
    }
    return set;
  }

  Set<String> propertyNamesMixed() {
    TreeSet<String> names = new TreeSet<>();

    // get all the values from getXXX methods
    for (Method method : getClass().getDeclaredMethods()) {
      if (!method.getName().startsWith("get")) {
        continue;
      }

      if (method.getParameterTypes().length != 0) {
        continue;
      }

      String propName = deCamel(method.getName().substring(3));
      if (propName.equals("run")) {
        // Not a real property
        continue;
      }
      if (propName.equals("autosave")) {
        // Deprecated; property is now "autoSave"
        continue;
      }
      names.add(propName);
    }

    return names;
  }

  /** Converts "CamelCase" to "camelCase". */
  private static String deCamel(String s) {
    return s.substring(0, 1).toLowerCase()
        + s.substring(1);
  }

  public Properties toProperties()
      throws IllegalAccessException,
      InvocationTargetException,
      ClassNotFoundException {
    Properties props = new Properties();

    for (String name : propertyNames()) {
      props.setProperty(PROPERTY_PREFIX + name,
          String.valueOf(sqlLine.getReflector().invoke(this, "get" + name)));
    }

    sqlLine.debug("properties: " + props.toString());
    return props;
  }

  public void load() throws IOException {
    if (rcFile.exists()) {
      InputStream in = new FileInputStream(rcFile);
      load(in);
      in.close();
    }
  }

  public void load(InputStream fin) throws IOException {
    Properties p = new Properties();
    p.load(fin);
    loadProperties(p);
  }

  public void loadProperties(Properties props) {
    for (String key : Commands.asMap(props).keySet()) {
      if (key.equals(PROPERTY_NAME_EXIT)) {
        // fix for sf.net bug 879422
        continue;
      }
      if (key.startsWith(PROPERTY_PREFIX)) {
        set(key.substring(PROPERTY_PREFIX.length()), props.getProperty(key));
      }
    }
  }

  public void set(String key, String value) {
    set(key, value, false);
  }

  public boolean set(String key, String value, boolean quiet) {
    try {
      sqlLine.getReflector().invoke(this, "set" + key, value);
      return true;
    } catch (Exception e) {
      if (!quiet) {
        // need to use System.err here because when bad command args
        // are passed this is called before init is done, meaning
        // that sqlline's error() output chokes because it depends
        // on properties like text coloring that can get set in
        // arbitrary order.
        System.err.println(
            sqlLine.loc(
                "error-setting",
                key,
                e.getCause() == null ? e : e.getCause()));
      }
      return false;
    }
  }

  public void setFastConnect(boolean fastConnect) {
    this.fastConnect = fastConnect;
  }

  public boolean getFastConnect() {
    return this.fastConnect;
  }

  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  public boolean getAutoCommit() {
    return this.autoCommit;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public boolean getVerbose() {
    return this.verbose;
  }

  public void setShowElapsedTime(boolean showElapsedTime) {
    this.showElapsedTime = showElapsedTime;
  }

  public boolean getShowElapsedTime() {
    return this.showElapsedTime;
  }

  public void setShowWarnings(boolean showWarnings) {
    this.showWarnings = showWarnings;
  }

  public boolean getShowWarnings() {
    return this.showWarnings;
  }

  public void setShowNestedErrs(boolean showNestedErrs) {
    this.showNestedErrs = showNestedErrs;
  }

  public boolean getShowNestedErrs() {
    return this.showNestedErrs;
  }

  public void setNumberFormat(String numberFormat) {
    this.numberFormat = numberFormat;
  }

  public String getNumberFormat() {
    return this.numberFormat;
  }

  public String getDateFormat() {
    return this.dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = getValidDateTimePatternOrThrow(dateFormat);
  }

  public String getTimeFormat() {
    return this.timeFormat;
  }

  public void setTimeFormat(String timeFormat) {
    this.timeFormat = getValidDateTimePatternOrThrow(timeFormat);
  }

  public String getNullValue() {
    return this.nullValue;
  }

  public void setNullValue(String nullValue) {
    this.nullValue = nullValue;
  }

  public String getTimestampFormat() {
    return this.timestampFormat;
  }

  public void setTimestampFormat(String timestampFormat) {
    this.timestampFormat = getValidDateTimePatternOrThrow(timestampFormat);
  }

  public void setMaxWidth(int maxWidth) {
    this.maxWidth = maxWidth;
  }

  public int getMaxWidth() {
    return this.maxWidth;
  }

  public void setMaxColumnWidth(int maxColumnWidth) {
    this.maxColumnWidth = maxColumnWidth;
  }

  public int getMaxColumnWidth() {
    return this.maxColumnWidth;
  }

  public void setRowLimit(int rowLimit) {
    this.rowLimit = rowLimit;
  }

  public int getRowLimit() {
    return this.rowLimit;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public int getTimeout() {
    return this.timeout;
  }

  public void setIsolation(String isolation) {
    if (DEFAULT.equalsIgnoreCase(isolation)) {
      this.isolation = DEFAULT_TRANSACTION_ISOLATION;
    } else {
      this.isolation = isolation.toUpperCase(Locale.ROOT);
    }
  }

  public String getIsolation() {
    return this.isolation;
  }

  public void setHistoryFile(String historyFile) {
    this.historyFile = historyFile;
    if (sqlLine != null && sqlLine.getLineReader() != null) {
      sqlLine.getLineReader()
          .setVariable(LineReader.HISTORY_FILE, this.historyFile);
    }
  }

  public String getHistoryFile() {
    return this.historyFile;
  }

  public void setColor(boolean color) {
    this.color = color;
  }

  public boolean getColor() {
    return this.color;
  }

  public void setCsvDelimiter(String csvDelimiter) {
    this.csvDelimiter = csvDelimiter;
  }

  public String getCsvDelimiter() {
    return this.csvDelimiter;
  }

  public void setCsvQuoteCharacter(String csvQuoteCharacter) {
    if (DEFAULT.equals(csvQuoteCharacter)) {
      this.csvQuoteCharacter = '\'';
      return;
    } else if (csvQuoteCharacter != null) {
      if (csvQuoteCharacter.length() == 1) {
        this.csvQuoteCharacter = csvQuoteCharacter.charAt(0);
        return;
      } else if (csvQuoteCharacter.length() == 2
          && csvQuoteCharacter.charAt(0) == '\\') {
        this.csvQuoteCharacter = csvQuoteCharacter.charAt(1);
        return;
      }
    }
    throw new IllegalArgumentException("CsvQuoteCharacter is '"
        + csvQuoteCharacter + "'; it must be a character of default");
  }

  public char getCsvQuoteCharacter() {
    return this.csvQuoteCharacter;
  }

  public void setShowHeader(boolean showHeader) {
    this.showHeader = showHeader;
  }

  public boolean getShowHeader() {
    return this.showHeader;
  }

  public void setHeaderInterval(int headerInterval) {
    this.headerInterval = headerInterval;
  }

  public int getHeaderInterval() {
    return this.headerInterval;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public boolean getForce() {
    return this.force;
  }

  public void setIncremental(boolean incremental) {
    this.incremental = incremental;
  }

  public boolean getIncremental() {
    return this.incremental;
  }

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public boolean getSilent() {
    return this.silent;
  }

  /**
   * @deprecated Use {@link #setAutoSave(boolean)}
   *
   * @param autoSave auto save flag
   */
  @Deprecated
  public void setAutosave(boolean autoSave) {
    setAutoSave(autoSave);
  }

  /**
   * @deprecated Use {@link #getAutoSave()}
   *
   * @return true if auto save is on, false otherwise
   */
  @Deprecated
  public boolean getAutosave() {
    return getAutoSave();
  }

  public void setAutoSave(boolean autoSave) {
    this.autoSave = autoSave;
  }

  public boolean getAutoSave() {
    return this.autoSave;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getOutputFormat() {
    return this.outputFormat;
  }

  public void setTrimScripts(boolean trimScripts) {
    this.trimScripts = trimScripts;
  }

  public boolean getTrimScripts() {
    return this.trimScripts;
  }

  public void setMaxHeight(int maxHeight) {
    this.maxHeight = maxHeight;
  }

  public int getMaxHeight() {
    return this.maxHeight;
  }

  public File getPropertiesFile() {
    return rcFile;
  }

  public void setRun(String runFile) {
    this.runFile = runFile;
  }

  public String getRun() {
    return this.runFile;
  }

  private String getValidDateTimePatternOrThrow(String dateTimePattern) {
    if (DEFAULT.equals(dateTimePattern)) {
      return dateTimePattern;
    }
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(dateTimePattern);
      sdf.format(TEST_DATE);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    return dateTimePattern;
  }

}

// End SqlLineOpts.java
