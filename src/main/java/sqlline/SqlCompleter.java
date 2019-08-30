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

import java.sql.DatabaseMetaData;
import java.util.*;
import java.util.stream.Collectors;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.completer.StringsCompleter;

/**
 * Suggests completions for SQL statements.
 */
class SqlCompleter extends StringsCompleter {
  private static final String ALLOWED_UPPER_CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
  private static final String ALLOWED_LOWER_CHARACTERS =
      "abcdefghijklmnopqrstuvwxyz0123456789_";
  private final SqlLine sqlLine;
  private final boolean skipMeta;

  SqlCompleter(SqlLine sqlLine, boolean skipMeta) {
    super(getCompletions(sqlLine, skipMeta));
    this.sqlLine = sqlLine;
    this.skipMeta = skipMeta;
  }

  private static Candidate[] getCompletions(SqlLine sqlLine, boolean skipMeta) {
    Set<Candidate> completions = new TreeSet<>();

    // now add the keywords from the current connection
    final DatabaseMetaData meta = sqlLine.getDatabaseConnection().meta;
    try {
      for (String sqlKeyWord: meta.getSQLKeywords().split(",")) {
        final String keyWord = sqlKeyWord.trim();
        completions.add(
            new SqlLineCommandCompleter.SqlLineCandidate(sqlLine, keyWord,
                keyWord, null, sqlLine.loc("keyword"), null, null, true));
      }
    } catch (Throwable t) {
      // ignore
    }
    try {
      for (String numericFunction: meta.getNumericFunctions().split(",")) {
        final String function = numericFunction.trim();
        completions.add(
            new SqlLineCommandCompleter.SqlLineCandidate(sqlLine, function,
                function, null, sqlLine.loc("function"), null, null, false));
      }
    } catch (Throwable t) {
      // ignore
    }
    try {
      for (String stringFunction: meta.getStringFunctions().split(",")) {
        final String function = stringFunction.trim();
        completions.add(
            new SqlLineCommandCompleter.SqlLineCandidate(sqlLine, function,
                function, null, sqlLine.loc("function"), null, null, false));
      }
    } catch (Throwable t) {
      // ignore
    }
    try {
      for (String systemFunction: meta.getSystemFunctions().split(",")) {
        final String function = systemFunction.trim();
        completions.add(
            new SqlLineCommandCompleter.SqlLineCandidate(sqlLine, function,
                function, null, sqlLine.loc("function"), null, null, false));
      }
    } catch (Throwable t) {
      // ignore
    }
    try {
      for (String timeDateFunction: meta.getTimeDateFunctions().split(",")) {
        final String function = timeDateFunction.trim();
        completions.add(
            new SqlLineCommandCompleter.SqlLineCandidate(sqlLine, function,
                function, null, sqlLine.loc("function"), null, null, false));
      }
    } catch (Throwable t) {
      // ignore
    }

    if (!skipMeta) {
      try {
        final Dialect dialect = sqlLine.getDialect();
        Map<String, Map<String, Set<String>>> schema2tables =
            sqlLine.getDatabaseConnection()
                .getSchema(true).getSchema2tables();
        for (String schemaName : schema2tables.keySet()) {
          // mariadb/mysql case of connection without specific db like
          // under user without grants to read any db
          if (schemaName == null) {
            continue;
          }
          String value =
              writeAsDialectSpecificValue(dialect, false, schemaName);
          completions.add(
              generateCandidate(schemaName, value, sqlLine, "schema", false));
        }

        for (String tableName : schema2tables.values().stream()
            .flatMap(t -> t.keySet().stream()).collect(Collectors.toSet())) {
          String value = writeAsDialectSpecificValue(dialect, false, tableName);
          completions.add(
              generateCandidate(tableName, value, sqlLine, "table", false));
        }
      } catch (Throwable t) {
        // ignore
      }
    }
    for (String keyWord : Dialect.DEFAULT_KEYWORD_SET) {
      completions.add(
          generateCandidate(keyWord, keyWord, sqlLine, "keyword", true));
    }
    // set the Strings that will be completed
    return completions.toArray(new Candidate[0]);
  }

  @Override public void complete(
      LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
    String sql = commandLine.line().substring(0, commandLine.cursor());
    SqlLineParser.SqlLineArgumentList argumentList =
        ((SqlLineParser) sqlLine.getLineReader().getParser())
            .parseState(sql, sql.length(), Parser.ParseContext.UNSPECIFIED);
    final String supplierMsg = argumentList.getSupplier().get();
    final char openQuote = sqlLine.getDialect().getOpenQuote();
    if (argumentList.getState()
        == SqlLineParser.SqlParserState.MULTILINE_COMMENT
        || (argumentList.getState() == SqlLineParser.SqlParserState.QUOTED
        && ((openQuote == '"' && !supplierMsg.endsWith("dquote"))
        || (openQuote == '`' && !supplierMsg.endsWith("`"))))) {
      return;
    }

    if (!skipMeta) {
      Deque<String> lastWords = getSchemaTableColumn(argumentList.word());
      candidates.addAll(getSchemaBasedCandidates(new ArrayDeque<>(lastWords)));
      candidates.addAll(getTableBasedCandidates(new ArrayDeque<>(lastWords)));
    }
    // suggest other candidates if not quoted
    // and previous word not finished with '.'
    if (argumentList.getState() != SqlLineParser.SqlParserState.QUOTED
        && ((argumentList.getState()
            != SqlLineParser.SqlParserState.SEMICOLON_REQUIRED
                && argumentList.getState()
                   != SqlLineParser.SqlParserState.ROUND_BRACKET_BALANCE_FAILED)
            || sql.isEmpty()
            || sql.charAt(sql.length() - 1) != '.')) {
      candidates.addAll(this.candidates);
    }
  }

