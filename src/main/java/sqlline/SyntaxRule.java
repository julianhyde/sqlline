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
 * Rules for highlighting.
 *
 * <p>Provides an additional set of keywords,
 * and the quotation character for SQL identifiers.
 */
public class SyntaxRule {
  private static final String DEFAULT_SQL_IDENTIFIER_QUOTE = "\"";
  private static final SyntaxRule DEFAULT_RULE =
      new SyntaxRule(null, null, null);
  private static final Set<String> DEFAULT_KEY_WORD_SET;
  static {
    DEFAULT_KEY_WORD_SET = initDefaultKeywordSet();
  }
  private final Set<String> keywords;
  private final Set<String> oneLineComments;
  private final char openQuote;
  private final char closeQuote;
  private final boolean upper;

  SyntaxRule(
      Set<String> keywords, String identifierQuote, String productName) {
    this(keywords, identifierQuote, productName, true);
  }

  SyntaxRule(Set<String> keywords, String identifierQuote,
             String productName, boolean storesUpperCaseIdentifier) {
    this.keywords = keywords == null ? Collections.emptySet() : keywords;

    if (identifierQuote == null
        || identifierQuote.equals("")
        || identifierQuote.equals(" ")) {
      if (productName != null && productName.startsWith("MySQL")) {
        // Some version of the MySQL JDBC driver lie.
        openQuote = '`';
        closeQuote = '`';
        upper = storesUpperCaseIdentifier;
      } else {
        openQuote = DEFAULT_SQL_IDENTIFIER_QUOTE.charAt(0);
        closeQuote = DEFAULT_SQL_IDENTIFIER_QUOTE.charAt(0);
        upper = storesUpperCaseIdentifier;
      }
    } else if (identifierQuote.equals("[")) {
      openQuote = '[';
      closeQuote = ']';
      upper = storesUpperCaseIdentifier;
    } else {
      openQuote = identifierQuote.charAt(0);
      closeQuote = identifierQuote.charAt(0);
      upper = storesUpperCaseIdentifier;
    }

    BuiltInCommentsDefinition builtInCommentsDefinition =
        BuiltInCommentsDefinition.valueOf(productName, false);
    oneLineComments = builtInCommentsDefinition.getCommentDefinition();
  }

  public static SyntaxRule getDefaultRule() {
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

// End SyntaxRule.java
