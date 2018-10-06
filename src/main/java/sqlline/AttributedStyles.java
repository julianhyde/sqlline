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
 * A collection of pre-defined attributed styles.
 */
class AttributedStyles {
  private AttributedStyles() {}

  static final AttributedStyle GREEN =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
  static final AttributedStyle CYAN =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
  static final AttributedStyle BRIGHT =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT);
  static final AttributedStyle YELLOW =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
  static final AttributedStyle WHITE =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
  static final AttributedStyle BLACK =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK);
  static final AttributedStyle MAGENTA =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
  static final AttributedStyle RED =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
  static final AttributedStyle BLUE =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

  static final AttributedStyle BOLD_GREEN =
      AttributedStyle.BOLD.foreground(AttributedStyle.GREEN);
  static final AttributedStyle BOLD_CYAN =
      AttributedStyle.BOLD.foreground(AttributedStyle.CYAN);
  static final AttributedStyle BOLD_BRIGHT =
      AttributedStyle.BOLD.foreground(AttributedStyle.BRIGHT);
  static final AttributedStyle BOLD_YELLOW =
      AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
  static final AttributedStyle BOLD_WHITE =
      AttributedStyle.BOLD.foreground(AttributedStyle.WHITE);
  static final AttributedStyle BOLD_BLACK =
      AttributedStyle.BOLD.foreground(AttributedStyle.BLACK);
  static final AttributedStyle BOLD_MAGENTA =
      AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA);
  static final AttributedStyle BOLD_RED =
      AttributedStyle.BOLD.foreground(AttributedStyle.RED);
  static final AttributedStyle BOLD_BLUE =
      AttributedStyle.BOLD.foreground(AttributedStyle.BLUE);

  static final AttributedStyle ITALIC_GREEN =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.GREEN);
  static final AttributedStyle ITALIC_CYAN =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.CYAN);
  static final AttributedStyle ITALIC_BRIGHT =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BRIGHT);
  static final AttributedStyle ITALIC_YELLOW =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.YELLOW);
  static final AttributedStyle ITALIC_WHITE =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.WHITE);
  static final AttributedStyle ITALIC_BLACK =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BLACK);
  static final AttributedStyle ITALIC_MAGENTA =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.MAGENTA);
  static final AttributedStyle ITALIC_RED =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.RED);
  static final AttributedStyle ITALIC_BLUE =
      AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BLUE);
}

// End AttributedStyles.java
