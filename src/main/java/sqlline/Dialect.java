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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database-specific rule which is used for highlighting,
 * completion and line continuation.
 *
 * <p>Provides an additional set of keywords,
 * and the quotation character for SQL identifiers.
 */
interface Dialect {
  Set<String> DEFAULT_KEYWORD_SET = BuiltInDialect.initDefaultKeywordSet();
  // SQL92 comment prefix is "--"
  // sqlline also supports shell-style "#" prefix
  String[] SQLLINE_ONE_LINE_COMMENTS = {"#", "--"};

  boolean containsKeyword(String keyword);

  Set<String> getOneLineComments();

  default Set<String> getSqlLineOneLineComments() {
    return Arrays.stream(
        SQLLINE_ONE_LINE_COMMENTS).collect(Collectors.toSet());
  }

  char getOpenQuote();

  char getCloseQuote();

  boolean isLower();

  boolean isUpper();

  /**
   * Retrieves all the "extra" characters that can be used
   * in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
   *
   * @return the string containing the extra characters
   */
  String getExtraNameCharacters();
}

// End Dialect.java
