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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import mockit.Mock;
import mockit.MockUp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Test cases for Completions.
 */
public class CompletionTest {
  private static final String DEV_NULL = "/dev/null";
  private static SqlLine sqlLine;
  private static ByteArrayOutputStream os;

  @BeforeAll
  public static void setUp() {
    // Required as DummyLineReader does not support keymap binding
    new MockUp<Candidate>() {
      @Mock public String suffix() {
        return null;
      }
    };

    System.setProperty(TerminalBuilder.PROP_DUMB,
        Boolean.TRUE.toString());

    sqlLine = new SqlLine();
    os = new ByteArrayOutputStream();
    sqlLine.getOpts().setPropertiesFile(DEV_NULL);

    SqlLine.Status status =
        begin(sqlLine, os, false,
            "-e", "!set maxwidth 80", "!set fastconnect true");
    assertEquals(status, SqlLine.Status.OK);
    sqlLine.runCommands(new DispatchCallback(),
        "!connect "
            + SqlLineArgsTest.ConnectionSpec.H2.url + " "
            + SqlLineArgsTest.ConnectionSpec.H2.username + " "
            + "\"\"");

    final String createScript = "CREATE SCHEMA TEST_SCHEMA_FIRST;\n"
        + "CREATE SCHEMA TEST_SCHEMA_SECOND;\n"
        + "CREATE SCHEMA \"TEST SCHEMA WITH SPACES\";\n"
        + "CREATE SCHEMA \"TEST \"\"SCHEMA \"\"WITH \"\"QUOTES\";\n"
        + "CREATE TABLE TEST_SCHEMA_FIRST.TABLE_FIRST(pk int, name varchar);\n"
        + "CREATE TABLE TEST_SCHEMA_FIRST.TABLE_SECOND(pk int, \"last name\" varchar);\n"
        + "CREATE TABLE TEST_SCHEMA_SECOND.\"TBL 1\"(pk int, desc varchar);\n"
        + "CREATE TABLE \"TEST \"\"SCHEMA \"\"WITH \"\"QUOTES\".\"QUOTES \"\"TBL 1\"(pk int, desc varchar);\n"
        + "CREATE TABLE TEST_SCHEMA_SECOND.\"TABLE 2  SECOND\"(pk int, \"field2\" varchar);\n"
        + "CREATE TABLE \"TEST SCHEMA WITH SPACES\".\"TABLE WITH SPACES\"(pk int, \"with spaces\" varchar);\n";
    sqlLine.runCommands(new DispatchCallback(), createScript);

    os.reset();
  }

  @BeforeEach
  public void setUpEach() {
    os.reset();
  }

  @AfterAll
  public static void tearDown() {
    System.setProperty(TerminalBuilder.PROP_DUMB,
        Boolean.FALSE.toString());
    sqlLine.setExit(true);
  }

