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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * DB specific rule which is used for highlighting,
 * completion and line continuation.
 *
 * <p>Provides an additional set of keywords,
 * and the quotation character for SQL identifiers.
 */
class DialectRule {
  private static final DialectRule DEFAULT_RULE =
      new DialectRule(null, null, null);
  private static final Set<String> DEFAULT_KEY_WORD_SET;
  static {
    DEFAULT_KEY_WORD_SET = initDefaultKeywordSet();
  }
  private final Set<String> keywords;
  private final Set<String> oneLineComments;
  private final char openQuote;
  private final char closeQuote;
  private final boolean upper;

  DialectRule(
      Set<String> keywords, String identifierQuote, String productName) {
    this(keywords, identifierQuote, productName, true);
  }

  DialectRule(Set<String> keywords, String identifierQuote,
      String productName, boolean storesUpperCaseIdentifier) {
    this.keywords = keywords == null
        ? Collections.emptySet()
        : Collections.unmodifiableSet(keywords);
    Dialect dialect =
        Dialect.valueOf(productName, true);
    upper = storesUpperCaseIdentifier;
    if (dialect != Dialect.MYSQL) {
      if ("[".equals(identifierQuote)) {
        openQuote = '[';
        closeQuote = ']';
      } else {
        String quote = identifierQuote == null
            ? String.valueOf(dialect.getSqlIdentifierQuote())
            : identifierQuote;
        openQuote = quote.charAt(0);
        closeQuote = quote.charAt(0);
      }
    } else {
      openQuote = dialect.getSqlIdentifierQuote();
      closeQuote = dialect.getSqlIdentifierQuote();
    }
    oneLineComments = dialect.getCommentDefinition();
  }

  public static DialectRule getDefaultRule() {
    return DEFAULT_RULE;
  }

  protected boolean containsKeyword(String keyword) {
    return keywords.contains(keyword)
        || DEFAULT_KEY_WORD_SET.contains(keyword);
  }

  public Set<String> getOneLineComments() {
    return oneLineComments;
  }

  public char getOpenQuote() {
    return openQuote;
  }

  public char getCloseQuote() {
    return closeQuote;
  }

  public boolean isUpper() {
    return upper;
  }

  public Set<String> getDefaultKeyWordSet() {
    return DEFAULT_KEY_WORD_SET;
  }

  private static Set<String> initDefaultKeywordSet() {
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
}

// End DialectRule.java
