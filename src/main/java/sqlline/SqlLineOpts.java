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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.jline.builtins.Completers;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;

import sqlline.SqlLineProperty.Type;

import static sqlline.BuiltInProperty.AUTO_COMMIT;
import static sqlline.BuiltInProperty.HISTORY_FLAGS;
import static sqlline.BuiltInProperty.READ_ONLY;
import static sqlline.BuiltInProperty.AUTO_SAVE;
import static sqlline.BuiltInProperty.COLOR;
import static sqlline.BuiltInProperty.COLOR_SCHEME;
import static sqlline.BuiltInProperty.CONFIRM;
import static sqlline.BuiltInProperty.CONFIRM_PATTERN;
import static sqlline.BuiltInProperty.CSV_DELIMITER;
import static sqlline.BuiltInProperty.CSV_QUOTE_CHARACTER;
import static sqlline.BuiltInProperty.DATE_FORMAT;
import static sqlline.BuiltInProperty.DEFAULT;
import static sqlline.BuiltInProperty.ESCAPE_OUTPUT;
import static sqlline.BuiltInProperty.FAST_CONNECT;
import static sqlline.BuiltInProperty.FORCE;
import static sqlline.BuiltInProperty.HEADER_INTERVAL;
import static sqlline.BuiltInProperty.HISTORY_FILE;
import static sqlline.BuiltInProperty.INCREMENTAL;
import static sqlline.BuiltInProperty.INCREMENTAL_BUFFER_ROWS;
import static sqlline.BuiltInProperty.ISOLATION;
import static sqlline.BuiltInProperty.MAX_COLUMN_WIDTH;
import static sqlline.BuiltInProperty.MAX_HEIGHT;
import static sqlline.BuiltInProperty.MAX_HISTORY_FILE_ROWS;
import static sqlline.BuiltInProperty.MAX_HISTORY_ROWS;
import static sqlline.BuiltInProperty.MAX_WIDTH;
import static sqlline.BuiltInProperty.MODE;
import static sqlline.BuiltInProperty.NULL_VALUE;
import static sqlline.BuiltInProperty.NUMBER_FORMAT;
import static sqlline.BuiltInProperty.OUTPUT_FORMAT;
import static sqlline.BuiltInProperty.PROMPT;
import static sqlline.BuiltInProperty.PROPERTIES_FILE;
import static sqlline.BuiltInProperty.RIGHT_PROMPT;
import static sqlline.BuiltInProperty.ROW_LIMIT;
import static sqlline.BuiltInProperty.SHOW_COMPLETION_DESCR;
import static sqlline.BuiltInProperty.SHOW_ELAPSED_TIME;
import static sqlline.BuiltInProperty.SHOW_HEADER;
import static sqlline.BuiltInProperty.SHOW_LINE_NUMBERS;
import static sqlline.BuiltInProperty.SHOW_NESTED_ERRS;
import static sqlline.BuiltInProperty.SHOW_WARNINGS;
import static sqlline.BuiltInProperty.SILENT;
import static sqlline.BuiltInProperty.STRICT_JDBC;
import static sqlline.BuiltInProperty.TIMEOUT;
import static sqlline.BuiltInProperty.TIMESTAMP_FORMAT;
import static sqlline.BuiltInProperty.TIME_FORMAT;
import static sqlline.BuiltInProperty.TRIM_SCRIPTS;
import static sqlline.BuiltInProperty.USE_LINE_CONTINUATION;
import static sqlline.BuiltInProperty.VERBOSE;

/**
 * Session options.
 */
public class SqlLineOpts implements Completer {
  public static final String PROPERTY_PREFIX = "sqlline.";
  public static final String PROPERTY_NAME_EXIT =
      PROPERTY_PREFIX + "system.exit";
  private static final Date TEST_DATE = new Date();
  private static final String DEV_NULL = "/dev/null";
  private SqlLine sqlLine;
  private String runFile;
  private Pattern compiledConfirmPattern = null;
  private Set<String> propertyNames;

