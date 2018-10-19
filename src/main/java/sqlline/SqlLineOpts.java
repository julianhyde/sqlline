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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;

import static sqlline.SqlLinePropertiesEnum.*;

/**
 * Session options.
 */
public class SqlLineOpts implements Completer {
  public static final String PROPERTY_PREFIX = "sqlline.";
  public static final String PROPERTY_NAME_EXIT =
      PROPERTY_PREFIX + "system.exit";
  private static final Date TEST_DATE = new Date();
  private SqlLine sqlLine;
  private File rcFile = new File(saveDir(), "sqlline.properties");
  private String runFile;
  private Set<String> propertyNames;

  private final Map<SqlLineProperty, Object> propertiesMap = new HashMap<>();
  // Map to specify how to set specific properties
  // if a default way
  // @{code sqlline.SqlLineOpts.set(sqlline.SqlLineProperty, java.lang.Object)}
  // is not suitable
  private final Map<SqlLineProperty, SqlLinePropertyWrite> propertiesConfig =
      Collections.unmodifiableMap(
          new HashMap<SqlLineProperty, SqlLinePropertyWrite>() {{
            put(HISTORY_FILE, SqlLineOpts.this::setHistoryFile);
            put(CSV_QUOTE_CHARACTER, SqlLineOpts.this::setCsvQuoteCharacter);
            put(DATE_FORMAT, SqlLineOpts.this::setDateFormat);
            put(NUMBER_FORMAT, SqlLineOpts.this::setNumberFormat);
            put(TIME_FORMAT, SqlLineOpts.this::setTimeFormat);
            put(TIMESTAMP_FORMAT, SqlLineOpts.this::setTimestampFormat);
        }}
      );


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

  /**
   * The save directory if HOME/.sqlline/ on UNIX, and HOME/sqlline/ on
   * Windows.
   *
   * @return save directory
   */
  public static File saveDir() {
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
            ((System.getProperty("os.name")
                .toLowerCase(Locale.ROOT).contains("windows"))
                ? "" : ".") + "sqlline")
            .getAbsoluteFile();
    try {
      f.mkdirs();
    } catch (Exception e) {
      // ignore
    }

