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
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Test cases for Completions.
 */
public class CompletionTest {
  private final SqlLine sqlLine = new SqlLine();

  @Test
  public void testCommandCompletions() throws IOException {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();

    TreeSet<String> commandSet = getCommandSet(sqlLine);

    // check completions of ! + one symbol
    for (char c = 'a'; c <= 'z'; c++) {
      final Set<String> expectedSubSet = filterSet(commandSet, "!" + c);
      final Set<String> actual = getLineReaderCompletedSet(lineReader, "!" + c);
      assertEquals("Completion for command !" + c, expectedSubSet, actual);
    }

    // check completions if the whole command is finished
<<<<<<< HEAD
    final Set<String> expectedQuit = filterSet(commandSet, "!quit");
    final Set<String> actualQuit =
        getLineReaderCompletedSet(lineReader, "!quit");
    assertEquals(expectedQuit, actualQuit);
=======
    final Set<String> expected2 = filterSet(commandSet, "!quit");
    final Set<String> actual2 = getLineReaderCompletedSet(lineReader, "!quit");
    assertEquals(expected2, actual2);
>>>>>>> [SQLLINE-186] Added tests for completions. Added possibility to do compmletion for properties values like outputformat or boolean properties
  }

  @Test
  public void testPropertyCompletions() throws IOException {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
<<<<<<< HEAD
    final Set<String> actualVerbose =
        getLineReaderCompletedSet(lineReader, "!set verbose tr");
    assertEquals(1, actualVerbose.size());
    assertEquals("!set verbose true", actualVerbose.iterator().next());

    final Set<String> actualJsonFormat =
        getLineReaderCompletedSet(lineReader, "!set outputFormat js");
    assertEquals(1, actualJsonFormat.size());
    assertEquals("!set outputFormat json", actualJsonFormat.iterator().next());

    final Set<String> actualXmlElementFormat =
        getLineReaderCompletedSet(lineReader, "!set outputFormat xmlel");
    assertEquals(1, actualXmlElementFormat.size());
    assertEquals("!set outputFormat xmlelements",
        actualXmlElementFormat.iterator().next());
=======
    final Set<String> actual =
        getLineReaderCompletedSet(lineReader, "!set verbose tr");
    assertEquals("!set verbose true", actual.iterator().next());

    final Set<String> actual1 =
        getLineReaderCompletedSet(lineReader, "!set outputFormat js");
    assertEquals("!set outputFormat json", actual1.iterator().next());

    final Set<String> actual2 =
        getLineReaderCompletedSet(lineReader, "!set outputFormat xmlel");
    assertEquals("!set outputFormat xmlelements", actual2.iterator().next());
>>>>>>> [SQLLINE-186] Added tests for completions. Added possibility to do compmletion for properties values like outputformat or boolean properties
  }

  @Test
  public void testSqlCompletions() throws IOException {
    SqlLine sqlLine = new SqlLine();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    SqlLine.Status status = begin(sqlLine, os, false);
    // Here the status is SqlLine.Status.OTHER
    // because of EOF as the result of InputStream which
    // is not used in the current test so it is ok
    assertThat(status, equalTo(SqlLine.Status.OTHER));
    DispatchCallback dc = new DispatchCallback();
    sqlLine.runCommands(Collections.singletonList("!set maxwidth 80"), dc);
    sqlLine.runCommands(
        Collections.singletonList("!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " "
            + "\"\""), dc);
    os.reset();

    LineReader lineReader = sqlLine.getLineReader();

    LineReaderCompletionImpl lineReaderCompletion =
        new LineReaderCompletionImpl(lineReader.getTerminal());
    lineReaderCompletion
        .setCompleter(((LineReaderImpl) lineReader).getCompleter());
<<<<<<< HEAD
    final Set<String> actualSqlLowerCase =
        getLineReaderCompletedSet(lineReaderCompletion, "sel");
    assertEquals(1, actualSqlLowerCase.size());
    assertEquals("select", actualSqlLowerCase.iterator().next());

    final Set<String> actualSqlUpperCase =
        getLineReaderCompletedSet(lineReaderCompletion, "SEL");
    assertEquals(1, actualSqlUpperCase.size());
    assertEquals("SELECT", actualSqlUpperCase.iterator().next());
=======
    final Set<String> actual =
        getLineReaderCompletedSet(lineReaderCompletion, "sel");
    assertEquals("select", actual.iterator().next());

    final Set<String> actual2 =
        getLineReaderCompletedSet(lineReaderCompletion, "SEL");
    assertEquals("SELECT", actual2.iterator().next());
>>>>>>> [SQLLINE-186] Added tests for completions. Added possibility to do compmletion for properties values like outputformat or boolean properties
  }

  private LineReaderCompletionImpl getDummyLineReader() throws IOException {
    TerminalBuilder terminalBuilder = TerminalBuilder.builder();
    final Terminal terminal = terminalBuilder.build();

    final LineReaderCompletionImpl lineReader =
        new LineReaderCompletionImpl(terminal);
    lineReader.setCompleter(sqlLine.getCommandCompleter());
    lineReader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);
    return lineReader;
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

<<<<<<< HEAD
  private Set<String> filterSet(TreeSet<String> commandSet, String filter) {
    Set<String> subset = commandSet.stream()
        .filter(s -> s.startsWith(filter))
        .collect(Collectors.toCollection(TreeSet::new));
    if (subset.isEmpty()) {
      Set<String> result = new TreeSet<>(commandSet);
      result.add(filter);
      return result;
    } else {
      return subset;
    }
=======
  private Set<String> filterSet(TreeSet<String> commandSet, String s2) {
    Set<String> subset = commandSet.stream()
        .filter(s -> s.startsWith(s2))
        .collect(Collectors.toCollection(TreeSet::new));
    return subset.isEmpty()
        ? new TreeSet<String>(commandSet) {{
            add(s2);
          }}
        : subset;
>>>>>>> [SQLLINE-186] Added tests for completions. Added possibility to do compmletion for properties values like outputformat or boolean properties
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
