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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

/**
 * Customization for the prompt shown at the start of each line.
 */
class Prompt {
  private static final String START_COLOR = "\\033";

  private static final Map<Character, Supplier<String>>
      DATE_TIME_FORMATS = Collections.unmodifiableMap(
        new HashMap<Character, Supplier<String>>() {
          {
            put('D', () -> getFormattedDateTime("yyyy-MM-dd HH:mm:ss.SSS"));
            put('m', () -> getFormattedDateTime("mm"));
            put('o', () -> getFormattedDateTime("MM"));
            put('O', () -> getFormattedDateTime("MMM"));
            put('P', () -> getFormattedDateTime("aa"));
            put('r', () -> getFormattedDateTime("hh:mm"));
            put('R', () -> getFormattedDateTime("HH:mm"));
            put('s', () -> getFormattedDateTime("ss"));
            put('w', () -> getFormattedDateTime("d"));
            put('W', () -> getFormattedDateTime("E"));
            put('y', () -> getFormattedDateTime("YY"));
            put('Y', () -> getFormattedDateTime("YYYY"));
          }
        });

  private static final StyleResolver STYLE_RESOLVER =
      new StyleResolver(s -> "");

  private Prompt() {
  }

  static AttributedString getRightPrompt(SqlLine sqlLine) {
    final int connectionIndex = sqlLine.getDatabaseConnections().getIndex();
    final String value = sqlLine.getOpts().get(BuiltInProperty.RIGHT_PROMPT);
    final String currentPrompt = String.valueOf((Object) null).equals(value)
        ? (String) BuiltInProperty.RIGHT_PROMPT.defaultValue() : value;
    return getPrompt(sqlLine, connectionIndex, currentPrompt);
  }

  static AttributedString getPrompt(SqlLine sqlLine) {
    final String defaultPrompt =
        String.valueOf(BuiltInProperty.PROMPT.defaultValue());
    final String currentPrompt = sqlLine.getOpts().get(BuiltInProperty.PROMPT);
    DatabaseConnection dbc = sqlLine.getDatabaseConnection();
    boolean useDefaultPrompt =
        String.valueOf((Object) null).equals(currentPrompt)
            || Objects.equals(currentPrompt, defaultPrompt);
    if (dbc == null || dbc.getUrl() == null) {
      return useDefaultPrompt
          ? getDefaultPrompt(-1, null, defaultPrompt)
          : getPrompt(sqlLine, -1, currentPrompt);
    } else {
      final int connectionIndex = sqlLine.getDatabaseConnections().getIndex();
      if (useDefaultPrompt || dbc.getNickname() != null) {
        final String nickNameOrUrl =
            dbc.getNickname() == null ? dbc.getUrl() : dbc.getNickname();
        return getDefaultPrompt(connectionIndex, nickNameOrUrl, defaultPrompt);
      } else {
        return getPrompt(sqlLine, connectionIndex, currentPrompt);
      }
    }
  }

  private static AttributedString getPrompt(
      SqlLine sqlLine, int connectionIndex, String prompt) {
    AttributedStringBuilder promptStringBuilder = new AttributedStringBuilder();
    final DatabaseConnection databaseConnection =
        sqlLine.getDatabaseConnection();
    final DatabaseMetaDataWrapper databaseMetaData = databaseConnection == null
        ? null : databaseConnection.meta;
    final SqlLineOpts opts = sqlLine.getOpts();
    for (int i = 0; i < prompt.length(); i++) {
      switch (prompt.charAt(i)) {
      case '%':
        if (i < prompt.length() - 1) {
          final Supplier<String> dateFormat =
              DATE_TIME_FORMATS.get(prompt.charAt(i + 1));
          if (dateFormat != null) {
            promptStringBuilder.append(dateFormat.get());
          } else {
            switch (prompt.charAt(i + 1)) {
            case 'c':
              if (connectionIndex >= 0) {
                promptStringBuilder.append(String.valueOf(connectionIndex));
              }
              break;
            case 'C':
              if (connectionIndex >= 0) {
                promptStringBuilder
                    .append(String.valueOf(connectionIndex)).append(": ");
              }
              break;
            case 'd':
              if (databaseConnection != null) {
                try {
                  promptStringBuilder
                      .append(databaseMetaData.getDatabaseProductName());
                } catch (Exception e) {
                  // skip
                }
              }
              break;
            case 'n':
              if (databaseConnection != null) {
                try {
                  promptStringBuilder
                      .append(databaseMetaData.getUserName());
                } catch (Exception e) {
                  // skip
                }
              }
              break;
            case 'u':
              if (databaseConnection != null) {
                promptStringBuilder.append(databaseConnection.getUrl());
              }
              break;
            case '[':
              int closeBracketIndex = prompt.indexOf("%]", i + 2);
              if (closeBracketIndex > 0) {
                String color = prompt.substring(i + 2, closeBracketIndex);
                AttributedStyle style = STYLE_RESOLVER.resolve(color);
                promptStringBuilder.style(style);
                i = closeBracketIndex;
                break;
              }
            // fall through if not a color
            case ':':
              int nextColonIndex = prompt.indexOf(":", i + 2);
              SqlLineProperty property;
              if (nextColonIndex > 0
                  && ((property = BuiltInProperty.valueOf(
                  prompt.substring(i + 2, nextColonIndex),
                  true)) != null)) {
                promptStringBuilder.append(opts.get(property));
                i = nextColonIndex - 1;
                break;
              }
            // fall through if not a variable name
            default:
              promptStringBuilder
                  .append(prompt.charAt(i)).append(prompt.charAt(i + 1));
            }
          }
          i = i + 1;
        }
        break;
      default:
        promptStringBuilder.append(prompt.charAt(i));
      }
    }
    return promptStringBuilder.toAttributedString();
  }

  private static AttributedString getDefaultPrompt(
      int connectionIndex, String url, String defaultPrompt) {
    String resultPrompt;
    if (url == null || url.length() == 0) {
      return new AttributedString(defaultPrompt);
    } else {
      if (url.contains(";")) {
        url = url.substring(0, url.indexOf(";"));
      }
      if (url.contains("?")) {
        url = url.substring(0, url.indexOf("?"));
      }
      resultPrompt = connectionIndex + ": " + url;
      if (resultPrompt.length() > 45) {
        resultPrompt = resultPrompt.substring(0, 45);
      }
      return new AttributedString(resultPrompt + "> ");
    }
  }

  private static String getFormattedDateTime(final String pattern) {
    final SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ROOT);
    return sdf.format(new Date());
  }
}

// End Prompt.java
