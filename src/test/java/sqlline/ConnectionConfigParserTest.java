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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Test cases for ConnectionConfigParserTest.
 */
public class ConnectionConfigParserTest {
  @ParameterizedTest
  @MethodSource("connectionProvider")
  public void testConnectionProperties(
      String fileContent, String[]... expected) {
    try {
      final Path tmpSavedConnections = Files.createTempFile(
          "tmpconfconnections", "temp");
      try (BufferedWriter bw =
          new BufferedWriter(
              new OutputStreamWriter(
                  new FileOutputStream(tmpSavedConnections.toFile()),
                      StandardCharsets.UTF_8))) {
        bw.write(fileContent);
        bw.flush();

        SqlLine sqlLine = new SqlLine();
        sqlLine.getOpts().set(BuiltInProperty.CONNECTION_CONFIG,
            tmpSavedConnections.toAbsolutePath());
        for (String[] expectedConnection : expected) {
          Properties properties =
              new ConnectionConfigParser(sqlLine)
                  .getConnectionProperties(expectedConnection[0]);
          assertNotNull(properties);
          assertEquals(properties.getProperty("url"), expectedConnection[1]);
          assertEquals(properties.getProperty("user"), expectedConnection[2]);
          assertEquals(
              properties.getProperty("password"), expectedConnection[3]);
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  static Stream<Arguments> connectionProvider() {
    return Stream.of(
        of("connection1:\n"
            + "  url:jdbc:postgresql://localhost:5432/postgres\n"
            + "  user:user1\n"
            + "  password:password\n"
            + "connection2: \n"
            + "    url:  jdbc:mysql://localhost/mysql?autoReconnect=true\n"
            + "    user: user2\n"
            + "    password:password2\n"
            + "connection3: #comment\n"
            + "  url:  url:3\n"
            + "  user: user:3\n"
            + "  password:  password3",
            new String[][]{
                {"connection1", "jdbc:postgresql://localhost:5432/postgres",
                    "user1", "password"},
                {"connection2",
                    "jdbc:mysql://localhost/mysql?autoReconnect=true",
                    "user2", "password2"},
                {"connection3", "url:3", "user:3", "password3"}}));
  }
}

// ConnectionConfigParserTest.java
