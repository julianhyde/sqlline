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
import java.util.Objects;
import java.util.Set;

/**
 * Custom implementation of {@link Dialect}.
 */
class DialectImpl implements Dialect {
  private static final DialectImpl DEFAULT =
      DialectImpl.create(null, null, null);

  private final Set<String> keywords;
  private final Set<String> oneLineComments;
  private final char openQuote;
  private final char closeQuote;
  private final boolean storesUpperCaseIdentifier;

  static DialectImpl create(Set<String> keywords, String identifierQuote,
      String productName) {
    return create(keywords, identifierQuote, productName, true);
  }

  static DialectImpl create(Set<String> keywords, String identifierQuote,
      String productName, boolean storesUpperCaseIdentifier) {
    final Set<String> keywords2 = keywords == null
        ? Collections.emptySet()
        : Collections.unmodifiableSet(keywords);

    // Find a built-in dialect corresponding to the database name. Never
    // null, but may be the default dialect (which behaves a bit like Oracle).
    final BuiltInDialect dialect = BuiltInDialect.valueOf(productName, true);
    final char openQuote;
    final char closeQuote;
    if (identifierQuote == null) {
      openQuote = dialect.getOpenQuote();
      closeQuote = dialect.getCloseQuote();
    } else {
      openQuote = identifierQuote.charAt(0);
      switch (openQuote) {
      case '[':
        closeQuote = ']';
        break;
      default:
        closeQuote = openQuote;
        break;
      }
    }
    return new DialectImpl(keywords2, storesUpperCaseIdentifier,
        dialect.getOneLineComments(), openQuote, closeQuote);
  }

  private DialectImpl(Set<String> keywords, boolean storesUpperCaseIdentifier,
      Set<String> oneLineComments, char openQuote, char closeQuote) {
    this.keywords = Objects.requireNonNull(keywords);
    this.storesUpperCaseIdentifier = storesUpperCaseIdentifier;
    this.oneLineComments = oneLineComments;
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
  }

  public static Dialect getDefault() {
    return DEFAULT;
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

  @Override public boolean storesUpperCaseIdentifiers() {
    return storesUpperCaseIdentifier;
  }
}

// End DialectImpl.java
