package sqlline;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

public class ConnectionConfigurationNameCompleter implements Completer {
	
	private SqlLine sqlline;

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		List<String> connectionNames = sqlline.getCommands().configuredConnectionNames();
		new StringsCompleter(connectionNames).complete(reader, line, candidates);
	}

}
