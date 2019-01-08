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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import org.jline.reader.Expander;
import org.jline.reader.History;

/**
 * SQLLine expander class for live templates.
 */
public class SqlLineExpander implements Expander {
  private final SqlLine sqlLine;
  private Properties expandProperties = null;

  public SqlLineExpander(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  @Override public String expandHistory(History history, String line) {
    return line;
  }

  @Override public String expandVar(String word) {
    if (expandProperties != null) {
      final String expandValue = (String) expandProperties.get(word);
      if (expandValue != null) {
        return expandValue;
      }
    }
    return word;
  }

  public void reset() {
    expandProperties = null;
    final String liveTemplatesFile = sqlLine.getOpts().getLiveTemplatesFile();
    if (Objects.equals(liveTemplatesFile,
        BuiltInProperty.LIVE_TEMPLATES.defaultValue())) {
      return;
    }
    final File path = new File(liveTemplatesFile);
    if (!path.exists() || !path.isFile()) {
      sqlLine.error(sqlLine.loc("no-file", path.getAbsolutePath()));
      return;
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(path), StandardCharsets.UTF_8))) {
      expandProperties = new Properties();
      expandProperties.load(reader);
    } catch (IOException e) {
      sqlLine.error(e);
    }
  }
}

// End SqlLineExpander.java
