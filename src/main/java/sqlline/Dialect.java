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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pre-defined one-line comments, sql identifier quotes for different databases.
 */
public enum Dialect {
  DEFAULT(null, '"', "--"),
  // http://www.h2database.com/html/grammar.html#comment
  H2("H2", "--", "//"),
  // https://dev.mysql.com/doc/refman/8.0/en/comments.html
  // https://mariadb.com/kb/en/library/comment-syntax/
  MYSQL("MySQL", '`', "-- ", "--\t", "--\n", "#"),
  // https://phoenix.apache.org/language/index.html#comments
  PHOENIX("Phoenix", "--", "//");

  private final String databaseName;
  private final char sqlIdentifierQuote;
  private final Set<String> commentDefinition;

  Dialect(String dbName, String... comments) {
    this(dbName, DEFAULT_SQL_IDENTIFIER_QUOTE, comments);
  }

  Dialect(String dbName, char sqlIdentifierQuote, String... comments) {
    this.databaseName = dbName;
    this.sqlIdentifierQuote = sqlIdentifierQuote;
    commentDefinition = Collections.unmodifiableSet(
        Stream.of(comments).collect(Collectors.toSet()));
  }

  public Set<String> getCommentDefinition() {
    return commentDefinition;
  }

  public char getSqlIdentifierQuote() {
    return sqlIdentifierQuote;
  }

  static Dialect valueOf(String dbName, boolean ignoreCase) {
    if (dbName == null) {
      return DEFAULT;
    }
    for (Dialect dialect : values()) {
      if (dialect == DEFAULT) {
        continue;
      }
      if (dbName.length() >= dialect.databaseName.length()
          && dbName.regionMatches(ignoreCase, 0,
          dialect.databaseName, 0,
          dialect.databaseName.length())) {
        return dialect;
      }
    }
    return DEFAULT;
  }

  static final Map<String, Dialect> BY_NAME;

  static {
    final Map<String, Dialect> map = new HashMap<>();
    for (Dialect value : values()) {
      String dbName = value.databaseName;
      map.put(dbName == null ? null : dbName.toLowerCase(Locale.ROOT), value);
    }
    BY_NAME = Collections.unmodifiableMap(map);
  }

  private static final char DEFAULT_SQL_IDENTIFIER_QUOTE = '"';
}

// End Dialect.java

