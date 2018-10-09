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

import java.util.HashMap;
import java.util.Map;

import org.jline.utils.AttributedStyle;

/**
 * Class to specify colors and styles while highlighting.
 */
public class HighlightStyle {
  private static final AttributedStyle GREEN =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
  private static final AttributedStyle CYAN =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
  private static final AttributedStyle BRIGHT =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT);
  private static final AttributedStyle YELLOW =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
  private static final AttributedStyle WHITE =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
  private static final AttributedStyle BLACK =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK);
  private static final AttributedStyle MAGENTA =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
  private static final AttributedStyle RED =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
  private static final AttributedStyle BLUE =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

  private static final AttributedStyle BOLD_GREEN =
      AttributedStyle.BOLD.foreground(AttributedStyle.GREEN);
  private static final AttributedStyle BOLD_CYAN =
      AttributedStyle.BOLD.foreground(AttributedStyle.CYAN);
  private static final AttributedStyle BOLD_BRIGHT =
      AttributedStyle.BOLD.foreground(AttributedStyle.BRIGHT);
  private static final AttributedStyle BOLD_YELLOW =
      AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
  private static final AttributedStyle BOLD_WHITE =
      AttributedStyle.BOLD.foreground(AttributedStyle.WHITE);
  private static final AttributedStyle BOLD_BLACK =
      AttributedStyle.BOLD.foreground(AttributedStyle.BLACK);
  private static final AttributedStyle BOLD_MAGENTA =
      AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA);
  private static final AttributedStyle BOLD_RED =
      AttributedStyle.BOLD.foreground(AttributedStyle.RED);
  private static final AttributedStyle BOLD_BLUE =
      AttributedStyle.BOLD.foreground(AttributedStyle.BLUE);

  private static final AttributedStyle ITALIC_GREEN =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.GREEN);
  private static final AttributedStyle ITALIC_CYAN =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.CYAN);
  private static final AttributedStyle ITALIC_BRIGHT =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BRIGHT);
  private static final AttributedStyle ITALIC_YELLOW =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.YELLOW);
  private static final AttributedStyle ITALIC_WHITE =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.WHITE);
  private static final AttributedStyle ITALIC_BLACK =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BLACK);
  private static final AttributedStyle ITALIC_MAGENTA =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.MAGENTA);
  private static final AttributedStyle ITALIC_RED =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.RED);
  private static final AttributedStyle ITALIC_BLUE =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BLUE);

  static final Map<String, HighlightStyle> NAME2HIGHLIGHT_STYLE =
      new HashMap<String, HighlightStyle>() {{
          put("dark", new HighlightStyle(
              BOLD_BLUE, BOLD_WHITE, GREEN, CYAN,
              ITALIC_BRIGHT, YELLOW, WHITE));
          put("light", new HighlightStyle(
              BOLD_RED, BOLD_BLACK, GREEN, CYAN,
              ITALIC_BRIGHT, YELLOW, BLACK));
          // The next four schemes inspired by
          // https://github.com/Gillisdc/sqldeveloper-syntax-highlighting
          // not the same but more or less similar
          put("chester", new HighlightStyle(
              BOLD_BLUE, BOLD_WHITE, RED, CYAN, ITALIC_GREEN, YELLOW, WHITE));
          put("dracula", new HighlightStyle(
              BOLD_MAGENTA, BOLD_WHITE, GREEN,
              RED, ITALIC_CYAN, YELLOW, WHITE));
          put("solarized", new HighlightStyle(
              BOLD_YELLOW, BOLD_BLUE, GREEN, RED, ITALIC_BRIGHT, CYAN, BLUE));
          put("vs2010", new HighlightStyle(
              BOLD_BLUE, BOLD_WHITE, RED, MAGENTA,
              ITALIC_GREEN, BRIGHT, WHITE));
          // inspired by https://github.com/ozmoroz/ozbsidian-sqldeveloper
          // not the same but more or less similar
          put("ozbsidian", new HighlightStyle(
              BOLD_GREEN, BOLD_WHITE, RED, MAGENTA,
              ITALIC_BRIGHT, YELLOW, WHITE));
      }};

  private final AttributedStyle keyWordsStyle;
  private final AttributedStyle commandsStyle;
  private final AttributedStyle quotedStyle;
  private final AttributedStyle sqlIdentifierStyle;
  private final AttributedStyle commentedStyle;
  private final AttributedStyle numberStyle;
  private final AttributedStyle defaultStyle;

  public HighlightStyle(AttributedStyle keyWordsStyle,
                        AttributedStyle commandsStyle,
                        AttributedStyle quotedStyle,
                        AttributedStyle sqlIdentifierStyle,
                        AttributedStyle commentedStyle,
                        AttributedStyle numberStyle,
                        AttributedStyle defaultStyle) {
    this.keyWordsStyle = keyWordsStyle;
    this.commandsStyle = commandsStyle;
    this.quotedStyle = quotedStyle;
    this.sqlIdentifierStyle = sqlIdentifierStyle;
    this.commentedStyle = commentedStyle;
    this.numberStyle = numberStyle;
    this.defaultStyle = defaultStyle;
  }

  public static Map<String, HighlightStyle> getName2highlightStyle() {
    return NAME2HIGHLIGHT_STYLE;
  }

  public AttributedStyle getSqlKeywordStyle() {
    return keyWordsStyle;
  }

  public AttributedStyle getQuotedStyle() {
    return quotedStyle;
  }

  public AttributedStyle getSqlIdentifierStyle() {
    return sqlIdentifierStyle;
  }

  public AttributedStyle getCommentedStyle() {
    return commentedStyle;
  }

  public AttributedStyle getNumbersStyle() {
    return numberStyle;
  }

  public AttributedStyle getDefaultStyle() {
    return defaultStyle;
  }

  public AttributedStyle getCommandStyle() {
    return commandsStyle;
  }
}

// End HighlightStyle.java
