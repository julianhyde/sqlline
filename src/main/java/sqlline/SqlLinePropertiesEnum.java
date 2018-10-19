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

import java.io.File;

/**
 * Properties that may be specified for SqlLine.
 */
public enum SqlLinePropertiesEnum implements SqlLineProperty {

  AUTO_COMMIT("autoCommit", Type.BOOLEAN, true),
  AUTO_SAVE("autoSave", Type.BOOLEAN, false),
  COLOR("color", Type.BOOLEAN, false),
  CSV_DELIMITER("csvDelimiter", Type.STRING, ","),

  CSV_QUOTE_CHARACTER("csvQuoteCharacter", Type.CHAR, '\''),

  DATE_FORMAT("dateFormat", Type.STRING, DEFAULT),

  FAST_CONNECT("fastConnect", Type.BOOLEAN, true),
  FORCE("force", Type.BOOLEAN, false),
  HEADER_INTERVAL("headerInterval", Type.INTEGER, 100),
  HISTORY_FILE("historyFile", Type.STRING,
      new File(SqlLineOpts.saveDir(), "history").getAbsolutePath()),
  INCREMENTAL("incremental", Type.BOOLEAN, true),
  ISOLATION("isolation", Type.STRING, "TRANSACTION_REPEATABLE_READ"),
  MAX_COLUMN_WIDTH("maxColumnWidth", Type.INTEGER, 15),
  // don't save maxheight, maxwidth: it is automatically set based on
  // the terminal configuration
  MAX_HEIGHT("maxHeight", Type.INTEGER, 80, false, false),
  MAX_WIDTH("maxWidth", Type.INTEGER, 80, false, false),

  NUMBER_FORMAT("numberFormat", Type.STRING, DEFAULT),
  NULL_VALUE("nullValue", Type.STRING, DEFAULT),
  SILENT("silent", Type.BOOLEAN, false),
  OUTPUT_FORMAT("outputFormat", Type.STRING, "table"),
  ROW_LIMIT("rowLimit", Type.INTEGER, 0),
  SHOW_ELAPSED_TIME("showElapsedTime", Type.BOOLEAN, true),
  SHOW_HEADER("showHeader", Type.BOOLEAN, true),
  SHOW_NESTED_ERRS("showNestedErrs", Type.BOOLEAN, false),
  SHOW_WARNINGS("showWarnings", Type.BOOLEAN, true),
  TIME_FORMAT("timeFormat", Type.STRING, DEFAULT),
  TIMEOUT("timeout", Type.INTEGER, -1),
  TIMESTAMP_FORMAT("timestampFormat", Type.STRING, DEFAULT),
  TRIM_SCRIPTS("trimScripts", Type.BOOLEAN, true),
  VERBOSE("verbose", Type.BOOLEAN, false),
  VERSION("version", Type.STRING, new Application().getVersion(), false, true);

  private final String propertyName;
  private final Type type;
  private final Object defaultValue;
  private final boolean isReadOnly;
  private final boolean couldBeStored;

  SqlLinePropertiesEnum(String propertyName, Type type, Object defaultValue) {
    this(propertyName, type, defaultValue, true, false);
  }

  SqlLinePropertiesEnum(
      String propertyName,
      Type type,
      Object defaultValue,
      boolean couldBeStored,
      boolean isReadOnly) {
    this.propertyName = propertyName;
    this.type = type;
    this.defaultValue = defaultValue;
    this.isReadOnly = isReadOnly;
    this.couldBeStored = couldBeStored;
  }

  @Override
  public String propertyName() {
    return propertyName;
  }

  @Override
  public Object defaultValue() {
    return defaultValue;
  }

  @Override
  public boolean isReadOnly() {
    return isReadOnly;
  }

  @Override
  public boolean couldBeStored() {
    return couldBeStored;
  }

  @Override
  public Type type() {
    return type;
  }

  public static SqlLineProperty valueOf(
      String propertyName, boolean ignoreCase) {
    for (SqlLineProperty property : values()) {
      if ((ignoreCase && property.propertyName().equalsIgnoreCase(propertyName))
          || property.propertyName().equals(propertyName)) {
        return property;
      }
    }
    return null;
  }
}

// End SqlLinePropertiesEnum.java
