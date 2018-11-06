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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pre-defined one-line comments for different databases.
 */
public enum BuiltInCommentsDefinition {
  DEFAULT(null, "--"),
  MYSQL("MySQL", "-- ", "--\n", "#"),
  MARIADB("MySQL", "-- ", "--\n", "#"),
  CASSANDRA("Cassandra", "--", "//"),
  PHOENIX("Phoenix", "--", "//");

  private final String databaseName;
  private final Set<String> commentDefinition;

  BuiltInCommentsDefinition(String dbName, String... comments) {
    this.databaseName = dbName;
    commentDefinition = new HashSet<>(Arrays.asList(comments));
  }

  public Set<String> getCommentDefinition() {
    return commentDefinition;
  }

  static final Map<String, BuiltInCommentsDefinition> BY_NAME;

  static {
    final Map<String, BuiltInCommentsDefinition> map = new HashMap<>();
    for (BuiltInCommentsDefinition value : values()) {
      String dbName = value.databaseName;
      map.put(dbName == null ? null : dbName.toLowerCase(Locale.ROOT), value);
    }
    BY_NAME = Collections.unmodifiableMap(map);
  }

}

// End BuiltInCommentsDefinition.java

