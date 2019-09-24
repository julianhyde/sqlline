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

import org.jline.utils.AttributedStyle;

/**
 * Class to specify colors and styles while highlighting.
 */
public class HighlightStyle {
  private final AttributedStyle keywordStyle;
  private final AttributedStyle commandStyle;
  private final AttributedStyle quotedStyle;
  private final AttributedStyle identifierStyle;
  private final AttributedStyle commentStyle;
  private final AttributedStyle numberStyle;
  private final AttributedStyle defaultStyle;

  /** Creates a HighlightStyle.
   *
   * @param keywordStyle Style for SQL keywords
   * @param commandStyle Style for SQLLine commands
   * @param quotedStyle Style for SQL character literals
   * @param identifierStyle Style for SQL identifiers
   * @param commentStyle Style for SQL comments
   * @param numberStyle Style for numeric values
   * @param defaultStyle Default style
   */
  public HighlightStyle(AttributedStyle keywordStyle,
      AttributedStyle commandStyle,
      AttributedStyle quotedStyle,
      AttributedStyle identifierStyle,
      AttributedStyle commentStyle,
      AttributedStyle numberStyle,
      AttributedStyle defaultStyle) {
    this.keywordStyle = keywordStyle;
    this.commandStyle = commandStyle;
    this.quotedStyle = quotedStyle;
    this.identifierStyle = identifierStyle;
    this.commentStyle = commentStyle;
    this.numberStyle = numberStyle;
    this.defaultStyle = defaultStyle;
  }

  /** Returns the style for a SQL keyword such as {@code SELECT} or
   * {@code ON}.
   *
   * @return Style for SQL keywords
   */
  public AttributedStyle getKeywordStyle() {
    return keywordStyle;
  }

  /** Returns the style for a SQL character literal, such as
   * {@code 'Hello, world!'}.
   *
   * @return Style for SQL character literals
   */
  public AttributedStyle getQuotedStyle() {
    return quotedStyle;
  }

  /** Returns the style for a SQL identifier, such as
   * {@code EMP} or {@code "Employee table"}.
   *
   * @return Style for SQL identifiers
   */
  public AttributedStyle getIdentifierStyle() {
    return identifierStyle;
  }

  /** Returns the style for a SQL comments, such as
   * {@literal /* This is a comment *}{@literal /} or
   * {@literal -- End of line comment}.
   *
   * @return Style for SQL comments
   */
  public AttributedStyle getCommentStyle() {
    return commentStyle;
  }

  /** Returns the style for numeric literals.
   *
   * @return Style for numeric literals
   */
  public AttributedStyle getNumberStyle() {
    return numberStyle;
  }

  /** Returns the style for text that does not match any other style.
   *
   * @return Default style
   */
  public AttributedStyle getDefaultStyle() {
    return defaultStyle;
  }

  /** Returns the style for SQLLine commands.
   *
   * @return Command style
   */
  public AttributedStyle getCommandStyle() {
    return commandStyle;
  }
}

// End HighlightStyle.java