  private Collection<Candidate> getSchemaBasedCandidates(
      Deque<String> schemaTableColumn) {
    // schema + table + column == 3
    if (schemaTableColumn.size() > 3) {
      return Collections.emptySet();
    }
    Collection<Candidate> candidates = new ArrayList<>();
    final Map<String, Map<String, Set<String>>> schema2tables =
        sqlLine.getDatabaseConnection().getSchema().getSchema2tables();

    final String originalSchemaName = schemaTableColumn.pollFirst();
    final Dialect dialect = sqlLine.getDialect();

    final String schemaName =
        readAsDialectSpecificName(dialect, originalSchemaName);
    final boolean need2Quote =
        isOriginalNameStartedQuoted(dialect, originalSchemaName);
    if (schemaName == null || schema2tables.get(schemaName) == null) {
      for (String sName: schema2tables.keySet()) {
        // without quotes covered in getCompletions
        if (sName == null || !need2Quote) {
          // case without schema like select epms.* from sales.emps;
          // instead of select sales.epms.* from sales.emps;
          continue;
        }
        String value = writeAsDialectSpecificValue(dialect, true, sName);
        candidates.add(
            generateCandidate(sName, value, sqlLine, "schema", false));
      }
      return candidates;
    }
    final String originalTableName = schemaTableColumn.pollFirst();
    final String tableName =
        readAsDialectSpecificName(dialect, originalTableName);
    final boolean need2QuoteTableName =
        isOriginalNameStartedQuoted(dialect, originalTableName);
    if (tableName == null
        || !schema2tables.get(schemaName).containsKey(tableName)) {
      for (String tName: schema2tables.get(schemaName).keySet()) {
        String value =
            writeAsDialectSpecificValue(dialect, need2Quote, schemaName)
            + "."
            + writeAsDialectSpecificValue(dialect, need2QuoteTableName, tName);
        candidates.add(
            generateCandidate(tName, value, sqlLine, "table", true));
      }
    } else {
      Collection<String> columnNames = sqlLine.getDatabaseConnection()
          .getSchema().getColumnNames(schemaName, tableName);
      String userWrittenColumnName = schemaTableColumn.pollFirst();
      final boolean need2QuoteColumnName =
          isOriginalNameStartedQuoted(dialect, userWrittenColumnName);
      for (String columnName: columnNames) {
        String value =
            writeAsDialectSpecificValue(dialect, need2Quote, schemaName)
            + "."
            + writeAsDialectSpecificValue(
                dialect, need2QuoteTableName, tableName)
            + "."
            + writeAsDialectSpecificValue(
                dialect, need2QuoteColumnName, columnName);
        candidates.add(
            generateCandidate(columnName, value, sqlLine, "column", true));
      }
    }
    return candidates;
  }

  private Collection<Candidate> getTableBasedCandidates(
      Deque<String> tableColumn) {
    // table + column == 2
    if (tableColumn.size() > 2) {
      return Collections.emptySet();
    }
    Collection<Candidate> candidates = new ArrayList<>();
    final Map<String, Set<String>> tables2columns = new HashMap<>();
    for (Map<String, Set<String>> map: sqlLine.getDatabaseConnection()
        .getSchema().getSchema2tables().values()) {
      for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
        tables2columns.put(entry.getKey(), entry.getValue());
      }
    }
    final Dialect dialect = sqlLine.getDialect();
    final String originalTableName = tableColumn.pollFirst();
    final String tableName =
        readAsDialectSpecificName(dialect, originalTableName);
    final boolean need2QuoteTableName =
        isOriginalNameStartedQuoted(dialect, originalTableName);
    if (!tables2columns.containsKey(tableName)) {
      for (String tName: tables2columns.keySet()) {
        // covered in getCompletions
        if (!need2QuoteTableName) {
          continue;
        }
        String value = writeAsDialectSpecificValue(dialect, true, tName);
        candidates.add(
            generateCandidate(tName, value, sqlLine, "table", false));
      }
      return candidates;
    }

