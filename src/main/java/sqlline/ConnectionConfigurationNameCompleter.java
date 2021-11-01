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

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

public class ConnectionConfigurationNameCompleter<SqlLine>
    implements Completer {

  private SqlLine sqlline;

  @Override
  public void complete(LineReader reader, ParsedLine line,
          List<Candidate> candidates) {
    List<String> connectionNames = sqlline.getCommands()
         .configuredConnectionNames();
    new StringsCompleter(connectionNames).complete(reader, line, candidates);
  }
}
