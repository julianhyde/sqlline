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

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import sqlline.ConnectionMetadata;
import sqlline.PromptHandler;
import sqlline.SqlLine;

/**
 * Sub-class of {@link PromptHandler} that is used to test custom
 * prompt handling configuration.
 */
public class CustomPromptHandler extends PromptHandler {

  public CustomPromptHandler(SqlLine sqlLine) {
    super(sqlLine);
  }

  @Override protected AttributedString getDefaultPrompt(int connectionIndex,
      String url, String defaultPrompt) {
    AttributedStringBuilder builder = new AttributedStringBuilder();
    builder.append("my_app");

    final ConnectionMetadata meta = sqlLine.getConnectionMetadata();

    final String currentSchema = meta.getCurrentSchema();
    if (currentSchema != null) {
      builder.append(" (").append(currentSchema).append(")");
    }
    return builder.append(">").toAttributedString();
  }
}

// End CustomPromptHandler.java
