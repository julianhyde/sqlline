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
import java.util.function.Supplier;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

/**
 * Default prompt handler class which allows customization
 * for the prompt shown at the start of each line.
 *
 * <p>This class can be extended to allow customizations for:
 * default prompt, prompt or right prompt.
 */
public class PromptHandler {
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

  protected final SqlLine sqlLine;

  final Supplier<ScriptEngine> scriptEngineSupplier =
      getEngineSupplier();

  MemoizingSupplier<ScriptEngine> getEngineSupplier() {
    return new MemoizingSupplier<>(() -> {
      final ScriptEngineManager engineManager = new ScriptEngineManager();
      String engineName = sqlLine.getOpts().get(BuiltInProperty.SCRIPT_ENGINE);
      ScriptEngine scriptEngine = engineManager.getEngineByName(engineName);
      if (scriptEngine == null) {
        if (engineManager.getEngineFactories().isEmpty()) {
          sqlLine.error(sqlLine.loc("not-supported-script-engine-no-available",
              engineName));
        } else {
          sqlLine.error(sqlLine.loc("not-supported-script-engine",
              engineName, BuiltInProperty.SCRIPT_ENGINE.getAvailableValues()));
        }
      }
      return scriptEngine;
    });
  }

  public PromptHandler(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  public AttributedString getRightPrompt() {
    final int connectionIndex = sqlLine.getDatabaseConnections().getIndex();
    final String currentPrompt =
        sqlLine.getOpts().isDefault(BuiltInProperty.RIGHT_PROMPT)
            ? (String) BuiltInProperty.RIGHT_PROMPT.defaultValue()
            : sqlLine.getOpts().get(BuiltInProperty.RIGHT_PROMPT);
    return getPrompt(sqlLine, connectionIndex, currentPrompt);
  }

  public AttributedString getPrompt() {
    final int connectionIndex = sqlLine.getConnectionMetadata().getIndex();
    if (!sqlLine.getOpts().isDefault(BuiltInProperty.PROMPT_SCRIPT)) {
      final String promptScript =
          sqlLine.getOpts().get(BuiltInProperty.PROMPT_SCRIPT);
      return getPrompt(sqlLine, connectionIndex,
          getPromptFromScript(sqlLine, promptScript));
    }
    final String defaultPrompt =
        String.valueOf(BuiltInProperty.PROMPT.defaultValue());
    final String currentPrompt = sqlLine.getOpts().get(BuiltInProperty.PROMPT);
    final DatabaseConnection dbc = sqlLine.getDatabaseConnection();
    final boolean useDefaultPrompt =
        sqlLine.getOpts().isDefault(BuiltInProperty.PROMPT);
    if (dbc == null || dbc.getUrl() == null) {
      return getPrompt(sqlLine, connectionIndex, useDefaultPrompt
          ? getDefaultPrompt(connectionIndex, null, defaultPrompt)
          : currentPrompt);
    } else {
      if (useDefaultPrompt || dbc.getNickname() != null) {
        final String nickNameOrUrl =
            dbc.getNickname() == null ? dbc.getUrl() : dbc.getNickname();
        return getPrompt(sqlLine, connectionIndex,
            getDefaultPrompt(connectionIndex, nickNameOrUrl, defaultPrompt));
      } else {
        return getPrompt(sqlLine, connectionIndex, currentPrompt);
      }
    }
  }

  private String getPromptFromScript(SqlLine sqlLine,
      String promptScript) {
    try {
      final ScriptEngine engine = scriptEngineSupplier.get();
      if (engine == null) {
        return ">";
      } else {
        final Bindings bindings = new SimpleBindings();
        final ConnectionMetadata meta = sqlLine.getConnectionMetadata();
        bindings.put("connectionIndex", meta.getIndex());
        bindings.put("databaseProductName", meta.getDatabaseProductName());
        bindings.put("userName", meta.getUserName());
        bindings.put("url", meta.getUrl());
        bindings.put("currentSchema", meta.getCurrentSchema());
        final Object o = engine.eval(promptScript, bindings);
        return String.valueOf(o);
      }
    } catch (ScriptException e) {
      e.printStackTrace();
      return ">";
    }
  }

  protected AttributedString getPrompt(
      SqlLine sqlLine, int connectionIndex, String prompt) {
    AttributedStringBuilder promptStringBuilder = new AttributedStringBuilder();
    final SqlLineOpts opts = sqlLine.getOpts();
    final ConnectionMetadata meta = sqlLine.getConnectionMetadata();
    for (int i = 0; i < prompt.length(); i++) {
      switch (prompt.charAt(i)) {
      case '%':
        if (i < prompt.length() - 1) {
          String dateTime = formatDateTime(prompt.charAt(i + 1));
          if (dateTime != null) {
            promptStringBuilder.append(dateTime);
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
              String databaseProductName = meta.getDatabaseProductName();
              if (databaseProductName != null) {
                promptStringBuilder.append(databaseProductName);
              }
              break;
            case 'n':
              String userName = meta.getUserName();
              if (userName != null) {
                promptStringBuilder.append(userName);
              }
              break;
            case 'u':
              String url = meta.getUrl();
              if (url != null) {
                promptStringBuilder.append(url);
              }
              break;
            case 'S':
              String currentSchema = meta.getCurrentSchema();
              if (currentSchema != null) {
                promptStringBuilder.append(currentSchema);
              }
              break;
            case '[':
              int closeBracketIndex = prompt.indexOf("%]", i + 2);
              if (closeBracketIndex > 0) {
                String color = prompt.substring(i + 2, closeBracketIndex);
                AttributedStyle style = resolveStyle(color);
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

  protected String getDefaultPrompt(
      int connectionIndex, String url, String defaultPrompt) {
    String resultPrompt;
    if (url == null || url.length() == 0) {
      return defaultPrompt;
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
      return resultPrompt + "> ";
    }
  }

  protected AttributedStyle resolveStyle(String value) {
    return STYLE_RESOLVER.resolve(value);
  }

  protected String formatDateTime(char c) {
    Supplier<String> dateFormat = DATE_TIME_FORMATS.get(c);
    if (dateFormat != null) {
      return dateFormat.get();
    }
    return null;
  }

  private static String getFormattedDateTime(final String pattern) {
    final SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ROOT);
    return sdf.format(new Date());
  }
}

// End PromptHandler.java
