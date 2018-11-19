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

import java.util.Set;

/**
 * Definition of property that may be specified for SqlLine.
 *
 * @see BuiltInProperty
 */
public interface SqlLineProperty {
  String DEFAULT = "default";
  String[] BOOLEAN_VALUES = {
      Boolean.TRUE.toString(), Boolean.FALSE.toString()};

  String propertyName();

  Object defaultValue();

  boolean isReadOnly();

  boolean couldBeStored();

  Type type();

  Set<String> getAvailableValues();

  /** Property writer. */
  @FunctionalInterface
  interface Writer {
    void write(String value);
  }

  /** Data type of property. */
  enum Type {
    BOOLEAN,
    CHAR,
    INTEGER,
    STRING;
  }

}

// End SqlLineProperty.java
