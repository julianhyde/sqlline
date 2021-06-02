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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class ConnectionConfigParser {
  public static final String GLOBAL_CONFIG_NAME = "global-conf";

  public static final File DEFAULT_CONNECTION_CONFIG_FILE =
          new File(SqlLineOpts.saveDir(), "configuration");
  private static final String DEFAULT_CONNECTION_CONFIG_LOCATION =
          DEFAULT_CONNECTION_CONFIG_FILE.getAbsolutePath();
  private static final String SEPARATOR = ":";
  private static final char COMMENT_START = '#';
  private final SqlLine sqlLine;
  private final Map<String, Properties> connections = new HashMap<>();

  ConnectionConfigParser(SqlLine sqlLine) {
    this.sqlLine = sqlLine;
  }

  Properties getConnectionProperties(String connectionName) {
    if (connections.isEmpty()) {
      final String connectionConfig = sqlLine.getOpts().getConnectionConfig();
      if (connectionConfig == null || connectionConfig.isEmpty()) {
        Path connectionConfigLocation = Paths
                .get(DEFAULT_CONNECTION_CONFIG_LOCATION);
        if (!Files.exists(connectionConfigLocation)
                || Files.isDirectory(connectionConfigLocation)) {
          return null;
        } else {
          readFromFile(connectionConfigLocation);
        }
      } else {
        readFromFile(Paths.get(connectionConfig));
      }

    }
    return connections.get(connectionName);
  }

  void resetConnectionProperties() {
    connections.clear();
  }

  private void readFromFile(Path path) {
    if (!Files.exists(path) || Files.isDirectory(path)) {
      sqlLine.error(sqlLine.loc("no-file", path.toAbsolutePath()));
      return;
    }
    int minOffset = Integer.MAX_VALUE;
    int offset;
    String connectionName = null;
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.isEmpty()) {
          continue;
        }
        String name;
        String value;
        offset = 0;
        while (Character.isWhitespace(line.charAt(offset))) {
          offset++;
        }
        if (line.charAt(offset) == COMMENT_START
            || line.charAt(offset) == ':' || !line.contains(SEPARATOR)) {
          // skip commented line, line started with ':',
          // line not containing SEPARATOR at all
          continue;
        }
        final int sepIndex = line.indexOf(SEPARATOR);
        name = line.substring(0, sepIndex).trim();
        if (minOffset >= offset) {
          connectionName = name;
          connections.put(connectionName, new Properties());
          minOffset = offset;
        } else {
          value = line.substring(sepIndex + SEPARATOR.length()).trim();
          if (!value.isEmpty() && value.charAt(0) == COMMENT_START) {
            value = "";
          }
          connections.get(connectionName).setProperty(name, value);
        }
      }
    } catch (Exception e) {
      sqlLine.error(e);
    }
  }
}

// ConnectionConfigParser.java
