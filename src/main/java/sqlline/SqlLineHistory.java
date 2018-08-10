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

import java.nio.file.Path;

import org.jline.reader.impl.history.DefaultHistory;

/**
 * SqlLineHistory.
 */
public class SqlLineHistory extends DefaultHistory {
  @Override
  protected void addHistoryLine(Path path, String line) {
    // logic to support jline2 based sqlline history
    if (!line.isEmpty()
        && (!line.contains(":")
            || !line.substring(0, line.indexOf(":")).matches("\\d+"))) {
      line = System.currentTimeMillis() + ":" + line.replaceAll("\\\\", "\\\\");
    }
    super.addHistoryLine(path, line);
  }
}

// SqlLineHistory.java