  private final Map<SqlLineProperty, Object> propertiesMap = new HashMap<>();
  /** Map to setters that are aware of how to set specific properties
   * if a default way
   * {@code sqlline.SqlLineOpts.set(sqlline.SqlLineProperty, java.lang.Object)}
   * is not suitable. */
  private final Map<SqlLineProperty, SqlLineProperty.Writer> propertiesConfig =
      Collections.unmodifiableMap(
          new HashMap<SqlLineProperty, SqlLineProperty.Writer>() {
            {
              put(COLOR_SCHEME, SqlLineOpts.this::setColorScheme);
              put(CONFIRM_PATTERN, SqlLineOpts.this::setConfirmPattern);
              put(CSV_QUOTE_CHARACTER, SqlLineOpts.this::setCsvQuoteCharacter);
              put(DATE_FORMAT, SqlLineOpts.this::setDateFormat);
              put(HISTORY_FILE, SqlLineOpts.this::setHistoryFile);
              put(MAX_HISTORY_FILE_ROWS,
                  SqlLineOpts.this::setMaxHistoryFileRows);
              put(MAX_HISTORY_ROWS, SqlLineOpts.this::setMaxHistoryRows);
              put(MODE, SqlLineOpts.this::setMode);
              put(NUMBER_FORMAT, SqlLineOpts.this::setNumberFormat);
              put(OUTPUT_FORMAT, SqlLineOpts.this::setOutputFormat);
              put(PROPERTIES_FILE, SqlLineOpts.this::setPropertiesFile);
              put(SHOW_COMPLETION_DESCR,
                  SqlLineOpts.this::setShowCompletionDesc);
              put(TIME_FORMAT, SqlLineOpts.this::setTimeFormat);
              put(TIMESTAMP_FORMAT, SqlLineOpts.this::setTimestampFormat);
            }
          });