  @ParameterizedTest(name = "Check completions like input + one symbol")
  @ValueSource(strings = {"!", "  \t\t  !", "\t \t!"})
  public void testCommandCompletions(String input) {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
    TreeSet<String> commandSet = getCommandSet(sqlLine);
    for (char c = 'a'; c <= 'z'; c++) {
      final Set<String> expectedSubSet =
          filterSet(commandSet, SqlLine.COMMAND_PREFIX + c);
      final List<Candidate> actualCandidates =
          getLineReaderCompletedList(lineReader, input + c);
      assertEquals(expectedSubSet.size(), actualCandidates.size());
      for (Candidate candidate : actualCandidates) {
        assertTrue(expectedSubSet.contains(candidate.value()));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"!quit", "!set", "!outputformat"})
  public void testCompletionAgainstCompletedCommands(String input) {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
    final List<Candidate> actual =
        getLineReaderCompletedList(lineReader, input);
    assertEquals(1, actual.size());
    assertEquals(input, actual.iterator().next().value());
  }

  @Test
  public void testNoCompletionsAfterResetCommandAndProperty() {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
    final String resetCommand = "!reset ";
    for (BuiltInProperty property : BuiltInProperty.values()) {
      final String command = resetCommand
          + property.propertyName().toLowerCase(Locale.ROOT);
      final List<Candidate> actual =
          getLineReaderCompletedList(lineReader, command + " ");
      assertEquals(1, actual.size());
      assertEquals(command, actual.iterator().next().value(),
          "Completion for command '"
              + resetCommand + property.propertyName() + "'");
    }
  }

  @ParameterizedTest(name = "Property with defined available values "
      + "should be autocompleted only with values from the defined ranges")
  @MethodSource("propertyCompletionProvider")
  public void testSetPropertyCompletions(String input, String expected) {
    final LineReaderCompletionImpl lineReader = getDummyLineReader();
    final Collection<Candidate> actual =
        getLineReaderCompletedList(lineReader, input);
    assertEquals(1, actual.size());
    assertEquals(expected, actual.iterator().next().value());
  }

  private static Stream<Arguments> propertyCompletionProvider() {
    return Stream.of(
        of("!set verbose tr", "!set verbose true"),
        of("!set verbose false", "!set verbose false"),
        of("!set mode v", "!set mode vi"),
        of("!set mode e", "!set mode emacs"),
        of("!set outputFormat js", "!set outputFormat json"),
        of("!set outputFormat xmlel", "!set outputFormat xmlelements"),
        of("!set colorScheme che", "!set colorScheme chester"),
        of("!set colorScheme sol", "!set colorScheme solarized"));
  }

  @ParameterizedTest
  @MethodSource("sqlKeywordCompletionProvider")
  public void testSqlCompletions(String input, String expected) {
    try {
      LineReader lineReader = sqlLine.getLineReader();
      LineReaderCompletionImpl lineReaderCompletion =
          new LineReaderCompletionImpl(lineReader.getTerminal());
      lineReaderCompletion.setCompleter(
          sqlLine.getDatabaseConnection().getSqlCompleter());
      final List<Candidate> actual =
          getLineReaderCompletedList(lineReaderCompletion, input);
      assertEquals(1, actual.size());
      assertEquals(expected, actual.iterator().next().value());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private static Stream<Arguments> sqlKeywordCompletionProvider() {
    return Stream.of(
        of("sel", "select"),
        of("FR", "FROM"),
        of("fr", "from"),
        of("wher", "where"),
        of("betwe", "between"));
  }

  @ParameterizedTest
  @MethodSource("schemaTableColumnProvider")
  public void testGetSchemaTableColumn(String input, List<String> expected) {
    final SqlCompleter sqlCompleter = new SqlCompleter(sqlLine, false);
    assertIterableEquals(expected, sqlCompleter.getSchemaTableColumn(input));
  }

  private static Stream<Arguments> schemaTableColumnProvider() {
    return Stream.of(
        of("SCHEMA", Collections.singletonList("SCHEMA")),
        of("SCHEMA.TABLE", Arrays.asList("SCHEMA", "TABLE")),
        of("TABLE.\"COL\"", Arrays.asList("TABLE", "\"COL\"")),
        of("SCHEMA.TABLE.\"COL\"", Arrays.asList("SCHEMA", "TABLE", "\"COL\"")),
        of("SCHEMA.\"TABLE\".COL", Arrays.asList("SCHEMA", "\"TABLE\"", "COL")),
        of("\"SCHEMA\".TABLE.COL", Arrays.asList("\"SCHEMA\"", "TABLE", "COL")),
        of("\"SCHEMA WITH SPACES\".TABLE.COL",
            Arrays.asList("\"SCHEMA WITH SPACES\"", "TABLE", "COL")),
        // wrong scenario
        of("SCHEMA WITH SPACES.TABLE.COL",
            Collections.singletonList("SCHEMA")));
  }

  @ParameterizedTest
  @MethodSource("dialectSpecificNameProvider")
  public void testReadAsDialectSpecificName(
      String expected, Dialect dialect, String input) {
    final SqlCompleter sqlCompleter = new SqlCompleter(sqlLine, false);
    assertEquals(expected,
        sqlCompleter.readAsDialectSpecificName(dialect, input));
  }

  private static Stream<Arguments> dialectSpecificNameProvider() {
    Dialect def = DialectImpl.getDefault();
    final Dialect storesLowerDialect = DialectImpl.create(
        def.DEFAULT_KEYWORD_SET, "\"\"", "storesLower", true, false, "");
    final Dialect storesUpperDialect = DialectImpl.create(
        def.DEFAULT_KEYWORD_SET, "[]", "storesUpper", false, true, "");
    return Stream.of(
        of("SCHEMA", BuiltInDialect.DEFAULT, "SCHEMA"),
        of("", BuiltInDialect.DEFAULT, ""),
        of("", BuiltInDialect.DEFAULT, "\"\""),
        of("ScHeMa", BuiltInDialect.DEFAULT, "\"ScHeMa\""),
        of("ScHeMa", BuiltInDialect.MYSQL, "`ScHeMa`"),
        of("", BuiltInDialect.MYSQL, "``"),
        of("schema with spaces", storesLowerDialect, "schema with spaces"),
        of("schema", storesLowerDialect, "SCHEMA"),
        of("SCHEMA", storesLowerDialect, "\"SCHEMA\""),
        of("ScHeMa", storesLowerDialect, "\"ScHeMa\""),
        of("SCHEMA", storesUpperDialect, "schema"),
        of("SCHEMA", storesUpperDialect, "schema"),
        of("SCHEMA", storesUpperDialect, "SCHEMA"),
        of("SCHEMA", storesUpperDialect, "[SCHEMA]"),
        of("ScHeMa", storesUpperDialect, "[ScHeMa]"),
        of("", storesUpperDialect, "[]"));
  }

  @ParameterizedTest
  @MethodSource("nameProvider")
  public void testWriteAsDialectSpecificValue(
      String expected, Dialect dialect, boolean forceQuote, String input) {
    assertEquals(expected,
        SqlCompleter.writeAsDialectSpecificValue(dialect, forceQuote, input));
  }

  private static Stream<Arguments> nameProvider() {
    Dialect dialect = DialectImpl.getDefault();
    final Dialect dialectLowWithExtra = DialectImpl.create(
        dialect.DEFAULT_KEYWORD_SET, "\"\"", "storesLower", true, false, "@#");
    final Dialect dialectUp = DialectImpl.create(
        dialect.DEFAULT_KEYWORD_SET, "[]", "storesUpper", false, true, "");
    return Stream.of(
        of("SCHEMA", BuiltInDialect.DEFAULT, false, "SCHEMA"),
        of("\"SCHEMA\"", BuiltInDialect.DEFAULT, true, "SCHEMA"),
        of("", BuiltInDialect.DEFAULT, false, ""),
        of("\"\"", BuiltInDialect.DEFAULT, true, ""),
        of("\"ScHeMA\"", BuiltInDialect.DEFAULT, true, "ScHeMA"),
        of("ScHeMA", BuiltInDialect.DEFAULT, false, "ScHeMA"),
        of("`SCHEMA`", BuiltInDialect.MYSQL, true, "SCHEMA"),
        of("``", BuiltInDialect.MYSQL, true, ""),
        of("\"schema with spaces\"",
            dialectLowWithExtra, true, "schema with spaces"),
        of("@schema#", dialectLowWithExtra, false, "@schema#"),
        of("[@SCHEMA#]", dialectUp, false, "@SCHEMA#"),
        of("\"ScHeMA\"", dialectLowWithExtra, false, "ScHeMA"),
        of("[schema]", dialectUp, false, "schema"),
        of("SCHEMA", dialectUp, false, "SCHEMA"),
        of("[SCHEMA]", dialectUp, true, "SCHEMA"),
        of("[ScHeMA]", dialectUp, true, "ScHeMA"),
        of("[]", dialectUp, true, ""));
  }

  @ParameterizedTest
  @MethodSource("stringsForSchemaTableColumnCompletions")
  public void testSchemaTableColumnCompletions(String expected, String input) {
    try {
      final LineReaderCompletionImpl lineReader = getDummyLineReader();
      lineReader.setCompleter(new SqlCompleter(sqlLine, false));
      // need for rehash as test data created after sql completer init
      sqlLine.runCommands(new DispatchCallback(), "!rehash");
      os.reset();

      final Collection<String> actual =
          getLineReaderCompletedList(lineReader, input).stream()
          .map(Candidate::value).collect(Collectors.toList());
      assertIterableEquals(Collections.singleton(expected), actual);
      assertEquals(1, actual.size());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private static Stream<Arguments> stringsForSchemaTableColumnCompletions() {
    return Stream.of(
        // Data to test with dialect open/close quotes ""
        // Schemas
        of("TEST_SCHEMA_FIRST", "TEST_SCHEMA_F"),
        of("TEST_SCHEMA_SECOND", "TEST_SCHEMA_S"),
        of("\"TEST_SCHEMA_FIRST\"", "\"TEST_SCHEMA_F"),
        of("\"TEST SCHEMA WITH SPACES\"", "\"TEST SCHEMA W"),
        of("\"TEST \"\"SCHEMA \"\"WITH \"\"QUOTES\"", "\"TEST \"\"SCHEMA \""),

        // Tables
        of("TABLE_FIRST", "TABLE_F"),
        of("TABLE_SECOND", "TABLE_S"),
        of("\"TBL 1\"", "\"TBL"),
        of("\"TABLE WITH SPACES\"", "\"TABLE WITH"),
        of("\"QUOTES \"\"TBL 1\"", "\"QUOTES"),

        // Schemas + Tables
        of("TEST_SCHEMA_FIRST.TABLE_FIRST", "TEST_SCHEMA_FIRST.TABLE_F"),
        of("TEST_SCHEMA_FIRST.TABLE_SECOND", "TEST_SCHEMA_FIRST.TABLE_S"),
        of("TEST_SCHEMA_FIRST.\"TABLE_SECOND\"", "TEST_SCHEMA_FIRST.\"TABLE_S"),
        of("TEST_SCHEMA_SECOND.\"TBL 1\"", "TEST_SCHEMA_SECOND.\"TBL"),
        of("\"TEST SCHEMA WITH SPACES\".\"TABLE WITH SPACES\"",
            "\"TEST SCHEMA WITH SPACES\".\"TABLE WI"),
        of("\"TEST \"\"SCHEMA \"\"WITH \"\"QUOTES\".\"QUOTES \"\"TBL 1\"",
            "\"TEST \"\"SCHEMA \"\"WITH \"\"QUOTES\".\"QUO"),

        // Tables + Columns
        of("TABLE_FIRST.PK", "TABLE_FIRST.P"),
        of("TABLE_FIRST.NAME", "TABLE_FIRST.N"),
        of("TABLE_SECOND.\"last name\"", "TABLE_SECOND.\"l"),
        of("\"TBL 1\".PK", "\"TBL 1\".P"),
        of("\"TABLE 2  SECOND\".\"field2\"", "\"TABLE 2  SECOND\".\"f"),

        // Schemas + Tables + Columns
        of("TEST_SCHEMA_FIRST.TABLE_FIRST.NAME",
            "TEST_SCHEMA_FIRST.TABLE_FIRST.N"),
        of("TEST_SCHEMA_FIRST.TABLE_SECOND.\"last name\"",
            "TEST_SCHEMA_FIRST.TABLE_SECOND.\"l"),
        of("TEST_SCHEMA_SECOND.\"TBL 1\".DESC",
            "TEST_SCHEMA_SECOND.\"TBL 1\".D"),
        of("TEST_SCHEMA_SECOND.\"TABLE 2  SECOND\".\"field2\"",
            "TEST_SCHEMA_SECOND.\"TABLE 2  SECOND\".\"f"),
        of("\"TEST SCHEMA WITH SPACES\".\"TABLE WITH SPACES\".PK",
            "\"TEST SCHEMA WITH SPACES\".\"TABLE WITH SPACES\".P"),
        of("\"TEST SCHEMA WITH SPACES\".\"TABLE WITH SPACES\".\"with spaces\"",
            "\"TEST SCHEMA WITH SPACES\".\"TABLE WITH SPACES\".\"w"));
  }

  @ParameterizedTest
  @MethodSource("stringsForSchemaTableColumnCompletionsForMySql")
  public void testSchemaTableColumnCompletionsForMySqlDialect(
      String expected, String input) {
    try {
      new MockUp<DialectImpl>() {
        @Mock public char getOpenQuote() {
          return '`';
        }

        @Mock public char getCloseQuote() {
          return '`';
        }
      };
      final LineReaderCompletionImpl lineReader = getDummyLineReader();
      lineReader.setCompleter(new SqlCompleter(sqlLine, false));
      // need for rehash as test data created after sql completer init
      sqlLine.runCommands(new DispatchCallback(), "!rehash");
      os.reset();

      final Collection<String> actual =
          getLineReaderCompletedList(lineReader, input).stream()
              .map(Candidate::value).collect(Collectors.toList());
      assertIterableEquals(Collections.singleton(expected), actual);
      assertEquals(1, actual.size());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private static Stream<Arguments>
      stringsForSchemaTableColumnCompletionsForMySql() {
    return Stream.of(
        // Data to test with dialect open/close quotes ``
        // Schemas
        of("TEST_SCHEMA_FIRST", "TEST_SCHEMA_F"),
        of("TEST_SCHEMA_SECOND", "TEST_SCHEMA_S"),
        of("`TEST_SCHEMA_FIRST`", "`TEST_SCHEMA_F"),
        of("`TEST SCHEMA WITH SPACES`", "`TEST SCHEMA W"),

        // Tables
        of("TABLE_FIRST", "TABLE_F"),
        of("TABLE_SECOND", "TABLE_S"),
        of("`TBL 1`", "`TBL"),
        of("`TABLE WITH SPACES`", "`TABLE WITH"),

        // Schemas + Tables
        of("TEST_SCHEMA_FIRST.TABLE_FIRST", "TEST_SCHEMA_FIRST.TABLE_F"),
        of("TEST_SCHEMA_FIRST.TABLE_SECOND", "TEST_SCHEMA_FIRST.TABLE_S"),
        of("TEST_SCHEMA_FIRST.`TABLE_SECOND`", "TEST_SCHEMA_FIRST.`TABLE_S"),
        of("TEST_SCHEMA_SECOND.`TBL 1`", "TEST_SCHEMA_SECOND.`TBL"),
        of("`TEST SCHEMA WITH SPACES`.`TABLE WITH SPACES`",
            "`TEST SCHEMA WITH SPACES`.`TABLE WI"),

        // Tables + Columns
        of("TABLE_FIRST.PK", "TABLE_FIRST.P"),
        of("TABLE_FIRST.NAME", "TABLE_FIRST.N"),
        of("TABLE_SECOND.`last name`", "TABLE_SECOND.`l"),
        of("`TBL 1`.PK", "`TBL 1`.P"),
        of("`TABLE 2  SECOND`.`field2`", "`TABLE 2  SECOND`.`f"),

        // Schemas + Tables + Columns
        of("TEST_SCHEMA_FIRST.TABLE_FIRST.NAME",
            "TEST_SCHEMA_FIRST.TABLE_FIRST.N"),
        of("TEST_SCHEMA_FIRST.TABLE_SECOND.`last name`",
            "TEST_SCHEMA_FIRST.TABLE_SECOND.`l"),
        of("TEST_SCHEMA_SECOND.`TBL 1`.DESC",
            "TEST_SCHEMA_SECOND.`TBL 1`.D"),
        of("TEST_SCHEMA_SECOND.`TABLE 2  SECOND`.`field2`",
            "TEST_SCHEMA_SECOND.`TABLE 2  SECOND`.`f"),
        of("`TEST SCHEMA WITH SPACES`.`TABLE WITH SPACES`.PK",
            "`TEST SCHEMA WITH SPACES`.`TABLE WITH SPACES`.P"),
        of("`TEST SCHEMA WITH SPACES`.`TABLE WITH SPACES`.`with spaces`",
            "`TEST SCHEMA WITH SPACES`.`TABLE WITH SPACES`.`w"));
  }

  @ParameterizedTest
  @MethodSource("stringsForSchemaTableColumnCompletionsForSquareBrackets")
  public void testSchemaTableColumnCompletionsForSquareBracketsDialect(
      String expected, String input) {
    try {
      new MockUp<DialectImpl>() {
        @Mock public char getOpenQuote() {
          return '[';
        }

        @Mock public char getCloseQuote() {
          return ']';
        }
      };
      final LineReaderCompletionImpl lineReader = getDummyLineReader();
      lineReader.setCompleter(new SqlCompleter(sqlLine, false));
      // need for rehash as test data created after sql completer init
      sqlLine.runCommands(new DispatchCallback(), "!rehash");
      os.reset();

      final Collection<String> actual =
          getLineReaderCompletedList(lineReader, input).stream()
              .map(Candidate::value).collect(Collectors.toList());
      assertIterableEquals(Collections.singleton(expected), actual);
      assertEquals(1, actual.size());
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private static Stream<Arguments>
      stringsForSchemaTableColumnCompletionsForSquareBrackets() {
    return Stream.of(
        // Data to test with dialect open/close quotes ``
        // Schemas
        of("TEST_SCHEMA_FIRST", "TEST_SCHEMA_F"),
        of("TEST_SCHEMA_SECOND", "TEST_SCHEMA_S"),
        of("[TEST_SCHEMA_FIRST]", "[TEST_SCHEMA_F"),
        of("[TEST SCHEMA WITH SPACES]", "[TEST SCHEMA W"),

        // Tables
        of("TABLE_FIRST", "TABLE_F"),
        of("TABLE_SECOND", "TABLE_S"),
        of("[TBL 1]", "[TBL"),
        of("[TABLE WITH SPACES]", "[TABLE WITH"),

        // Schemas + Tables
        of("TEST_SCHEMA_FIRST.TABLE_FIRST", "TEST_SCHEMA_FIRST.TABLE_F"),
        of("TEST_SCHEMA_FIRST.TABLE_SECOND", "TEST_SCHEMA_FIRST.TABLE_S"),
        of("TEST_SCHEMA_FIRST.[TABLE_SECOND]", "TEST_SCHEMA_FIRST.[TABLE_S"),
        of("TEST_SCHEMA_SECOND.[TBL 1]", "TEST_SCHEMA_SECOND.[TBL"),
        of("[TEST SCHEMA WITH SPACES].[TABLE WITH SPACES]",
            "[TEST SCHEMA WITH SPACES].[TABLE WI"),

        // Tables + Columns
        of("TABLE_FIRST.PK", "TABLE_FIRST.P"),
        of("TABLE_FIRST.NAME", "TABLE_FIRST.N"),
        of("TABLE_SECOND.[last name]", "TABLE_SECOND.[l"),
        of("[TBL 1].PK", "[TBL 1].P"),
        of("[TABLE 2  SECOND].[field2]", "[TABLE 2  SECOND].[f"),

        // Schemas + Tables + Columns
        of("TEST_SCHEMA_FIRST.TABLE_FIRST.NAME",
            "TEST_SCHEMA_FIRST.TABLE_FIRST.N"),
        of("TEST_SCHEMA_FIRST.TABLE_SECOND.[last name]",
            "TEST_SCHEMA_FIRST.TABLE_SECOND.[l"),
        of("TEST_SCHEMA_SECOND.[TBL 1].DESC",
            "TEST_SCHEMA_SECOND.[TBL 1].D"),
        of("TEST_SCHEMA_SECOND.[TABLE 2  SECOND].[field2]",
            "TEST_SCHEMA_SECOND.[TABLE 2  SECOND].[f"),
        of("[TEST SCHEMA WITH SPACES].[TABLE WITH SPACES].PK",
            "[TEST SCHEMA WITH SPACES].[TABLE WITH SPACES].P"),
        of("[TEST SCHEMA WITH SPACES].[TABLE WITH SPACES].[with spaces]",
            "[TEST SCHEMA WITH SPACES].[TABLE WITH SPACES].[w"));
  }

  @ParameterizedTest
  @MethodSource("noSchemaSqlCompletionsWithFastConnect")
  public void testNoSchemaSqlCompletionsWithFastConnect(
      String expected, String input) {
    try {
      final LineReaderCompletionImpl lineReader = getDummyLineReader();
      lineReader.setCompleter(new SqlCompleter(sqlLine, true));
      // need for rehash as test data created after sql completer init
      sqlLine.runCommands(new DispatchCallback(), "!set fastconnect true");
      sqlLine.runCommands(new DispatchCallback(), "!rehash");
      os.reset();

      final Collection<String> actual =
          getLineReaderCompletedList(lineReader, input).stream()
              .map(Candidate::value).collect(Collectors.toList());
      assertFalse(actual.stream().allMatch(t -> t.startsWith(expected)));
      sqlLine.runCommands(new DispatchCallback(), "!set fastconnect false");
      sqlLine.runCommands(new DispatchCallback(), "!rehash");
    } catch (Exception e) {
      // fail
      throw new RuntimeException(e);
    }
  }

  private static Stream<Arguments> noSchemaSqlCompletionsWithFastConnect() {
    return Stream.of(
        of("TEST_SCHEMA_FIRST", "TEST_SCHEMA_F"),
        of("TABLE_FIRST", "TABLE_F"));
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

  private List<Candidate> getLineReaderCompletedList(
      LineReaderCompletionImpl lineReader, String s) {
    return lineReader.complete(s);
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
  private static class LineReaderCompletionImpl extends LineReaderImpl {
    List<Candidate> list;

    LineReaderCompletionImpl(Terminal terminal) throws IOException {
      super(terminal);
      parser = new SqlLineParser(sqlLine);
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
