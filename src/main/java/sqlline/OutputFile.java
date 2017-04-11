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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Output file.
 */
public class OutputFile {
  final File file;
  final PrintWriter out;

  public OutputFile(String filename) throws IOException {
    filename = expand(filename);
    file = new File(filename);
    out = new PrintWriter(new FileWriter(file), true);
  }

  /** Expands "~" to the home directory. */
  private static String expand(String filename) {
    if (filename.startsWith("~" + File.separator)) {
      try {
        String home = System.getProperty("user.home");
        if (home != null) {
          return home + filename.substring(1);
        }
      } catch (SecurityException e) {
        // ignore
      }
    }
    return filename;
  }

  @Override public String toString() {
    return file.getAbsolutePath();
  }

  public void addLine(String command) {
    out.println(command);
    out.flush();
  }

  public void print(String command) {
    out.print(command);
    out.flush();
  }

  public void close() throws IOException {
    out.close();
  }
}

// End OutputFile.java
