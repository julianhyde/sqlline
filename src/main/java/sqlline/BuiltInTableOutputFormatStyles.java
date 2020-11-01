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

public enum BuiltInTableOutputFormatStyles {
  DEFAULT('-', '+', '+', '+', '|', '+', '+', '+', '-', '|', '+', '+', '+'),
  BOLD_HEADER_SOLID('━', '┏', '┓', '┳', '┃', '╇',
      '┡', '┩', '─', '│', '└', '┘', '┴'),
  BOLD_SOLID('━', '┏', '┓', '┳', '┃', '╋',
      '┣', '┫', '━', '┃', '┗', '┛', '┻'),
  DOUBLE_SOLID('═', '╔', '╗', '╦', '║', '╬', '╠',
      '╣', '═', '║', '╚', '╝', '╩'),
  ROUND_CORNERS('─', '╭', '╮', '┬', '│', '┼', '├',
      '┤', '─', '│', '╰', '╯', '┴'),
  SOLID('─', '┌', '┐', '┬', '│', '┼', '├',
      '┤', '─', '│', '└', '┘', '┴');
  private final TableOutputFormatStyle style;

  BuiltInTableOutputFormatStyles(char headerLine, char headerTopLeft,
      char headerTopRight, char headerCrossDown, char headerSeparator,
      char headerBodyCross, char headerBodyCrossLeft,
      char headerBodyCrossRight, char bodyLine, char bodySeparator,
      char bodyBottomLeft, char bodyBottomRight, char bodyCrossUp) {
    style = new TableOutputFormatStyle(headerLine, headerTopLeft,
        headerTopRight, headerCrossDown, headerSeparator, headerBodyCross,
        headerBodyCrossLeft, headerBodyCrossRight, bodyLine, bodySeparator,
        bodyBottomLeft, bodyBottomRight, bodyCrossUp);
  }

  public TableOutputFormatStyle getStyle() {
    return style;
  }

  static final Map<String, TableOutputFormatStyle> BY_NAME;

  static {
    final Map<String, TableOutputFormatStyle> map = new HashMap<>();
    for (BuiltInTableOutputFormatStyles value : values()) {
      map.put(value.name().toLowerCase(Locale.ROOT), value.style);
    }
    BY_NAME = Collections.unmodifiableMap(map);
  }
}

// End BuiltInTableOutputFormats.java
