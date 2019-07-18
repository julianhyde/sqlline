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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for Completions.
 */
public class CompletionTest {
  private final SqlLine sqlLine = new SqlLine();

  @BeforeEach
  public void setUp() {
    System.setProperty(TerminalBuilder.PROP_DUMB,
        Boolean.TRUE.toString());
  }

  @AfterEach
  public void tearDown() {
    System.setProperty(TerminalBuilder.PROP_DUMB,
        Boolean.FALSE.toString());
  }

  @Test
  public void testCommandCompletions() {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();

    TreeSet<String> commandSet = getCommandSet(sqlLine);

    // check completions of ! + one symbol
    for (char c = 'a'; c <= 'z'; c++) {
      final Set<String> expectedSubSet = filterSet(commandSet, "!" + c);
      final Set<String> actual = getLineReaderCompletedSet(lineReader, "!" + c);
      assertEquals(expectedSubSet, actual, "Completion for command !" + c);
    }

    // check completions of space + tabs + ! + one symbol
    for (char c = 'a'; c <= 'z'; c++) {
      final Set<String> expectedSubSet = filterSet(commandSet, "!" + c);
      final Set<String> actual =
          getLineReaderCompletedSet(lineReader, "  \t\t  !" + c);
      assertEquals(expectedSubSet, actual, "Completion for command !" + c);
    }

    // check completions if the whole command is finished
    final Set<String> quitExpected = filterSet(commandSet, "!quit");
    final Set<String> quitActual =
        getLineReaderCompletedSet(lineReader, "!quit");
    assertEquals(quitExpected, quitActual);
  }

  @Test
  public void testResetPropertyCompletions() {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
    final String resetCommand = "!reset ";

    for (BuiltInProperty property: BuiltInProperty.values()) {
      final String command = resetCommand
          + property.propertyName().toLowerCase(Locale.ROOT);
      final Set<String> actual =
          getLineReaderCompletedSet(lineReader, command + " ");
      assertEquals(Collections.singleton(command), actual,
          "Completion for command '"
              + resetCommand + property.propertyName() + "'");
    }
  }

  @Test
  public void testSetPropertyCompletions() {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
    final Set<String> actual =
        getLineReaderCompletedSet(lineReader, "!set verbose tr");
    assertEquals("!set verbose true", actual.iterator().next());

    final Set<String> viModeActual =
        getLineReaderCompletedSet(lineReader, "!set mode v");
    assertEquals(1, viModeActual.size());
    assertEquals("!set mode vi", viModeActual.iterator().next());

    final Set<String> emacsModeActual =
        getLineReaderCompletedSet(lineReader, "!set mode e");
    assertEquals(1, emacsModeActual.size());
    assertEquals("!set mode emacs", emacsModeActual.iterator().next());

    final Set<String> jsonActual =
        getLineReaderCompletedSet(lineReader, "!set outputFormat js");
    assertEquals(1, jsonActual.size());
    assertEquals("!set outputFormat json", jsonActual.iterator().next());

    final Set<String> xmlelActual =
        getLineReaderCompletedSet(lineReader, "!set outputFormat xmlel");
    assertEquals(1, xmlelActual.size());
    assertEquals("!set outputFormat xmlelements",
        xmlelActual.iterator().next());

    final Set<String> chesterActual =
        getLineReaderCompletedSet(lineReader, "!set colorScheme che");
    assertEquals(1, chesterActual.size());
    assertEquals("!set colorScheme chester", chesterActual.iterator().next());

    final Set<String> solarizedActual =
        getLineReaderCompletedSet(lineReader, "!set colorScheme sol");
    assertEquals(1, solarizedActual.size());
    assertEquals("!set colorScheme solarized",
        solarizedActual.iterator().next());
  }

