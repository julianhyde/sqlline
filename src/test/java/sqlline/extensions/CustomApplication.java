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
package sqlline.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jline.reader.impl.completer.StringsCompleter;

import sqlline.Application;
import sqlline.BuiltInProperty;
import sqlline.CommandHandler;
import sqlline.OutputFormat;
import sqlline.ReflectiveCommandHandler;
import sqlline.SqlLine;
import sqlline.SqlLineOpts;

/**
 * Sub-class of {@link Application} that is used to test custom
 * application configuration.
 *
 * <p>Overrides information message, output formats, commands,
 * connection url examples, session options.
 */
public class CustomApplication extends Application {

  public static final String CUSTOM_INFO_MESSAGE = "my_custom_info_message";

  @Override public String getInfoMessage() {
    return CUSTOM_INFO_MESSAGE;
  }

  @Override public Map<String, OutputFormat> getOutputFormats(SqlLine sqlLine) {
    final Map<String, OutputFormat> outputFormats =
        new HashMap<>(sqlLine.getOutputFormats());
    outputFormats.remove("json");
    return outputFormats;
  }

  @Override public Collection<CommandHandler> getCommandHandlers(
      SqlLine sqlLine) {
    final List<CommandHandler> commandHandlers =
        new ArrayList<>(sqlLine.getCommandHandlers());
    final Iterator<CommandHandler> iterator =
        commandHandlers.iterator();
    while (iterator.hasNext()) {
      CommandHandler next = iterator.next();
      List<String> names = next.getNames();
      if (names.contains("tables")
          || names.contains("connect")
          || names.contains("outputformat")) {
        iterator.remove();
      }
    }

    commandHandlers.add(
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(getConnectionUrlExamples()), "connect",
            "open"));

    commandHandlers.add(
        new ReflectiveCommandHandler(sqlLine,
            new StringsCompleter(getOutputFormats(sqlLine).keySet()),
            "outputformat"));

    return commandHandlers;
  }

  @Override public Collection<String> getConnectionUrlExamples() {
    return Collections.singletonList("my_custom_url_connection_example");
  }

  @Override public SqlLineOpts getOpts(SqlLine sqlLine) {
    SqlLineOpts opts = sqlLine.getOpts();
    opts.set(BuiltInProperty.NULL_VALUE, "custom_null");
    return opts;
  }
}

// End CustomApplication.java
