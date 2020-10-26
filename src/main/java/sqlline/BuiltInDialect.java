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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pre-defined one-line comments, sql identifier quotes for different databases.
 */
public enum BuiltInDialect implements Dialect {
  /** Default built-in dialect. Does not correspond to any particular database,
   * but behaves similarly to Oracle and PostgreSQL. */
  DEFAULT("SQLLineDefaultDialect", '"', '"', "", "--"),

  POSTGRESQL("PostgreSQL", '"', '"', "",
      new String[] {"--"}, BuiltInDialect.postgresPgSqlBlocksBoundaries()),

  ORACLE("Oracle", '"', '"', "",
      new String[] {"--"}, BuiltInDialect.oraclePLSqlBlocksBoundaries()),

  /** HyperSQL dialect.
   * See <a href="https://www.h2database.com/html/grammar.html#comment">HyperSQL
   * grammar.</a>. */
  H2("H2", '"', '"', "", "--", "//"),

  /** MySQL dialect.
   * See <a href="https://dev.mysql.com/doc/refman/8.0/en/comments.html">MySQL
   * grammar</a>
   * and <a href="https://mariadb.com/kb/en/library/comment-syntax/">MariaDB
   * grammar</a>. */
  MYSQL("MySQL", '`', '`', "#@", "-- ", "--\t", "--\n", "#"),

  /** Apache Phoenix dialect.
   * See <a href="https://phoenix.apache.org/language/index.html#comments">
   * Phoenix grammar</a>. */
  PHOENIX("Phoenix", '"', '"', "", "--", "//");

  private final String databaseName;
  private final Set<String> oneLineComments;
  private final Set<String> keywords;
  private final boolean storesUpperCaseIdentifier;
  private final boolean storesLowerCaseIdentifier;
  private final char openQuote;
  private final char closeQuote;
  private final String extraNameCharacters;
  private final CodeBlocks codeBlocks;

  BuiltInDialect(String databaseName, char openQuote, char closeQuote,
      String extraNameCharacters, String... comments) {
    this(databaseName, openQuote, closeQuote,
        extraNameCharacters, comments, null);
  }

  BuiltInDialect(String databaseName, char openQuote, char closeQuote,
      String extraNameCharacters, String[] comments,
      CodeBlocks codeBlocks) {
    this.databaseName = Objects.requireNonNull(databaseName);
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
    this.oneLineComments = Collections.unmodifiableSet(
        Stream.of(comments).collect(Collectors.toSet()));
    this.storesUpperCaseIdentifier = false;
    this.storesLowerCaseIdentifier = false;
    this.extraNameCharacters = extraNameCharacters;
    this.keywords = Collections.emptySet();
    this.codeBlocks = codeBlocks;
  }

  @Override public boolean containsKeyword(String keyword) {
    return keywords.contains(keyword)
        || DEFAULT_KEYWORD_SET.contains(keyword);
  }

  @Override public Set<String> getOneLineComments() {
    return oneLineComments;
  }

  @Override public char getOpenQuote() {
    return openQuote;
  }

  @Override public char getCloseQuote() {
    return closeQuote;
  }

  @Override public boolean isLower() {
    return storesLowerCaseIdentifier;
  }

  @Override public boolean isUpper() {
    return storesUpperCaseIdentifier;
  }

  @Override public String getExtraNameCharacters() {
    return extraNameCharacters;
  }

  @Override public CodeBlocks getCodeBlocks() {
    return codeBlocks;
  }

  static Set<String> initDefaultKeywordSet() {
    try {
      final Set<String> defaultKeywordSet = new TreeSet<>();
      String keywords =
          new BufferedReader(
              new InputStreamReader(
                  SqlCompleter.class.getResourceAsStream(
                      "sql-keywords.properties"), StandardCharsets.UTF_8))
              .readLine();
      keywords += "," + keywords.toLowerCase(Locale.ROOT);
      for (StringTokenizer tok = new StringTokenizer(keywords, ",");
           tok.hasMoreTokens();) {
        defaultKeywordSet.add(tok.nextToken());
      }
      return Collections.unmodifiableSet(defaultKeywordSet);
    } catch (Exception e) {
      return Collections.emptySet();
    }
  }

  /** Finds a built-in dialect that matches a given database name. If not found,
   * returns the default dialect, never null.  */
  static BuiltInDialect valueOf(String databaseName, boolean ignoreCase) {
    if (databaseName == null) {
      return DEFAULT;
    }
    for (BuiltInDialect dialect : values()) {
      if (dialect == DEFAULT) {
        continue;
      }
      if (databaseName.length() >= dialect.databaseName.length()
          && databaseName.regionMatches(ignoreCase, 0,
              dialect.databaseName, 0,
              dialect.databaseName.length())) {
        return dialect;
      }
    }
    return DEFAULT;
  }

  private static CodeBlocks postgresPgSqlBlocksBoundaries() {
    final Pattern postgresqlStartBlock = Pattern.compile("^\\$[a-zA-Z]*\\$$");
    final Pattern postgresqlEndBlockSecondPart = Pattern.compile("^\\s*;*$");
    return new CodeBlocks() {
      @Override public Predicate<String> isBlockStarted() {
        return currentWord ->
            postgresqlStartBlock.matcher(currentWord).find();
      }

      @Override public BiPredicate<String, String> isBlockEnded() {
        return (prevWord, currentWord) -> prevWord.equals(currentWord)
            || currentWord.startsWith(prevWord)
                && postgresqlEndBlockSecondPart
                    .matcher(currentWord.substring(prevWord.length())).find();
      }
    };
  }

  private static CodeBlocks oraclePLSqlBlocksBoundaries() {
    final Pattern oracleOptionalStartBlock =
        Pattern.compile("^declare$", Pattern.CASE_INSENSITIVE);
    final Pattern oracleStartBlock =
        Pattern.compile("^begin$", Pattern.CASE_INSENSITIVE);
    final Pattern endBlock =
        Pattern.compile("^end\\s*;$", Pattern.CASE_INSENSITIVE);

    return new CodeBlocks() {
      @Override public Predicate<String> isBlockStarting() {
        return oracleOptionalStartBlock.asPredicate();
      }

      @Override public Predicate<String> isBlockStarted() {
        return currentWord ->
            oracleStartBlock.matcher(currentWord).find();
      }

      @Override public BiPredicate<String, String> isBlockEnded() {
        return (s, s2) -> endBlock.matcher(s2).find();
      }
    };
  }
}

// End BuiltInDialect.java