  @Test
  public void testSqlCompletions() {
    try {
      SqlLine sqlLine = new SqlLine();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertEquals(status, SqlLine.Status.OK);
      sqlLine.runCommands(new DispatchCallback(),
          "!set maxwidth 80",
          "!connect "
              + SqlLineArgsTest.ConnectionSpec.H2.url + " "
              + SqlLineArgsTest.ConnectionSpec.H2.username + " "
              + "\"\"");
      os.reset();

      LineReader lineReader = sqlLine.getLineReader();

      LineReaderCompletionImpl lineReaderCompletion =
          new LineReaderCompletionImpl(lineReader.getTerminal());
      lineReaderCompletion.setCompleter(new SqlLineCompleter(sqlLine));
      final Set<String> lowCaseActual =
          getLineReaderCompletedSet(lineReaderCompletion, "sel");
      assertEquals(1, lowCaseActual.size());
      assertEquals("select", lowCaseActual.iterator().next());

      final Set<String> upperCaseActual =
          getLineReaderCompletedSet(lineReaderCompletion, "SEL");
      assertEquals(1, upperCaseActual.size());
      assertEquals("SELECT", upperCaseActual.iterator().next());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSqlCompletionsWithoutFastConnect() {
    try {
      SqlLine sqlLine = new SqlLine();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertEquals(status, SqlLine.Status.OK);
      sqlLine.runCommands(new DispatchCallback(),
          "!set maxwidth 80",
          "!set fastconnect false",
          "!connect "
              + SqlLineArgsTest.ConnectionSpec.H2.url + " "
              + SqlLineArgsTest.ConnectionSpec.H2.username + " "
              + "\"\"");
      os.reset();

      LineReader lineReader = sqlLine.getLineReader();

      LineReaderCompletionImpl lineReaderCompletion =
          new LineReaderCompletionImpl(lineReader.getTerminal());
      lineReaderCompletion.setCompleter(new SqlLineCompleter(sqlLine));
      final String tableStartName = "FUNCTION_ALIASE";
      final String tableStartNameExpected = "FUNCTION_ALIASES";
      final Set<String> tableAndColumns =
          getLineReaderCompletedSet(lineReaderCompletion, tableStartName);
      assertFalse(tableAndColumns.isEmpty());
      assertTrue(tableAndColumns.stream()
          .allMatch(t -> t.startsWith(tableStartNameExpected)));
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSqlCompletionsWithAndWithoutFastConnect() {
    try {
      SqlLine sqlLine = new SqlLine();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      SqlLine.Status status =
          begin(sqlLine, os, false, "-e", "!set maxwidth 80");
      assertEquals(status, SqlLine.Status.OK);
      sqlLine.runCommands(new DispatchCallback(),
          "!set maxwidth 80",
          "!set fastconnect true",
          "!connect "
              + SqlLineArgsTest.ConnectionSpec.H2.url + " "
              + SqlLineArgsTest.ConnectionSpec.H2.username + " "
              + "\"\"");
      os.reset();

      LineReader lineReader = sqlLine.getLineReader();

      LineReaderCompletionImpl lineReaderCompletion =
          new LineReaderCompletionImpl(lineReader.getTerminal());
      lineReaderCompletion.setCompleter(new SqlLineCompleter(sqlLine));
      final String tableStartNameInput = "FUNCTION_ALIASE";
      final String tableStartNameExpected = "FUNCTION_ALIASES";
      final Set<String> tableAndColumns =
          getLineReaderCompletedSet(lineReaderCompletion, tableStartNameInput);
      assertFalse(tableAndColumns.stream()
          .allMatch(t -> t.startsWith(tableStartNameExpected)));

      sqlLine.runCommands(new DispatchCallback(),
          "!set fastconnect false",
          "!reconnect");
      os.reset();
      final Set<String> tableAndColumnsAfterReconnect =
          getLineReaderCompletedSet(lineReaderCompletion, tableStartNameInput);
      assertTrue(tableAndColumnsAfterReconnect.stream()
          .allMatch(t -> t.startsWith(tableStartNameExpected)));
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private LineReaderCompletionImpl getDummyLineReader() {
    try {
      TerminalBuilder terminalBuilder = TerminalBuilder.builder();
      final Terminal terminal = terminalBuilder.build();

      final LineReaderCompletionImpl lineReader =
          new LineReaderCompletionImpl(terminal);
      lineReader.setCompleter(sqlLine.getCommandCompleter());
      lineReader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);
      return lineReader;
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private TreeSet<String> getCommandSet(SqlLine sqlLine) {
    TreeSet<String> commandSet = new TreeSet<>();
    for (CommandHandler ch: sqlLine.getCommandHandlers()) {
      for (String name: ch.getNames()) {
        commandSet.add("!" + name);
      }
      commandSet.add("!" + ch.getName());
    }
    return commandSet;
  }

  private Set<String> getLineReaderCompletedSet(
      LineReaderCompletionImpl lineReader, String s) {
    return lineReader.complete(s).stream()
        .map(Candidate::value).collect(Collectors.toCollection(TreeSet::new));
  }

  private Set<String> filterSet(TreeSet<String> commandSet, String s2) {
    Set<String> subset = commandSet.stream()
        .filter(s -> s.startsWith(s2))
        .collect(Collectors.toCollection(TreeSet::new));
    if (subset.isEmpty()) {
      Set<String> result = new TreeSet<>(commandSet);
      result.add(s2);
      return result;
    } else {
      return subset;
    }
  }

  /** LineReaderImpl extension to test completion*/
  private class LineReaderCompletionImpl extends LineReaderImpl {
    List<Candidate> list;

    LineReaderCompletionImpl(Terminal terminal) throws IOException {
      super(terminal);
    }

    List<Candidate> complete(final String line) {
      buf.write(line);
      buf.cursor(line.length());
      doComplete(CompletionType.Complete, true, true);
      final String buffer = buf.toString();
      buf.clear();
      return list == null || buffer.length() > line.length()
          ? Collections.singletonList(new Candidate(buffer.trim()))
          : list;
    }

    protected boolean doMenu(
        List<Candidate> original,
        String completed,
        BiFunction<CharSequence, Boolean, CharSequence> escaper) {
      list = original;
      return true;
    }
  }

  private static SqlLine.Status begin(
      SqlLine sqlLine, OutputStream os, boolean saveHistory, String... args) {
    try {
      PrintStream beelineOutputStream = getPrintStream(os);
      sqlLine.setOutputStream(beelineOutputStream);
      sqlLine.setErrorStream(beelineOutputStream);
      final InputStream is = new ByteArrayInputStream(new byte[0]);
      return sqlLine.begin(args, is, saveHistory);
    } catch (Throwable t) {
      // fail
      throw new RuntimeException(t);
    }
  }

  private static PrintStream getPrintStream(OutputStream os) {
    try {
      return new PrintStream(os, false, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // fail
      throw new RuntimeException(e);
    }
  }
}

// End CompletionTest.java