    Collection<String> columnNames = sqlLine.getDatabaseConnection()
        .getSchema().getColumnNames(null, tableName);
    String userWrittenColumnName = tableColumn.pollFirst();
    final boolean need2QuoteColumnName =
        isOriginalNameStartedQuoted(dialect, userWrittenColumnName);
    for (String columnName: columnNames) {
      String value =
          writeAsDialectSpecificValue(dialect, need2QuoteTableName, tableName)
          + "."
          + writeAsDialectSpecificValue(
              dialect, need2QuoteColumnName, columnName);
      candidates.add(
          generateCandidate(columnName, value, sqlLine, "column", true));
    }
    return candidates;
  }

  static Candidate generateCandidate(
      String sName, String value, SqlLine sqlLine,
      String descr, boolean complete) {
    return new Candidate(value, sName, null,
        sqlLine.loc(descr),
        "table".equalsIgnoreCase(descr) || "schema".equalsIgnoreCase(descr)
            ? "." : null,
        null, complete);
  }

  Deque<String> getSchemaTableColumn(String word) {
    if (word.length() == 0) {
      return new ArrayDeque<>(Collections.emptyList());
    }
    Deque<String> wordList = new ArrayDeque<>();
    Dialect dialect = sqlLine.getDialect();
    String wordToCheck = addClosingSqlIdentifierIfRequired(dialect, word)
        ? word + dialect.getCloseQuote() : word;
    String[][] splitted = sqlLine.splitCompound(wordToCheck, true);
    if (splitted.length > 0) {
      for (String wordItem : splitted[0]) {
        if (wordItem.length() > 0) {
          wordList.addLast(wordItem);
        } else if (wordToCheck.charAt(wordToCheck.length() - 1)
            == dialect.getOpenQuote()) {
          wordList.addLast(String.valueOf(dialect.getOpenQuote()));
        }
      }
    }
    return wordList;
  }

  private boolean addClosingSqlIdentifierIfRequired(
      Dialect dialect, String word) {
    if (word == null || word.isEmpty()) {
      return false;
    }
    int sqlIdentifierQuoteCounter = 0;
    for (int i = 0; i < word.length(); i++) {
      if ((word.charAt(i) == dialect.getOpenQuote()
          || word.charAt(i) == dialect.getCloseQuote())
          && !sqlLine.isCharEscaped(word, i)) {
        sqlIdentifierQuoteCounter++;
      }
    }
    return sqlIdentifierQuoteCounter % 2 == 1;
  }

  String readAsDialectSpecificName(
      Dialect dialect, String originalName) {
    if (originalName == null) {
      return null;
    }
    final boolean isQuoted = !originalName.isEmpty()
        && originalName.charAt(0) == dialect.getOpenQuote();
    if (isQuoted) {
      if (originalName.charAt(originalName.length() - 1)
          == dialect.getCloseQuote()) {
        return originalName.length() == 1
            ? "" : originalName.substring(1, originalName.length() - 1);
      } else {
        return originalName.substring(1);
      }
    } else {
      return dialect.isUpper()
          ? originalName.toUpperCase(Locale.ROOT)
          : dialect.isLower()
              ? originalName.toLowerCase(Locale.ROOT)
              : originalName;
    }
  }

  static String writeAsDialectSpecificValue(
      Dialect dialect, boolean forceQuote, String name2Write) {
    if (name2Write == null) {
      return null;
    }
    boolean needToQuote = false;
    if (forceQuote) {
      needToQuote = true;
    } else {
      boolean isUpper = dialect.isUpper();
      boolean isLower = dialect.isLower();
      String extraChars = dialect.getExtraNameCharacters();
      for (int i = 0; i < name2Write.length(); i++) {
        if (isLower
            && ALLOWED_LOWER_CHARACTERS.indexOf(name2Write.charAt(i)) == -1
            && extraChars.indexOf(name2Write.charAt(i)) == -1) {
          needToQuote = true;
          break;
        } else if (isUpper
            && ALLOWED_UPPER_CHARACTERS.indexOf(name2Write.charAt(i)) == -1
            && extraChars.indexOf(name2Write.charAt(i)) == -1) {
          needToQuote = true;
          break;
        } else if (!isLower
            && !isUpper
            && ALLOWED_LOWER_CHARACTERS.indexOf(name2Write.charAt(i)) == -1
            && ALLOWED_UPPER_CHARACTERS.indexOf(name2Write.charAt(i)) == -1
            && extraChars.indexOf(name2Write.charAt(i)) == -1) {
          needToQuote = true;
          break;
        }
      }
    }
    if (needToQuote) {
      StringBuilder result = new StringBuilder();
      result.append(dialect.getOpenQuote());
      for (char c: name2Write.toCharArray()) {
        if (c == dialect.getOpenQuote() || c == dialect.getCloseQuote()) {
          result.append(c);
        }
        result.append(c);
      }
      result.append(dialect.getCloseQuote());
      return result.toString();
    } else {
      return name2Write;
    }
  }

  private boolean isOriginalNameStartedQuoted(
      Dialect dialect, String originalName) {
    return originalName != null
        && !originalName.isEmpty()
        && originalName.charAt(0) == dialect.getOpenQuote();
  }
}

// End SqlCompleter.java
