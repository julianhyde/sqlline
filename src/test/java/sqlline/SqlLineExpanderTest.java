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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;
import static sqlline.SqlLineArgsTest.begin;

/**
 * Test cases for expander.
 */
public class SqlLineExpanderTest {
  private static SqlLine sqlLine;

  @BeforeAll
  public static void setUp() throws IOException {
    System.setProperty(TerminalBuilder.PROP_DUMB, Boolean.TRUE.toString());

    sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    File liveTemplatesFile = Files.createTempFile("expander_", "").toFile();
    try (BufferedWriter bw =
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(liveTemplatesFile),
                StandardCharsets.UTF_8))) {
      StringJoiner joiner = new StringJoiner("\n");
      expansionMap().forEach((key, value) ->
          joiner.add(key + ": " + value.replaceAll("\n", "\\\\n\\\\")));
      final String fileContent = joiner.toString();
      bw.write(fileContent);
      bw.flush();
    }
    begin(sqlLine, os, false, "-e", "!set maxcolumnwidth 80");
    ((LineReaderImpl) sqlLine.getLineReader())
        .setExpander(new SqlLineExpander(sqlLine));
    sqlLine.getOpts().setLiveTemplatesFile(liveTemplatesFile.getAbsolutePath());
    os.reset();
  }

  private static Map<String, String> expansionMap() {
    Map<String, String> map = new HashMap<>();
    map.put("epf", "explain plan for ");
    map.put("ctas", "create table as select ");
    map.put("mepf", "explain \n plan \n for ");
    return map;
  }

  @ParameterizedTest
  @MethodSource("expandedValuesProvider")
  public void testV(String liveTemplate, String expected) {
    assertEquals(expected,
        sqlLine.getLineReader().getExpander().expandVar(liveTemplate));
  }

  protected static Stream<Arguments> expandedValuesProvider() {
    return Stream.of(
        expansionMap().entrySet().stream()
            .map(t -> of(t.getKey(), t.getValue())).toArray(Arguments[]::new)
    );
  }
}

// End SqlLineExpanderTest.java
