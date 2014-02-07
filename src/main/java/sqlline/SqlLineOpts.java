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
import java.util.*;

import jline.TerminalFactory;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

/**
 * Session options.
 */
class SqlLineOpts implements Completer {
  public static final String PROPERTY_PREFIX = "sqlline.";
  public static final String PROPERTY_NAME_EXIT =
      PROPERTY_PREFIX + "system.exit";
  private SqlLine sqlLine;
  private boolean autosave = false;
  private boolean silent = false;
  private boolean color = false;
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
  private String numberFormat = "default";
  private int maxWidth = TerminalFactory.get().getWidth();
  private int maxHeight = TerminalFactory.get().getHeight();
  private int maxColumnWidth = 15;
  int rowLimit = 0;
  int timeout = -1;
  private String isolation = "TRANSACTION_REPEATABLE_READ";
  private String outputFormat = "table";
  private boolean trimScripts = true;
  private File rcFile = new File(saveDir(), "sqlline.properties");
  private String historyFile =
      new File(saveDir(), "history").getAbsolutePath();
  private String scriptFile = null;

  private String runFile;

  public SqlLineOpts(SqlLine sqlLine, Properties props) {
    this.sqlLine = sqlLine;
    loadProperties(props);
  }

  public Completer[] optionCompleters() {
    return new Completer[] {this};
  }

  public String[] possibleSettingValues() {
    List<String> vals = new LinkedList<String>();
    vals.addAll(Arrays.asList("yes", "no"));
    return vals.toArray(new String[vals.size()]);
  }

  /**
   * The save directory if HOME/.sqlline/ on UNIX, and HOME/sqlline/ on
   * Windows.
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

  public int complete(String buf, int pos, List<CharSequence> candidates) {
    try {
      return new StringsCompleter(propertyNames())
          .complete(buf, pos, candidates);
    } catch (Throwable t) {
      return -1;
    }
  }

  public void save() throws IOException {
    OutputStream out = new FileOutputStream(rcFile);
    save(out);
    out.close();
  }

  public void save(OutputStream out) throws IOException {
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

  String[] propertyNames()
    throws IllegalAccessException, InvocationTargetException {
    TreeSet names = new TreeSet();

    // get all the values from getXXX methods
    Method[] m = getClass().getDeclaredMethods();
    for (int i = 0; (m != null) && (i < m.length); i++) {
      if (!(m[i].getName().startsWith("get"))) {
        continue;
      }

      if (m[i].getParameterTypes().length != 0) {
        continue;
      }

      String propName = m[i].getName().substring(3).toLowerCase();
      if (propName.equals("run")) {
        // Not a real property
        continue;
      }
      names.add(propName);
    }

    return (String[]) names.toArray(new String[names.size()]);
  }

  public Properties toProperties()
    throws IllegalAccessException,
      InvocationTargetException,
      ClassNotFoundException {
    Properties props = new Properties();

    String[] names = propertyNames();
    for (int i = 0; (names != null) && (i < names.length); i++) {
      props.setProperty(PROPERTY_PREFIX + names[i],
          sqlLine.getReflector().invoke(this, "get" + names[i], new Object[0])
              .toString());
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
    for (Iterator i = props.keySet().iterator(); i.hasNext();) {
      String key = i.next().toString();
      if (key.equals(PROPERTY_NAME_EXIT)) {
        // fix for sf.net bug 879422
        continue;
      }
      if (key.startsWith(PROPERTY_PREFIX)) {
        set(
            key.substring(PROPERTY_PREFIX.length()),
            props.getProperty(key));
      }
    }
  }

  public void set(String key, String value) {
    set(key, value, false);
  }

  public boolean set(String key, String value, boolean quiet) {
    try {
      sqlLine.getReflector().invoke(this, "set" + key, new Object[]{value});
      return true;
    } catch (Exception e) {
      if (!quiet) {
        // need to use System.err here because when bad command args
        // are passed this is called before init is done, meaning
        // that sqlline's error() output chokes because it depends
        // on properties like text coloring that can get set in
        // arbitrary order.
        System.err.println(
            sqlLine.loc("error-setting", new Object[]{key, e}));
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
    this.isolation = isolation;
  }

  public String getIsolation() {
    return this.isolation;
  }

  public void setHistoryFile(String historyFile) {
    this.historyFile = historyFile;
  }

  public String getHistoryFile() {
    return this.historyFile;
  }

  public void setScriptFile(String scriptFile) {
    setRun(scriptFile);
  }

  public String getScriptFile() {
    return getRun();
  }

  public void setColor(boolean color) {
    this.color = color;
  }

  public boolean getColor() {
    return this.color;
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

  public void setAutosave(boolean autosave) {
    this.autosave = autosave;
  }

  public boolean getAutosave() {
    return this.autosave;
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
}

// End SqlLineOpts.java
