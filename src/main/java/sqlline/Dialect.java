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

import java.sql.DatabaseMetaData;
import java.util.Set;

/**
 * Database-specific rule which is used for highlighting,
 * completion and line continuation.
 *
 * <p>Provides an additional set of keywords,
 * and the quotation character for SQL identifiers.
 */
interface Dialect {
  Set<String> DEFAULT_KEYWORD_SET = BuiltInDialect.initDefaultKeywordSet();

  boolean containsKeyword(String keyword);

  Set<String> getOneLineComments();

  /** Returns the character that starts quoted identifiers in this dialect,
  char getOpenQuote();

  char getCloseQuote();

  /** Whether this dialect stores unquoted identifiers in upper-case.
   *
   * @see DatabaseMetaData#storesUpperCaseIdentifiers() */
  boolean storesUpperCaseIdentifiers();
}

// End Dialect.java