  public SqlLineOpts(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public SqlLineOpts(SqlLine sqlLine, Properties props) {
    this(sqlLine);
    loadProperties(props);
  }

  public List<Completer> resetOptionCompleters() {
    return Collections.singletonList(this);
  }

  /**
   * Builds and returns {@link org.jline.builtins.Completers.RegexCompleter} for
   * <code>!set</code> command based on
   * (in decreasing order of priority)
   * <ul>
   * <li>Customizations via {@code customCompletions}</li>
   * <li>Available values defined in {@link BuiltInProperty}</li>
   * <li>{@link Type} of property.
   * Currently there is completion only for boolean type</li>
   * </ul>
   *
   * @param customCompletions defines custom completions values per property
   * @return a singleton list with a built RegexCompleter */
  public List<Completer> setOptionCompleters(
      Map<BuiltInProperty, Collection<String>> customCompletions) {
    Map<String, Completer> comp = new HashMap<>();
    final String start = "START";
    comp.put(start,
        new StringsCompleter(
            new SqlLineCommandCompleter.SqlLineCandidate(sqlLine, "!set",
                "!set", sqlLine.loc("command-name"), sqlLine.loc("help-set"),
                null, "!set", true)));
    Collection<BuiltInProperty> booleanProperties = new ArrayList<>();
    Collection<BuiltInProperty> withDefinedAvailableValues = new ArrayList<>();
    StringBuilder sb = new StringBuilder(start + " (");
    for (BuiltInProperty property : BuiltInProperty.values()) {
      if (customCompletions.containsKey(property)) {
        continue;
      } else if (!property.getAvailableValues().isEmpty()) {
        withDefinedAvailableValues.add(property);
      } else if (property.type() == Type.BOOLEAN) {
        booleanProperties.add(property);
      } else {
        sb.append(property.name()).append(" | ");
        comp.put(property.name(),
            new StringsCompleter(property.propertyName()));
      }
    }
    // all boolean properties without defined available values and
    // not customized via {@code customCompletions} have values
    // for autocompletion specified in SqlLineProperty.BOOLEAN_VALUES
    final String booleanTypeString = Type.BOOLEAN.toString();
    sb.append(booleanTypeString);
    comp.put(booleanTypeString,
        new StringsCompleter(booleanProperties
            .stream()
            .map(BuiltInProperty::propertyName)
            .toArray(String[]::new)));
    final String booleanPropertyValueKey = booleanTypeString + "_value";
    comp.put(booleanPropertyValueKey,
        new StringsCompleter(BuiltInProperty.BOOLEAN_VALUES));
    sb.append(" ").append(booleanPropertyValueKey);
    // If a property has defined values they will be used for autocompletion
    for (BuiltInProperty property : withDefinedAvailableValues) {
      final String propertyName = property.propertyName();
      sb.append(" | ").append(propertyName);
      comp.put(propertyName, new StringsCompleter(propertyName));
      final String propertyValueKey = propertyName + "_value";
      comp.put(propertyValueKey,
          new StringsCompleter(
              property.getAvailableValues().toArray(new String[0])));
      sb.append(" ").append(propertyValueKey);
    }
    for (Map.Entry<BuiltInProperty, Collection<String>> mapEntry
        : customCompletions.entrySet()) {
      final String propertyName = mapEntry.getKey().propertyName();
      comp.put(propertyName, new StringsCompleter(propertyName));
      final String propertyValueKey = propertyName + "_value";
      comp.put(propertyValueKey,
          new StringsCompleter(mapEntry.getValue().toArray(new String[0])));
      sb.append("| ").append(propertyName).append(" ")
          .append(propertyValueKey);
    }
    sb.append(") ");
    return Collections.singletonList(
        new Completers.RegexCompleter(sb.toString(), comp::get));
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
    final String pathToPropertyFile = get(PROPERTIES_FILE);
    if (DEV_NULL.equals(pathToPropertyFile)) {
      sqlLine.error(sqlLine.loc("saving-to-dev-null-not-supported"));
      return;
    }
    OutputStream out = new FileOutputStream(pathToPropertyFile);
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
    Set<String> set = Arrays.stream(BuiltInProperty.values())
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

    for (BuiltInProperty property : BuiltInProperty.values()) {
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
    final File rcFile = new File(getPropertiesFile());
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
    final SqlLineProperty property = BuiltInProperty.valueOf(key, true);
    if (property == null) {
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
    if (property.isReadOnly()) {
      if (!quiet) {
        sqlLine.error(sqlLine.loc("property-readonly", key));
      }
      return false;
    } else {
      SqlLineProperty.Writer propertyWriter = propertiesConfig.get(property);
      if (propertyWriter != null) {
        propertyWriter.write(value);
      } else {
        set(property, value);
      }
      return true;
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

  /**
   * Returns whether the property is its default value.
   *
   * <p>This true if it has not been assigned a value,
   * or if has been assigned a value equal to its default value.
   *
   * @param property Property
   * @return whether property has its default value
   */
  public boolean isDefault(SqlLineProperty property) {
    final String defaultValue = String.valueOf(property.defaultValue());
    final Object currentValue =
        propertiesMap.getOrDefault(property, property.defaultValue());
    return currentValue == null
        || Objects.equals(String.valueOf(currentValue), defaultValue);
  }

  public String get(String key) {
    SqlLineProperty property = BuiltInProperty.valueOf(key, true);
    if (property == null) {
      return null; // unknown property
    }
    final Object o =
        propertiesMap.getOrDefault(property, property.defaultValue());
    return String.valueOf(o);
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
      if (!key.getAvailableValues().isEmpty()
          && !key.getAvailableValues().contains(valueToSet.toString())) {
        sqlLine.error(
            sqlLine.loc("unknown-value",
                key.propertyName(), value, key.getAvailableValues()));
        return;
      }
      break;
    case INTEGER:
      try {
        valueToSet = value instanceof Integer || value.getClass() == int.class
          ? value : Integer.parseInt(String.valueOf(value));
      } catch (Exception e) {
        sqlLine.error(
            sqlLine.loc("not-a-number",
                key.propertyName().toLowerCase(Locale.ROOT),
                value));
        if (getVerbose()) {
          sqlLine.handleException(e);
        }
        return;
      }
      break;
    case BOOLEAN:
      if (value instanceof Boolean || value.getClass() == boolean.class) {
        valueToSet = value;
      } else {
        strValue = String.valueOf(value);
        valueToSet = "true".equalsIgnoreCase(strValue)
            || "1".equalsIgnoreCase(strValue)
            || "on".equalsIgnoreCase(strValue)
            || "yes".equalsIgnoreCase(strValue);
      }
      break;
    }
    propertiesMap.put(key, valueToSet);
  }

  public boolean getFastConnect() {
    return getBoolean(FAST_CONNECT);
  }

  public boolean getAutoCommit() {
    return getBoolean(AUTO_COMMIT);
  }

  public boolean getReadOnly() {
    return getBoolean(READ_ONLY);
  }

  public boolean getVerbose() {
    return getBoolean(VERBOSE);
  }

  public boolean getShowElapsedTime() {
    return getBoolean(SHOW_ELAPSED_TIME);
  }

  public boolean getShowWarnings() {
    return getBoolean(SHOW_WARNINGS);
  }

  public boolean getShowCompletionDescr() {
    return getBoolean(SHOW_COMPLETION_DESCR);
  }

  public boolean getShowNestedErrs() {
    return getBoolean(SHOW_NESTED_ERRS);
  }

  public String getNumberFormat() {
    return get(NUMBER_FORMAT);
  }

  public boolean getEscapeOutput() {
    return getBoolean(ESCAPE_OUTPUT);
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
      sqlLine.handleException(e);
    }
    propertiesMap.put(NUMBER_FORMAT, numberFormat);
  }

  public String getDateFormat() {
    return get(DATE_FORMAT);
  }

  public void setDateFormat(String dateFormat) {
    set(DATE_FORMAT, getValidDateTimePatternOrThrow(dateFormat));
  }

  public String getTimeFormat() {
    return get(TIME_FORMAT);
  }

  public void setTimeFormat(String timeFormat) {
    set(TIME_FORMAT, getValidDateTimePatternOrThrow(timeFormat));
  }

  public String getTimestampFormat() {
    return get(TIMESTAMP_FORMAT);
  }

  public void setTimestampFormat(String timestampFormat) {
    set(TIMESTAMP_FORMAT,
        getValidDateTimePatternOrThrow(timestampFormat));
  }

  public void setShowCompletionDesc(String setShowCompletionDesc) {
    set(SHOW_COMPLETION_DESCR, setShowCompletionDesc);
  }

  public String getNullValue() {
    return get(NULL_VALUE);
  }

  public int getRowLimit() {
    return getInt(ROW_LIMIT);
  }

  public int getTimeout() {
    return getInt(TIMEOUT);
  }

  public String getIsolation() {
    return get(ISOLATION);
  }

  public void setIsolation(String isolation) {
    set(ISOLATION, isolation.toUpperCase(Locale.ROOT));
  }

  public String getHistoryFile() {
    return get(HISTORY_FILE);
  }

  public void setHistoryFile(String historyFile) {
    final String currentValue = get(HISTORY_FILE);
    if (Objects.equals(currentValue, historyFile)
        || Objects.equals(currentValue, Commands.expand(historyFile))) {
      return;
    }
    if (DEFAULT.equalsIgnoreCase(historyFile)) {
      set(HISTORY_FILE, DEFAULT);
    } else {
      propertiesMap.put(HISTORY_FILE, Commands.expand(historyFile));
    }
    if (sqlLine != null && sqlLine.getLineReader() != null) {
      History history = sqlLine.getLineReader().getHistory();
      if (history == null) {
        history = new DefaultHistory();
      } else {
        try {
          history.save();
        } catch (IOException e) {
          sqlLine.handleException(e);
        }
      }
      sqlLine.getLineReader()
          .setVariable(LineReader.HISTORY_FILE, get(HISTORY_FILE));
      history.attach(sqlLine.getLineReader());
    }
  }

  public void setColorScheme(String colorScheme) {
    if (DEFAULT.equals(colorScheme)
        || BuiltInHighlightStyle.BY_NAME.containsKey(colorScheme)) {
      propertiesMap.put(COLOR_SCHEME, colorScheme);
      return;
    }
    sqlLine.error(
        sqlLine.loc("unknown-value", COLOR_SCHEME.propertyName(), colorScheme,
            COLOR_SCHEME.getAvailableValues()));
  }

  public String getColorScheme() {
    return get(COLOR_SCHEME);
  }

  public boolean getColor() {
    return getBoolean(COLOR);
  }

  public String getCsvDelimiter() {
    return get(CSV_DELIMITER);
  }

  public char getCsvQuoteCharacter() {
    return getChar(CSV_QUOTE_CHARACTER);
  }

  public void setMaxHistoryRows(String maxHistoryRows) {
    setLineReaderHistoryIntVariable(
        LineReader.HISTORY_SIZE,
        maxHistoryRows,
        BuiltInProperty.MAX_HISTORY_ROWS);
  }

  public void setMaxHistoryFileRows(String maxHistoryFileRows) {
    setLineReaderHistoryIntVariable(
        LineReader.HISTORY_FILE_SIZE,
        maxHistoryFileRows,
        MAX_HISTORY_FILE_ROWS);
  }

  private void setLineReaderHistoryIntVariable(
      String variableName, String value, SqlLineProperty property) {
    LineReader lineReader = sqlLine.getLineReader();
    if (lineReader == null) {
      return;
    }
    int currentValue = getInt(property);
    try {
      if (DEFAULT.equals(value)) {
        if (currentValue == (Integer) property.defaultValue()) {
          return;
        } else {
          lineReader.setVariable(variableName, property.defaultValue());
          lineReader.getHistory().save();
          propertiesMap.put(property, property.defaultValue());
          return;
        }
      }

      int parsedValue = Integer.parseInt(value);
      if (parsedValue == currentValue) {
        return;
      } else {
        lineReader.setVariable(variableName, parsedValue);
        lineReader.getHistory().save();
        propertiesMap.put(property, parsedValue);
      }
    } catch (Exception e) {
      sqlLine.handleException(e);
    }
  }

  public void setCsvQuoteCharacter(String csvQuoteCharacter) {
    if (DEFAULT.equals(csvQuoteCharacter)) {
      propertiesMap.put(
          CSV_QUOTE_CHARACTER, CSV_QUOTE_CHARACTER.defaultValue());
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
    sqlLine.error("CsvQuoteCharacter is '"
        + csvQuoteCharacter + "'; it must be a character or default");
  }

  public boolean getShowHeader() {
    return getBoolean(SHOW_HEADER);
  }

  public int getHeaderInterval() {
    return getInt(HEADER_INTERVAL);
  }

  public boolean getForce() {
    return getBoolean(FORCE);
  }

  public boolean getIncremental() {
    return getBoolean(INCREMENTAL);
  }

  public int getIncrementalBufferRows() {
    return getInt(INCREMENTAL_BUFFER_ROWS);
  }

  public boolean getSilent() {
    return getBoolean(SILENT);
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

  public boolean getAutoSave() {
    return getBoolean(AUTO_SAVE);
  }

  public boolean getShowLineNumbers() {
    return getBoolean(SHOW_LINE_NUMBERS);
  }

  public String getOutputFormat() {
    return get(OUTPUT_FORMAT);
  }

  public String getPrompt() {
    return get(PROMPT);
  }

  public String getRightPrompt() {
    return get(RIGHT_PROMPT);
  }

  public boolean getTrimScripts() {
    return getBoolean(TRIM_SCRIPTS);
  }

  public int getMaxHeight() {
    return getInt(MAX_HEIGHT);
  }

  public int getMaxWidth() {
    return getInt(MAX_WIDTH);
  }

  public int getMaxColumnWidth() {
    return getInt(MAX_COLUMN_WIDTH);
  }

  public boolean getUseLineContinuation() {
    return getBoolean(USE_LINE_CONTINUATION);
  }

  public String getMode() {
    return get(MODE);
  }

  public void setMode(String mode) {
    final LineReader reader = sqlLine.getLineReader();
    if (reader == null || reader.getKeyMaps() == null) {
      return;
    }
    final Map<String, KeyMap<Binding>> keyMaps = reader.getKeyMaps();
    switch (mode) {
    case LineReader.EMACS:
    case SqlLineProperty.DEFAULT:
      set(BuiltInProperty.MODE, LineReader.EMACS);
      keyMaps.put(LineReader.MAIN, keyMaps.get(LineReader.EMACS));
      break;
    case "vi":
      set(BuiltInProperty.MODE, mode);
      keyMaps.put(LineReader.MAIN, keyMaps.get(LineReader.VIINS));
      break;
    default:
      sqlLine.error(
          sqlLine.loc("unknown-value", MODE.propertyName(),
              mode, Arrays.asList(LineReader.EMACS, "vi")));
    }
  }

  public void setOutputFormat(String outputFormat) {
    if (DEFAULT.equalsIgnoreCase(outputFormat)) {
      set(OUTPUT_FORMAT, OUTPUT_FORMAT.defaultValue());
      return;
    }

    Set<String> availableFormats =
        sqlLine.getOutputFormats().keySet().stream()
            .map(t -> t.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    if (availableFormats.contains(outputFormat.toUpperCase(Locale.ROOT))) {
      set(OUTPUT_FORMAT, outputFormat);
    } else {
      sqlLine.error(
          sqlLine.loc("unknown-value",
              OUTPUT_FORMAT.propertyName(),
              outputFormat,
              sqlLine.getOutputFormats().keySet()));
    }
  }

  public boolean getStrictJdbc() {
    return getBoolean(STRICT_JDBC);
  }

  public String getPropertiesFile() {
    return get(PROPERTIES_FILE);
  }

  public void setPropertiesFile(String propertyFile) {
    final String oldPropertyFile = get(PROPERTIES_FILE);
    if (Objects.equals(propertyFile, oldPropertyFile)
        || (Objects.equals(PROPERTIES_FILE.defaultValue(), oldPropertyFile)
            && DEFAULT.equalsIgnoreCase(propertyFile))) {
      return;
    }
    // reset properties
    propertiesMap.clear();
    set(PROPERTIES_FILE, propertyFile);
    if (DEV_NULL.equals(propertyFile)) {
      return;
    }
    try {
      load();
    } catch (IOException e) {
      sqlLine.handleException(e);
    }
  }

  public void setRun(String runFile) {
    this.runFile = runFile;
  }

  public String getRun() {
    return this.runFile;
  }

  public boolean getConfirm() {
    return getBoolean(CONFIRM);
  }

  public String getConfirmPattern() {
    return get(CONFIRM_PATTERN);
  }

  public void setConfirmPattern(String confirmPattern) {
    set(CONFIRM_PATTERN, getValidConfirmPatternOrThrow(confirmPattern));
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

  private String getValidConfirmPatternOrThrow(String confirmPattern) {
    if (confirmPattern == null) {
      throw new IllegalArgumentException("Confirm pattern is null");
    }
    if (DEFAULT.equalsIgnoreCase(confirmPattern)) {
      confirmPattern = sqlLine.loc("default-confirm-pattern");
    }
    set(CONFIRM_PATTERN, confirmPattern);
    try {
      compiledConfirmPattern = Pattern.compile(confirmPattern);
    } catch (PatternSyntaxException ex) {
      throw new IllegalArgumentException("confirmPattern is invalid regex: "
          + confirmPattern);
    }
    return confirmPattern;
  }

  public Pattern getCompiledConfirmPattern() {
    if (compiledConfirmPattern == null) {
      compiledConfirmPattern = Pattern.compile(getConfirmPattern());
    }
    return compiledConfirmPattern;
  }

  public String getHistoryFlags() {
    return get(HISTORY_FLAGS);
  }
}

// End SqlLineOpts.java