    return f;
  }

  @Override public void complete(LineReader lineReader, ParsedLine parsedLine,
      List<Candidate> list) {
    try {
      new StringsCompleter(propertyNames())
          .complete(lineReader, parsedLine, list);
    } catch (Throwable ignored) {
    }
  }

  public void save() throws IOException {
    OutputStream out = new FileOutputStream(rcFile);
    save(out);
    out.close();
  }

  public void save(OutputStream out) {
    try {
      Properties props = toProperties(true);
      props.store(out, sqlLine.getApplicationTitle());
    } catch (Exception e) {
      sqlLine.handleException(e);
    }
  }

  public Set<String> propertyNames() {
    if (propertyNames != null) {
      return propertyNames;
    }
    // properties names do not change at runtime
    // cache for further re-use
    Set<String> set = Arrays.stream(SqlLinePropertiesEnum.values())
        .map(t -> t.propertyName().toLowerCase(Locale.ROOT))
        .collect(Collectors.toCollection(TreeSet::new));
    propertyNames = Collections.unmodifiableSet(set);
    return propertyNames;
  }

  public Properties toProperties() {
    return toProperties(false);
  }

  public Properties toProperties(boolean toSave) {
    Properties props = new Properties();

    for (SqlLinePropertiesEnum property : SqlLinePropertiesEnum.values()) {
      if (!toSave || property.couldBeStored()) {
        props.setProperty(PROPERTY_PREFIX + property.propertyName(),
            String.valueOf(
                propertiesMap.getOrDefault(property, property.defaultValue())));
      }
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
    if ("run".equals(key)) {
      setRun(value);
      return true;
    }
    SqlLineProperty property = SqlLinePropertiesEnum.valueOf(key, true);
    if (property != null) {
      if (property.isReadOnly()) {
        if (!quiet) {
          sqlLine.error(sqlLine.loc("property-readonly", key));
        }
        return false;
      } else {
        SqlLinePropertyWrite propertyWriter = propertiesConfig.get(property);
        if (propertyWriter != null) {
          propertyWriter.write(value);
        } else {
          set(property, value);
        }
        return true;
      }
    } else {
      if (!quiet) {
        // need to use System.err here because when bad command args
        // are passed this is called before init is done, meaning
        // that sqlline's error() output chokes because it depends
        // on properties like text coloring that can get set in
        // arbitrary order.
        System.err.println(sqlLine.loc("unknown-prop", key));
      }
      return false;
    }
  }

  public boolean hasProperty(String name) {
    try {
      return propertyNames().contains(name);
    } catch (Exception e) {
      // this should not happen
      // since property names are retrieved
      // based on available getters in this class
      sqlLine.debug(e.getMessage());
      return false;
    }
  }

  public String get(SqlLineProperty key) {
    return String.valueOf(propertiesMap.getOrDefault(key, key.defaultValue()));
  }

  public char getChar(SqlLineProperty key) {
    if (key.type() == Type.CHAR) {
      return (char) propertiesMap.getOrDefault(key, key.defaultValue());
    } else {
      throw new IllegalArgumentException(
          sqlLine.loc("wrong-prop-type", key.propertyName(), key.type()));
    }
  }

  public int getInt(SqlLineProperty key) {
    if (key.type() == Type.INTEGER) {
      return (int) propertiesMap.getOrDefault(key, key.defaultValue());
    } else {
      throw new IllegalArgumentException(
          sqlLine.loc("wrong-prop-type", key.propertyName(), key.type()));
    }
  }

  public boolean getBoolean(SqlLineProperty key) {
    if (key.type() == Type.BOOLEAN) {
      return (boolean) propertiesMap.getOrDefault(key, key.defaultValue());
    } else {
      throw new IllegalArgumentException(
          sqlLine.loc("wrong-prop-type", key.propertyName(), key.type()));
    }
  }

  public String get(String key) {
    SqlLineProperty property = SqlLinePropertiesEnum.valueOf(key, true);
    return property == null
        ? null
        : String.valueOf(
            propertiesMap.getOrDefault(property, property.defaultValue()));
  }

  public void set(SqlLineProperty key, Object value) {
    Object valueToSet = value;
    String strValue;
    switch (key.type()) {
    case STRING:
      strValue = value instanceof String
          ? (String) value : String.valueOf(value);
      valueToSet = DEFAULT.equalsIgnoreCase(strValue)
          ? key.defaultValue() : value;
      break;
    case INTEGER:
      try {
        valueToSet = value instanceof Integer || value.getClass() == int.class
          ? value : Integer.parseInt(String.valueOf(value));
      } catch (Exception ignored) {
      }
      break;
    case BOOLEAN:
      if (value instanceof Boolean || value.getClass() == boolean.class) {
        valueToSet = value;
      } else {
        strValue = String.valueOf(value);
        valueToSet = "true".equalsIgnoreCase(strValue)
            || (true + "").equalsIgnoreCase(strValue)
            || "1".equalsIgnoreCase(strValue)
            || "on".equalsIgnoreCase(strValue)
            || "yes".equalsIgnoreCase(strValue);
      }
      break;
    }
    propertiesMap.put(key, valueToSet);
  }

  public void setNumberFormat(String numberFormat) {
    if (DEFAULT.equalsIgnoreCase(numberFormat)) {
      propertiesMap.put(NUMBER_FORMAT, NUMBER_FORMAT.defaultValue());
      return;
    }
    try {
      NumberFormat nf = new DecimalFormat(numberFormat,
          DecimalFormatSymbols.getInstance(Locale.ROOT));
      nf.format(Integer.MAX_VALUE);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    propertiesMap.put(NUMBER_FORMAT, numberFormat);
  }

  public void setDateFormat(String dateFormat) {
    set(DATE_FORMAT, getValidDateTimePatternOrThrow(dateFormat));
  }

  public void setTimeFormat(String timeFormat) {
    set(TIME_FORMAT, getValidDateTimePatternOrThrow(timeFormat));
  }

  public void setTimestampFormat(String timestampFormat) {
    set(TIMESTAMP_FORMAT,
        getValidDateTimePatternOrThrow(timestampFormat));
  }

  public void setIsolation(String isolation) {
    set(ISOLATION, isolation.toUpperCase(Locale.ROOT));
  }

  public void setHistoryFile(String historyFile) {
    final String currentValue = get(HISTORY_FILE);
    if (Objects.equals(currentValue, historyFile)
        || Objects.equals(currentValue, Commands.expand(historyFile))) {
      return;
    }
    if (!DEFAULT.equalsIgnoreCase(historyFile)) {
      propertiesMap.put(HISTORY_FILE, Commands.expand(historyFile));
    } else {
      set(HISTORY_FILE, DEFAULT);
    }
    if (sqlLine != null && sqlLine.getLineReader() != null) {
      final History history = sqlLine.getLineReader().getHistory();
      if (history != null) {
        try {
          history.save();
        } catch (IOException e) {
          sqlLine.handleException(e);
        }
      }
      sqlLine.getLineReader()
          .setVariable(LineReader.HISTORY_FILE, get(HISTORY_FILE));
      new DefaultHistory().attach(sqlLine.getLineReader());
    }
  }

  public void setCsvQuoteCharacter(String csvQuoteCharacter) {
    if (DEFAULT.equals(csvQuoteCharacter)) {
      propertiesMap.put(CSV_QUOTE_CHARACTER, CSV_DELIMITER.defaultValue());
      return;
    } else if (csvQuoteCharacter != null) {
      if (csvQuoteCharacter.length() == 1) {
        propertiesMap.put(CSV_QUOTE_CHARACTER, csvQuoteCharacter.charAt(0));
        return;
      } else if (csvQuoteCharacter.length() == 2
          && csvQuoteCharacter.charAt(0) == '\\') {
        propertiesMap.put(CSV_QUOTE_CHARACTER, csvQuoteCharacter.charAt(1));
        return;
      }
    }
    throw new IllegalArgumentException("CsvQuoteCharacter is '"
        + csvQuoteCharacter + "'; it must be a character of default");
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
    if (DEFAULT.equalsIgnoreCase(dateTimePattern)) {
      return dateTimePattern;
    }
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(dateTimePattern, Locale.ROOT);
      sdf.format(TEST_DATE);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    return dateTimePattern;
  }

}

// End SqlLineOpts.java
