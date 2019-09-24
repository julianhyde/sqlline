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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Output file.
 */
public class OutputFile {
  final File file;
  final PrintWriter out;

  public OutputFile(String filename) throws IOException {
    file = new File(filename);
    out = new PrintWriter(
        new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8), true);
  }

  @Override public String toString() {
    return file.getAbsolutePath();
  }

  public void addLine(String command) {
    out.println(command);
  }

  public void print(String command) {
    out.print(command);
  }

  public void close() throws IOException {
    out.close();
  }
}

// End OutputFile.java
