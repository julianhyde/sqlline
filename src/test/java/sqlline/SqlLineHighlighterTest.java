package sqlline;

import org.jline.reader.EOFError;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.junit.Assert;
import org.junit.Test;

public class SqlLineHighlighterTest {
  @Test
  public void testHighlight() {
    DefaultHighlighter highlighter = new SqlLineHighlighter(new SqlLine());
    //highlighter.highlight()

  }

  @Test
  public void testSqlLineParserForWrongLines() {
    DefaultParser parser = new SqlLineParser(new SqlLine())
        .eofOnUnclosedQuote(true)
        .eofOnEscapedNewLine(true);
    Parser.ParseContext acceptLine = Parser.ParseContext.ACCEPT_LINE;
    String[] successfulLinesToCheck = new String[] {
        "!sql",
        "   !all",
        " \n select",
        " \n test ",
        "  test ';",
        " \n test ';'\";",
    };
    for (String line : successfulLinesToCheck) {
      try {
        parser.parse(line, line.length(), acceptLine);
        Assert.fail("Missing closing quote or semicolon for line " + line);
      } catch (EOFError eofError) {
        //ok
      }
    }
  }
}
