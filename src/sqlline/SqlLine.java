/*
 *	Copyright (c) 2002 Marc Prud'hommeaux
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *
 *	This software is hosted by SourceForge.
 *	SourceForge is a trademark of VA Linux Systems, Inc.
 */
package sqlline;

import jline.*;
import java.io.*;
import java.text.*;
import java.sql.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.zip.*;

/** 
 *  A console SQL shell with command completion.
 *  <p>
 *  TODO:
 *  <ul>
 *	<li>Page results</li>
 *	<li>Implement command aliases</li>
 *	<li>Stored procedure execution</li>
 *	<li>Binding parameters to prepared statements</li>
 *	<li>Scripting language</li>
 *	<li>Multiple simultaneous connections</li>
 *	<li>XA transactions</li>
 *  </ul>
 *  
 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
 */
public class SqlLine
{
	private static final ResourceBundle loc = ResourceBundle.getBundle (
		SqlLine.class.getName ());

	public static final String APP_NAME = "sqlline";
	public static final String APP_VERSION = "0.6.3";
	public static final String APP_AUTHOR = "Marc Prud'hommeaux";
	public static final String APP_AUTHOR_EMAIL = "marc@apocalypse.org";
	public static final String APP_TITLE = APP_NAME + " " + APP_VERSION
											+ " by " + APP_AUTHOR;
	private static final String sep = System.getProperty ("line.separator");
	private boolean exit = false;
	private final SqlLine sqlline = this;
	private Collection drivers = null;
	private Connections connections = new Connections ();
	public static final String COMMAND_PREFIX = "!";
	private Completor sqlLineCommandCompletor;
	private Map completions = new HashMap ();
	private Opts opts = new Opts (System.getProperties ());
	String lastProgress = null;
	String prompt = "sqlline";
	private Map seenWarnings = new HashMap ();
	private final Commands command = new Commands ();
	private Script script = null;


	private CommandHandler [] commands = new CommandHandler []
	{
		new ReflectiveCommandHandler (new String [] { "quit", "done", "exit" },
			loc ("help-quit")),
		new ReflectiveCommandHandler (new String [] { "connect", "open" },
			loc ("help-connect")),
		new ReflectiveCommandHandler (new String [] { "describe", "desc" },
			loc ("help-describe")),
		new ReflectiveCommandHandler (new String [] { "reconnect" },
			loc ("help-reconnect")),
		new MetaDataCommandHandler (new String [] { "metadata", "meta" },
			loc ("help-metadata")),
		new ReflectiveCommandHandler (new String [] { "dbinfo" },
			loc ("help-dbinfo")),
		new ReflectiveCommandHandler (new String [] { "rehash" },
			loc ("help-rehash")),
		new ReflectiveCommandHandler (new String [] { "verbose" },
			loc ("help-verbose")),
		new ReflectiveCommandHandler (new String [] { "run" },
			loc ("help-run")),
		new ReflectiveCommandHandler (new String [] { "list" },
			loc ("help-list")),
		new ReflectiveCommandHandler (new String [] { "all" },
			loc ("help-all")),
		new ReflectiveCommandHandler (new String [] { "go", "#" },
			loc ("help-go")),
		new ReflectiveCommandHandler (new String [] { "script" },
			loc ("help-script")),
		new ReflectiveCommandHandler (new String [] { "brief" },
			loc ("help-brief")),
		new ReflectiveCommandHandler (new String [] { "close" },
			loc ("help-close")),
		new ReflectiveCommandHandler (new String [] { "isolation" },
			loc ("help-isolation")),
		new ReflectiveCommandHandler (new String [] { "autocommit" },
			loc ("help-autocommit")),
		new ReflectiveCommandHandler (new String [] { "driverinfo" },
			loc ("help-driverinfo")),
		new ReflectiveCommandHandler (new String [] { "commit" },
			loc ("help-commit")),
		new ReflectiveCommandHandler (new String [] { "rollback" },
			loc ("help-rollback")),
		new ReflectiveCommandHandler (new String [] { "help", "?" },
			loc ("help-help")),
		new ReflectiveCommandHandler (new String [] { "set" },
			loc ("help-set")),
		new ReflectiveCommandHandler (new String [] { "save" },
			loc ("help-save")),
		new ReflectiveCommandHandler (new String [] { "alias" },
			loc ("help-alias")),
		new ReflectiveCommandHandler (new String [] { "unalias" },
			loc ("help-unalias")),
		new ReflectiveCommandHandler (new String [] { "scan" },
			loc ("help-scan")),
		new ReflectiveCommandHandler (new String [] { "sql" },
			loc ("help-sql")),
	};


	static final SortedSet KNOWN_DRIVERS = new TreeSet (Arrays.asList (
		new String [] {
			"com.merant.datadirect.jdbc.sqlserver.SQLServerDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"com.ddtek.jdbc.informix.InformixDriver",
			"org.sourceforge.jxdbcon.JXDBConDriver",
			"com.ddtek.jdbc.oracle.OracleDriver",
			"net.sourceforge.jtds.jdbc.Driver",
			"com.pointbase.jdbc.jdbcDriver",
			"com.internetcds.jdbc.tds.SybaseDriver",
			"org.enhydra.instantdb.jdbc.idbDriver",
			"com.sybase.jdbc2.jdbc.SybDriver",
			"com.ddtek.jdbc.sybase.SybaseDriver",
			"COM.cloudscape.core.JDBCDriver",
			"in.co.daffodil.db.jdbc.DaffodilDBDriver",
			"com.jnetdirect.jsql.JSQLDriver",
			"COM.ibm.db2.jdbc.net.DB2Driver",
			"org.hsqldb.jdbcDriver",
			"com.pointbase.jdbc.jdbcUniversalDriver",
			"com.ddtek.jdbc.sqlserver.SQLServerDriver",
			"com.ddtek.jdbc.db2.DB2Driver",
			"com.merant.datadirect.jdbc.oracle.OracleDriver",
			"COM.cloudscape.core.RmiJdbcDriver",
			"oracle.jdbc.OracleDriver",
			"com.informix.jdbc.IfxDriver",
			"com.merant.datadirect.jdbc.informix.InformixDriver",
			"com.ibm.db2.jcc.DB2Driver",
			"com.pointbase.jdbc.jdbcEmbeddedDriver",
			"org.gjt.mm.mysql.Driver",
			"org.postgresql.Driver",
			"com.mysql.jdbc.Driver",
			"oracle.jdbc.driver.OracleDriver",
			"interbase.interclient.Driver",
			"com.mysql.jdbc.NonRegisteringDriver",
			"com.merant.datadirect.jdbc.db2.DB2Driver",
			"com.merant.datadirect.jdbc.sybase.SybaseDriver",
			"com.internetcds.jdbc.tds.Driver",
			"org.hsqldb.jdbcDriver",
			"org.hsql.jdbcDriver",
			"COM.cloudscape.core.JDBCDriver",
			"in.co.daffodil.db.jdbc.DaffodilDBDriver",
			"com.ddtek.jdbc.db2.DB2Driver",
			"interbase.interclient.Driver",
			"com.mysql.jdbc.Driver",
			"com.ddtek.jdbc.oracle.OracleDriver",
			"org.postgresql.Driver",
			"com.pointbase.jdbc.jdbcUniversalDriver",
			"org.sourceforge.jxdbcon.JXDBConDriver",
			"com.ddtek.jdbc.sqlserver.SQLServerDriver",
			"com.jnetdirect.jsql.JSQLDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"weblogic.jdbc.mssqlserver4.Driver",
			"com.ddtek.jdbc.sybase.SybaseDriver",
			"oracle.jdbc.pool.OracleDataSource",
			"org.axiondb.jdbc.AxionDriver",
			"COM.ibm.db2.jdbc.app.DB2Driver",
			"com.ibm.as400.access.AS400JDBCDriver",
			"COM.FirstSQL.Dbcp.DbcpDriver",
			"COM.ibm.db2.jdbc.net.DB2Driver",
			"org.enhydra.instantdb.jdbc.idbDriver",
			"com.informix.jdbc.IfxDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"com.imaginary.sql.msql.MsqlDriver",
			"sun.jdbc.odbc.JdbcOdbcDriver",
			"oracle.jdbc.driver.OracleDriver",
			"intersolv.jdbc.sequelink.SequeLinkDriver",
			"openlink.jdbc2.Driver",
			"com.pointbase.jdbc.jdbcUniversalDriver",
			"postgres95.PGDriver",
			"postgresql.Driver",
			"solid.jdbc.SolidDriver",
			"centura.java.sqlbase.SqlbaseDriver",
			"interbase.interclient.Driver",
			"com.mckoi.JDBCDriver",
			"com.inet.tds.TdsDriver",
			"com.microsoft.jdbc.sqlserver.SQLServerDriver",
			"com.thinweb.tds.Driver",
			"weblogic.jdbc.mssqlserver4.Driver",
			"com.mysql.jdbc.DatabaseMetaData",
			"org.gjt.mm.mysql.Driver",
			"com.sap.dbtech.jdbc.DriverSapDB",
			"com.sybase.jdbc2.jdbc.SybDriver",
			"com.sybase.jdbc.SybDriver",
			"com.internetcds.jdbc.tds.Driver",
			"weblogic.jdbc.pool.Driver",
	}));


	static
	{
		try
		{
			Class.forName ("jline.ConsoleReader");
		}
		catch (Throwable t)
		{
			throw new ExceptionInInitializerError (loc ("jline-missing"));
		}
	}


	static String loc (String res)
	{
		return loc (res, new Object [0]);
	}


	static String loc (String res, int param)
	{
		return loc (res, new Object [] { new Integer (param) });
	}


	static String loc (String res, Object param1)
	{
		return loc (res, new Object [] { param1 });
	}


	static String loc (String res, Object param1, Object param2)
	{
		return loc (res, new Object [] { param1, param2 });
	}


	static String loc (String res, Object [] params)
	{
		try
		{
			return MessageFormat.format (loc.getString (res), params);
		}
		catch (Exception e)
		{
			try
			{
				return res + ": " + Arrays.asList (params);
			}
			catch (Exception e2)
			{
				return res;
			}
		}
	}


	/** 
	 *  Starts the program.
	 */
	public static void main (String [] args)
		throws IOException
	{
		SqlLine sqlline = new SqlLine ();
		sqlline.begin (args);
	}


	SqlLine ()
	{
		// registerKnownDrivers ();

		sqlLineCommandCompletor = new ArgumentCompletor (
			new SQLLineCommandCompletor ());
	}


	DatabaseConnection con ()
	{
		return connections.current ();
	}


	/** 
	 *  Entry point to creating a {@link ColorBuffer} with color
	 *  enabled or disabled depending on the calue of
	 *  {@link Opts#getColor}.
	 */
	ColorBuffer color ()
	{
		return new ColorBuffer (opts.getColor ());
	}


	/** 
	 *  Entry point to creating a {@link ColorBuffer} with color
	 *  enabled or disabled depending on the calue of
	 *  {@link Opts#getColor}.
	 */
	ColorBuffer color (String msg)
	{
		return new ColorBuffer (msg, opts.getColor ());
	}


	/** 
	 *  Walk through all the known drivers and try to register them.
	 */
	void registerKnownDrivers ()
	{
		for (Iterator i = KNOWN_DRIVERS.iterator (); i.hasNext (); )
		{
			try { Class.forName (i.next ().toString ()); }
				catch (Throwable t) { }
		}
	}


	void initArgs (String [] args)
	{
		Properties props = new Properties ();

		String command = null;
		String driver = null, user = null, pass = null, url = null, cmd = null;

		for (int i = 0; i < args.length; i++)
		{
			// -- arguments are treated as properties
			if (args [i].startsWith ("--"))
			{
				String [] parts = split (args [i].substring (2), "=");
				debug (loc ("setting-prop", Arrays.asList (parts)));
				if (parts.length > 0)
				{
					if (parts.length >= 2)
						props.setProperty (parts [0], parts [1]);
					else
						props.setProperty (parts [0], "true");
				}

				continue;
			}

			if (i + 1 >= args.length)
				continue;

			if (args [i].equals ("-d"))
				driver = args [i++ + 1];
			else if (args [i].equals ("-n"))
				user = args [i++ + 1];
			else if (args [i].equals ("-p"))
				pass = args [i++ + 1];
			else if (args [i].equals ("-u"))
				url = args [i++ + 1];
			else if (args [i].equals ("-e"))
				command = args [i++ + 1];
		}

		for (Iterator i = props.keySet ().iterator (); i.hasNext (); )
		{
			String key = i.next ().toString ();
			opts.set (key, props.getProperty (key));
		}

		if (user != null && pass != null && url != null)
		{
			String com = "!connect " + url + " " + user + " " + pass + " "
				+ (driver == null ? "" : driver);
			debug ("issuing: " + com);
			dispatch (com);
		}

		if (command != null)
		{
			debug (loc ("executing-command", command));
			dispatch (command);
		}
	}


	/** 
	 *  Start accepting input from stdin, and dispatch it
	 *  to the appropriate {@link CommandHandler} until the
	 *  global variable <code>exit</code> is true.
	 */
	void begin (String [] args)
		throws IOException
	{
		try
		{
			output (APP_TITLE);
			command.load (null);
		}
		catch (Exception e)
		{
		}

		ConsoleReader reader = getConsoleReader ();
		initArgs (args);

		try
		{
			while (!exit && dispatch (reader.readLine (getPrompt ())));
		}
		catch (EOFException eof)
		{
			// CTRL-D
			command.quit (null);
		}
		catch (Throwable t)
		{
			handleException (t);
		}
	}


	public ConsoleReader getConsoleReader ()
		throws IOException
	{
		Terminal.setupTerminal ();
		ConsoleReader reader = new ConsoleReader ();


		// setup history
		ByteArrayInputStream historyBuffer = null;

		if (new File (opts.getHistoryFile ()).isFile ())
		{
			try
			{
				// save the current contents of the history buffer. This gets
				// around a bug in JLine where setting the output before the
				// input will clobber the history input, but setting the 
				// input before the output will cause the previous commands
				// to not be saved to the buffer.
				FileInputStream historyIn = new FileInputStream (
					opts.getHistoryFile ());
				ByteArrayOutputStream hist = new ByteArrayOutputStream ();
				int n;
				while ((n = historyIn.read ()) != -1)
					hist.write (n);
				historyIn.close ();
	
				historyBuffer = new ByteArrayInputStream (hist.toByteArray ());
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}

		try
		{
			// now set the output for the history
			PrintWriter historyOut = new PrintWriter (new FileWriter (
				opts.getHistoryFile ()));
			reader.getHistory ().setOutput (historyOut);
		}
		catch (Exception e)
		{
			handleException (e);
		}

		try
		{
			// now load in the previous history
			if (historyBuffer != null)
				reader.getHistory ().load (historyBuffer);
		}
		catch (Exception e)
		{
			handleException (e);
		}


		reader.addCompletor (new SQLLineCompletor ());

		return reader;
	}


	/** 
	 *  Dispatch the specified line to the appropriate
	 *  {@link CommandHandler}.
	 *  
	 *  @param  line  the commmand-line to dispatch
	 *  @return  true if we should continue accepting input
	 */
	boolean dispatch (String line)
	{
		if (line == null)
			return false;

		if (line.trim ().length () == 0)
			return true;

		line = line.trim ();

		// save it to the current script, if any
		if (script != null)
			script.addCommand (line);

		if (line.equals ("?") || line.equalsIgnoreCase ("help"))
			line = "!help";

		if (line.startsWith (COMMAND_PREFIX))
		{
			line = line.substring (1);
			for (int i = 0; i < commands.length; i++)
			{
				if (commands [i].matches (line))
				{
					commands [i].execute (line);
					return true;
				}
			}
		}
		else
		{
			command.sql (line);
			return true;
		}

		if (line != null)
			error (loc ("unknown-command", line));

		return true;
	}


	/** 
	 *  Pint the specified message to the console
	 *  
	 *  @param  msg  the message to print
	 */
	void output (String msg)
	{
		output (msg, true);
	}


	void error (String msg)
	{
		output (color ().red (msg), true);
	}


	void info (String msg)
	{
		output (color ().green (msg), true);
	}


	void debug (String msg)
	{
		if (opts.getVerbose ())
			output (color ().blue (msg), true);
	}


	void output (ColorBuffer msg)
	{
		output (msg.getColor ());
	}


	void output (ColorBuffer msg, boolean newline)
	{
		output (msg.getColor (), newline);
	}


	/** 
	 *  Print the specified message to the console
	 *  
	 *  @param  msg  the message to print
	 *  @param  newline  if false, do not append a newline
	 */
	void output (String msg, boolean newline)
	{
		if (newline)
			System.out.println (msg);
		else
			System.out.print (msg);
	}


	/** 
	 *  Ensure that autocommit is on for the current connection
	 *  
	 *  @return  true if autocommit is set
	 */
	boolean assertAutoCommit ()
	{
		if (!(assertConnection ()))
			return false;

		try
		{
			if (con ().connection.getAutoCommit ())
			{
				error (loc ("autocommit-needs-off"));
				return false;
			}
		}
		catch (Exception e)
		{
			handleException (e);
			return false;
		}

		return true;
	}


	/** 
	 *  Assert that we have an active, living connection. Print
	 *  an error message if we do not.
	 *  
	 *  @return  true if there is a current, active connection
	 */
	boolean assertConnection ()
	{
		try
		{
			if (con () == null || con ().connection == null)
			{
				error (loc ("no-current-connection"));
				return false;
			}

			if (con ().connection.isClosed ())
			{
				error (loc ("connection-is-closed"));
				return false;
			}
		}
		catch (SQLException sqle)
		{
			error (loc ("no-current-connection"));
			return false;
		}

		return true;
	}


	/** 
	 *  Print out any warnings that exist for the current connection.
	 */
	void showWarnings ()
	{
		if (con ().connection == null)
			return;

		if (!opts.getVerbose ())
			return;

		try
		{
			showWarnings (con ().connection.getWarnings ());
		}
		catch (Exception e)
		{
			handleException (e);
		}
	}


	/** 
	 *  Print the specified warning on the console, as well as
	 *  any warnings that are returned from
	 *  {@link SQLWarning#getNextWarning}.
	 *  
	 *  @param  warn  the {@link SQLWarning} to print
	 */
	void showWarnings (SQLWarning warn)
	{
		if (warn == null)
			return;

		if (seenWarnings.get (warn) == null)
		{
			// don't re-display warnings we have already seen
			seenWarnings.put (warn, new java.util.Date ());
			handleSQLException (warn);
		}

		SQLWarning next = warn.getNextWarning ();
		if (next != warn)
			showWarnings (next);
	}


	public void reconnect (String line)
	{
		if (con () == null || con ().url == null)
		{
			error ("No current connection");
			return;
		}

		output ("Reconnecting to " + con ().url);
		try
		{
			con ().reconnect ();
		}
		catch (Exception e)
		{
			handleException (e);
		}
	}


	String getPrompt ()
	{
		if (con () == null || con ().url == null)
			return "sqlline> ";
		else
			return getPrompt (connections.getIndex ()
				+ ": " + con ().url) + "> ";
	}


	static String getPrompt (String url)
	{
		if (url == null || url.length () == 0)
			url = "sqlline";

		if (url.indexOf (";") > -1)
			url = url.substring (0, url.indexOf (";"));
		if (url.indexOf ("?") > -1)
			url = url.substring (0, url.indexOf ("?"));

		if (url.length () > 45)
			url = url.substring (0, 45);

		return url;
	}


	/** 
	 *  Try to obtain the current size of the specified {@link ResultSet}
	 *  by jumping to the last row and getting the row number.
	 *  
	 *  @param  rs	the {@link ResultSet} to get the size for
	 *  @return the size, or -1 if it could not be obtained
	 */
	int getSize (ResultSet rs)
	{
		try
		{
			if (rs.getType () == rs.TYPE_FORWARD_ONLY)
				return -1;

			rs.last ();
			int total = rs.getRow ();
			rs.beforeFirst ();
			return total;
		}
		catch (SQLException sqle)
		{
			return -1;
		}
		// JDBC 1 driver error
		catch (AbstractMethodError ame)
		{
			return -1;
		}
	}


	ResultSet getColumns (String table)
		throws SQLException
	{
		if (!(assertConnection ()))
			return null;

		return con ().meta.getColumns (
			con ().meta.getConnection ().getCatalog (), null, table, "%");
	}


	ResultSet getTables ()
		throws SQLException
	{
		if (!(assertConnection ()))
			return null;

		return con ().meta.getTables (
			con ().meta.getConnection ().getCatalog (), null, "%",
				new String [] { "TABLE" });
	}


	String [] getColumnNames (DatabaseMetaData meta)
		throws SQLException
	{
		Set names = new HashSet ();

		output ("Building list of tables and columns for tab-completion "
			+ "(set fastconnect to true to skip)....");

		try
		{
			ResultSet columns = getColumns ("%");

			try
			{
				int total = getSize (columns);
				int index = 0;
	
				while (columns.next ())
				{
					// add the following strings:
					// 1. column name
					// 2. table name
					// 3. tablename.columnname

					progress (index++, total);
					String name = columns.getString ("TABLE_NAME");
					names.add (name);
					names.add (columns.getString ("COLUMN_NAME"));
					names.add (columns.getString ("TABLE_NAME") + "."
						+ columns.getString ("COLUMN_NAME"));
				}

				progress (index, index);
			}
			finally
			{
				columns.close ();
			}
	
			output ("Done");
	
			return (String [])names.toArray (new String [0]);
		}
		catch (Throwable t)
		{
			handleException (t);
			return new String [0];
		}
	}


	////////////////////
	// String utilities
	////////////////////


	/** 
	 *  Pad the specified String with spaces to the indicated length
	 *  
	 *  @param  str  the String to pad
	 *  @param  len  the length we want the return String to be
	 *  @return  the passed in String with spaces appended until the
	 *  		length matches the specified length.
	 */
	ColorBuffer pad (ColorBuffer str, int len)
	{
		if (str == null)
			str = color ();

		while (str.getVisibleLength () < len)
			str.append (" ");

		return str;
	}


	String center (String str, int len)
	{
		StringBuffer buf = new StringBuffer (str);

		while (buf.length () < len)
		{
			buf.append (" ");

			if (buf.length () < len)
				buf.insert (0, " ");
		}

		return buf.toString ();
	}


	ColorBuffer pad (String str, int len)
	{
		if (str == null)
			str = "";

		return pad (color (str), len);
	}


	/** 
	 *  Split the line into an array by tokenizing on space characters
	 *  
	 *  @param  line	the line to break up
	 *  @return  		an array of individual words
	 */
	String [] split (String line)
	{
		return split (line, " ");
	}


	String [] split (String line, String delim)
	{
		StringTokenizer tok = new StringTokenizer (line, delim);
		String [] ret = new String [tok.countTokens ()];
		int index = 0;
		while (tok.hasMoreTokens ())
			ret [index++] = tok.nextToken ();

		return ret;
	}


	/** 
	 *  Split the line based on spaces, asserting that the
	 *  number of words is correct.
	 *  
	 *  @param  line		the line to split
	 *  @param  assertLen	the number of words to assure
	 *  @param  usage		the message to output if there are an incorrect
	 *  					number of words.
	 *  @return  the split lines, or null if the assertion failed.
	 */
	String [] split (String line, int assertLen, String usage)
	{
		String [] ret = split (line);

		if (ret.length != assertLen)
		{
			error (usage);
			return null;
		}

		return ret;
	}


	/** 
	 *  Wrap the specified string by breaking on space characters.
	 *  
	 *  @param  toWrap		the string to wrap
	 *  @param  len			the maximum length of any line
	 *  @param  start		the number of spaces to pad at the
	 *  					beginning of a line
	 *  @return  the wrapped string
	 */
	String wrap (String toWrap, int len, int start)
	{
		String cur = toWrap;
		StringBuffer buff = new StringBuffer ();
		StringBuffer line = new StringBuffer ();

		char [] head = new char [start];
		Arrays.fill (head, ' ');

		for (StringTokenizer tok = new StringTokenizer (toWrap, " ");
			tok.hasMoreTokens (); )
		{
			String next = tok.nextToken ();
			if (line.length () + next.length () > len)
			{
				buff.append (line).append (sep).append (head);
				line.setLength (0);
			}

			line.append (line.length () == 0 ? "" : " ").append (next);
		}

		buff.append (line);

		return buff.toString ();
	}


	/** 
	 *  Output a progress indicator to the console.
	 *  
	 *  @param  cur  the current progress
	 *  @param  max  the maximum progress, or -1 if unknown
	 */
	void progress (int cur, int max)
	{
		StringBuffer out = new StringBuffer ();

		if (lastProgress != null)
		{
			char [] back = new char [lastProgress.length ()];
			Arrays.fill (back, '\b');
			out.append (back);
		}

		String progress = cur + "/" + (max == -1 ? "?" : "" + max) + " "
			+ (max == -1 ? "(??%)"
				: ("(" + (cur * 100 / (max == 0 ? 1 : max)) + "%)"));;

		if (cur >= max && max != -1)
		{
			progress += " done" + sep;
			lastProgress = null;
		}
		else
		{
			lastProgress = progress;
		}

		out.append (progress);

		System.out.print (out.toString ());
		System.out.flush ();
	}

	///////////////////////////////
	// Exception handling routines
	///////////////////////////////

	void handleException (Throwable e)
	{
		while (e instanceof InvocationTargetException)
		{
			e = ((InvocationTargetException)e).getTargetException ();
		}

		if (e instanceof SQLException)
		{
			handleSQLException ((SQLException)e);
		}
		else if (!(opts.getVerbose ()))
		{
			if (e.getMessage () == null)
				error (e.getClass ().getName ());
			else
				error (e.getMessage ());
		}
		else
		{
			e.printStackTrace ();
		}
	}


	void handleSQLException (SQLException e)
	{
		if (e instanceof SQLWarning && !(opts.getShowWarnings ()))
			return;

		String type = e instanceof SQLWarning ? "Warning" : "Error";

		error (type + ": " + e.getMessage ()
			+ " (state=" + e.getSQLState ()
			+ ",code=" + e.getErrorCode () + ")");
	}


	Driver [] scanDrivers (String line)
	{
		Set paths = new HashSet ();
		Set driverClasses = new HashSet ();

		for (StringTokenizer tok = new StringTokenizer (
			System.getProperty ("java.ext.dirs"),
			System.getProperty ("path.separator"));
			tok.hasMoreTokens (); )
		{
			File [] files = new File (tok.nextToken ()).listFiles ();
			for (int i = 0; i < files.length; i++)
				paths.add (files [i].getAbsolutePath ());
		}

		for (StringTokenizer tok = new StringTokenizer (
			System.getProperty ("java.class.path"),
			System.getProperty ("path.separator"));
			tok.hasMoreTokens (); )
		{
			paths.add (new File (tok.nextToken ()).getAbsolutePath ());
		}


		for (Iterator i = paths.iterator (); i.hasNext (); )
		{
			File f = new File ((String)i.next ());
			output (pad (loc ("scanning", f.getAbsolutePath ()), 60), false);

			try
			{
				ZipFile zf = new ZipFile (f);
				int total = zf.size ();
				int index = 0;

				for (Enumeration enum = zf.entries ();
					enum.hasMoreElements (); )
				{
					ZipEntry entry = (ZipEntry)enum.nextElement ();
					String name = entry.getName ();
					progress (index++, total);

					if (name.endsWith (".class"))
					{
						name = name.replace ('/', '.');
						name = name.substring (0, name.length () - 6);

						try
						{
							// check for the string "driver" in the class
							// to see if we should load it. Not perfect, but
							// it is far too slow otherwise.
							if (name.toLowerCase ().indexOf ("driver") != -1)
							{
								Class c = Class.forName (name, false,
									getClass ().getClassLoader ());
								if (Driver.class.isAssignableFrom (c)
									&& !(Modifier.isAbstract (
										c.getModifiers ())))
								{
									try
									{
										// load and initialize
										Class.forName (name);
									}
									catch (Exception e)
									{
									}
									driverClasses.add (c.newInstance ());
								}
							}
						}
						catch (Throwable t) { }
					}
				}

				progress (total, total);
			}
			catch (Exception e) { }
		}

		return (Driver [])driverClasses.toArray (new Driver [0]);
	}


	///////////////////////////////////////
	// ResultSet output formatting classes
	///////////////////////////////////////

	void print (ResultSet rs)
		throws SQLException
	{
		ResultSetMetaData meta = rs.getMetaData ();
		int count = meta.getColumnCount ();
		Rows rows = new Rows (rs);
		int index = 0;
		ColorBuffer header = null;
		ColorBuffer headerCols = null;
		final int width = opts.getMaxWidth () - 4;


		// normalize the columns sizes
		rows.normalizeWidths ();

		for (Iterator i = rows.iterator (); i.hasNext (); )
		{
			Rows.Row row = (Rows.Row)i.next ();
			ColorBuffer cbuf = row.getOutputString ();
			cbuf = cbuf.truncate (width);

			if (index == 0)
			{
				StringBuffer h = new StringBuffer ();
				for (int j = 0; j < row.sizes.length; j++)
				{
					for (int k = 0; k < row.sizes [j]; k++)
						h.append ('-');
					h.append ("-+-");
				}

				headerCols = cbuf;
				header = color ().green (h.toString ()).truncate (
					headerCols.getVisibleLength ());
			}

			if (index == 0 ||
				(opts.getHeaderInterval () > 0
				 && index % opts.getHeaderInterval () == 0
				 && opts.getShowHeader ()))
			{
				printRow (header, true);
				printRow (headerCols, false);
				printRow (header, true);
			}

			if (index != 0) // don't output the header twice
				printRow (cbuf, false);

			index++;
		}

		if (header != null && opts.getShowHeader ())
			printRow (header, true);

		output (loc ("rows-selected", rows.getRowCount ()));
	}


	void printRow (ColorBuffer cbuff, boolean header)
	{
		if (header)
			output (color ().green ("+-").append (cbuff).green ("-+"));
		else
			output (color ().green ("| ").append (cbuff).green (" |"));
	}


	class Rows
		extends LinkedList
	{
		final ResultSetMetaData rsMeta;
		final Boolean [] primaryKeys;

		Rows (ResultSet rs)
			throws SQLException
		{
			this.rsMeta = rs.getMetaData ();

			int count = rsMeta.getColumnCount ();
			primaryKeys = new Boolean [count];

			add (new Row (count));

			while (rs.next ())
				add (new Row (count, rs));
		}


		public int getRowCount ()
		{
			// the number of rows minus the header
			return size () - 1;
		}


		boolean isPrimaryKey (int col)
		{
			if (primaryKeys [col] != null)
				return primaryKeys [col].booleanValue ();

			try
			{
				String table = rsMeta.getTableName (col + 1);
				String column = rsMeta.getColumnName (col + 1);

				if (table == null || table.length () == 0 ||
						column == null || column.length () == 0)
				{
					return (primaryKeys [col] = new Boolean (false))
						.booleanValue ();
				}

				ResultSet pks = con ().meta.getPrimaryKeys (
					con ().meta.getConnection ().getCatalog (), null, table);

				try
				{
					while (pks.next ())
					{
						if (column.equalsIgnoreCase (
							pks.getString ("COLUMN_NAME")))
							return (primaryKeys [col] = new Boolean (true))
								.booleanValue ();
					}
				}
				finally
				{
					pks.close ();
				}

				return (primaryKeys [col] = new Boolean (false))
					.booleanValue ();
			}
			catch (SQLException sqle)
			{
				return (primaryKeys [col] = new Boolean (false))
					.booleanValue ();
			}
		}


		/** 
		 *  Update all of the rows to have the same size, set to the
		 *  maximum length of each column in the Rows. 
		 */
		void normalizeWidths ()
		{
			int [] max = null;
			for (int i = 0; i < size (); i++)
			{
				Row row = (Row)get (i);
				if (max == null)
					max = new int [row.values.length];

				for (int j = 0; j < max.length; j++)
				{
					max [j] = Math.max (max [j], row.sizes [j] + 1);
				}
			}

			for (int i = 0; i < size (); i++)
			{
				Row row = (Row)get (i);
				row.sizes = max;
			}
		}


		class Row
		{
			final String [] values;
			final boolean isMeta;
			private boolean deleted;
			private boolean inserted;
			private boolean updated;
			private int [] sizes;
	

			Row (int size)
				throws SQLException
			{
				isMeta = true;
				values = new String [size];
				sizes = new int [size];
				for (int i = 0; i < size; i++)
				{
					values [i] = rsMeta.getColumnLabel (i + 1);
					sizes [i] = values [i] == null ? 1 : values [i].length ();
				}

				deleted = false;
				updated = false;
				inserted = false;
			}


			Row (int size, ResultSet rs)
				throws SQLException
			{
				isMeta = false;
				values = new String [size];
				sizes = new int [size];
	
				try { deleted = rs.rowDeleted (); } catch (Throwable t) { }
				try { updated = rs.rowUpdated (); } catch (Throwable t) { }
				try { inserted = rs.rowInserted (); } catch (Throwable t) { }

				for (int i = 0; i < size; i++)
				{
					values [i] = rs.getString (i + 1);
					sizes [i] = values [i] == null ? 1 : values [i].length ();
				}
			}
	
	
			public ColorBuffer getOutputString ()
			{
				return getOutputString (" | ");
			}
	
	
			ColorBuffer getOutputString (String delim)
			{
				ColorBuffer buf = color ();
	
				for (int i = 0; i < values.length; i++)
				{
					if (buf.getVisibleLength () > 0)
						buf.green (delim);
	
					ColorBuffer v;
					
					if (isMeta)
					{
						v = color (center (values [i], sizes [i]));
						if (isPrimaryKey (i))
							buf.cyan (v.getMono ());
						else
							buf.bold (v.getMono ());
					}
					else
					{
						v = pad (values [i], sizes [i]);
						if (isPrimaryKey (i))
							buf.cyan (v.getMono ());
						else
							buf.append (v.getMono ());
					}
				}
	
				if (deleted) // make deleted rows red
					buf = color ().red (buf.getMono ());
				else if (updated) // make updated rows blue
					buf = color ().blue (buf.getMono ());
				else if (inserted) // make new rows green
					buf = color ().green (buf.getMono ());
	
				return buf;
			}
		}
	}


	///////////////////////////////
	// Console interaction classes
	///////////////////////////////

	final static class ColorBuffer
	{
		private static final ColorAttr BOLD = new ColorAttr ("\033[1m");
		private static final ColorAttr NORMAL = new ColorAttr ("\033[m");
		private static final ColorAttr REVERS = new ColorAttr ("\033[7m");
		private static final ColorAttr LINED = new ColorAttr ("\033[4m");
		private static final ColorAttr GREY = new ColorAttr ("\033[1;30m");
		private static final ColorAttr RED = new ColorAttr ("\033[1;31m");
		private static final ColorAttr GREEN = new ColorAttr ("\033[1;32m");
		private static final ColorAttr BLUE = new ColorAttr ("\033[1;34m");
		private static final ColorAttr CYAN = new ColorAttr ("\033[1;36m");
		private static final ColorAttr YELLOW = new ColorAttr ("\033[1;33m");
		private static final ColorAttr MAGENTA = new ColorAttr ("\033[1;35m");
		private static final ColorAttr INVISIBLE = new ColorAttr ("\033[8m");

		private final List parts = new LinkedList ();

		private final boolean useColor;


		public ColorBuffer (boolean useColor)
		{
			this.useColor = useColor;

			append ("");
		}


		public ColorBuffer (String str, boolean useColor)
		{
			this.useColor = useColor;

			append (str);
		}


		public String getColor ()
		{
			return getBuffer (useColor);
		}


		public String getMono ()
		{
			return getBuffer (false);
		}


		String getBuffer (boolean color)
		{
			StringBuffer buf = new StringBuffer ();
			for (Iterator i = parts.iterator (); i.hasNext (); )
			{
				Object next = i.next ();
				if (!color && next instanceof ColorAttr)
					continue;

				buf.append (next.toString ());
			}

			return buf.toString ();
		}


		/** 
		 *  Truncate the ColorBuffer to the specified length and return
		 *  the new ColorBuffer. Any open color tags will be closed.
		 */
		public ColorBuffer truncate (int len)
		{
			ColorBuffer cbuff = new ColorBuffer (useColor);
			ColorAttr lastAttr = null;
			for (Iterator i = parts.iterator ();
				cbuff.getVisibleLength () < len && i.hasNext (); )
			{
				Object next = i.next ();
				if (next instanceof ColorAttr)
				{
					lastAttr = (ColorAttr)next;
					cbuff.append ((ColorAttr)next);
					continue;
				}

				String val = next.toString ();
				if (cbuff.getVisibleLength () + val.length () > len)
				{
					int partLen = len - cbuff.getVisibleLength ();
					val = val.substring (0, partLen);
				}

				cbuff.append (val);
			}

			// close off the buffer with a normal tag
			if (lastAttr != null && lastAttr != NORMAL)
			{
				cbuff.append (NORMAL);
			}

			return cbuff;
		}


		public String toString ()
		{
			return getColor ();
		}


		public ColorBuffer append (String str)
		{
			parts.add (str);
			return this;
		}


		public ColorBuffer append (ColorBuffer buf)
		{
			parts.addAll (buf.parts);
			return this;
		}


		public ColorBuffer append (ColorAttr attr)
		{
			parts.add (attr);
			return this;
		}


		public int getVisibleLength ()
		{
			return getMono ().length ();
		}


		public ColorBuffer append (ColorAttr attr, String val)
		{
			parts.add (attr);
			parts.add (val);
			parts.add (NORMAL);
			return this;
		}


		public ColorBuffer bold (String str)
		{
			return append (BOLD, str);
		}


		public ColorBuffer lined (String str)
		{
			return append (LINED, str);
		}


		public ColorBuffer grey (String str)
		{
			return append (GREY, str);
		}


		public ColorBuffer red (String str)
		{
			return append (RED, str);
		}


		public ColorBuffer blue (String str)
		{
			return append (BLUE, str);
		}


		public ColorBuffer green (String str)
		{
			return append (GREEN, str);
		}


		public ColorBuffer cyan (String str)
		{
			return append (CYAN, str);
		}


		public ColorBuffer yellow (String str)
		{
			return append (YELLOW, str);
		}


		public ColorBuffer magenta (String str)
		{
			return append (MAGENTA, str);
		}


		private static class ColorAttr
		{
			private final String attr;
	
			public ColorAttr (String attr)
			{
				this.attr = attr;
			}
	

			public String toString ()
			{
				return attr;
			}
		}
	}



	//////////////////////////////////
	// Command-line completion method
	//////////////////////////////////



	////////////////////////////
	// Command handling classes
	////////////////////////////

	/** 
	 *  A generic command to be executed. Execution of the command
	 *  should be dispatched to the {@link #execute(java.lang.String)}
	 *  method after determining that the command is appropriate with
	 *  the {@link #matches(java.lang.String)} method.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	interface CommandHandler
	{
		/** 
		 *  @return  the name of the command
		 */
		public String getName ();


		/** 
		 *  @return  all the possible names of this command.
		 */
		public String [] getNames ();


		/** 
		 *  @return  the short help description for this command.
		 */
		public String getHelpText ();


		/** 
		 *  Check to see if the specified string can be dispatched to this
		 *  command.
		 *  
		 *  @param  line  the command line to check.
		 *  @return  true if this command can handle the specified line
		 */
		public boolean matches (String line);


		/** 
		 *  Execute the specified command.
		 *  
		 *  @param  line  the full command line to execute.
		 */
		public void execute (String line);
	}


	/** 
	 *  An abstract implementation of CommandHandler.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	abstract class AbstractCommandHandler
		implements CommandHandler
	{
		private final String name;
		private final String [] names;
		private final String helpText;

		public AbstractCommandHandler (String [] names, String helpText)
		{
			this.name = names [0];
			this.names = names;
			this.helpText = helpText;
		}


		public String getHelpText ()
		{
			return helpText;
		}


		public String getName ()
		{
			return this.name;
		}


		public String [] getNames ()
		{
			return this.names;
		}


		public boolean matches (String line)
		{
			if (line == null || line.length () == 0)
				return false;

			for (int i = 0; i < names.length; i++)
			{
				if (line.startsWith (names [i]))
					return true;
			}


			return false;
		}
	}


	/** 
	 *  A {@link Command} implementation that uses reflection to
	 *  determine the method to dispatch the command.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	class ReflectiveCommandHandler
		extends AbstractCommandHandler
	{
		ReflectiveCommandHandler (String [] cmds,
			String helpText)
		{
			super (cmds, helpText);
		}


		public void execute (String line)
		{
			try
			{
				command.getClass ().getMethod (getName (),
					new Class [] { String.class })
					.invoke (command, new Object [] { line });
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}
	}


	class MetaDataCommandHandler
		extends AbstractCommandHandler
	{
		MetaDataCommandHandler (String [] cmds, String helpText)
		{
			super (cmds, helpText);
		}


		public void execute (String line)
		{
			try
			{
				String [] parts = split (line);
				List params = new LinkedList (Arrays.asList (parts));
				params.remove (0);
				params.remove (0);


				Method [] m = con ().meta.getClass ().getMethods ();
				Set methodNames = new TreeSet ();
				Set methodNamesUpper = new TreeSet ();
				for (int i = 0; i < m.length; i++)
				{
					methodNames.add (m [i].getName ());
					methodNamesUpper.add (m [i].getName ().toUpperCase ());
				}

				if (!methodNamesUpper.contains (parts [1].toUpperCase ()))
				{
					error (loc ("no-such-method", parts [1]));
					error (loc ("possible-methods"));
					for (Iterator i = methodNames.iterator (); i.hasNext (); )
						error ("   " + i.next ());
					return;
				}

				Object res = Reflector.invoke (con ().meta, parts [1], params);

				if (res instanceof ResultSet)
				{
					ResultSet rs = (ResultSet)res;

					if (rs != null)
					{
						try
						{
							print (rs);
						}
						finally
						{
							rs.close ();
						}
					}
				}
				else if (res != null)
				{
					output (res.toString ());
				}
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}
	}



	//////////////////////////
	// Command methods follow
	//////////////////////////

	public class Commands
	{
		public void scan (String line)
		{
			TreeSet names = new TreeSet ();

			if (drivers == null)
				drivers = Arrays.asList (scanDrivers (line));

			output (loc ("divers-found-count", drivers.size ()));

			// unique the list
			for (Iterator i = drivers.iterator (); i.hasNext (); )
				names.add (((Driver)i.next ()).getClass ().getName ());

			for (Iterator i = names.iterator (); i.hasNext (); )
			{
				String name = i.next ().toString ();

				try
				{
					Driver driver = (Driver)Class.forName (name).newInstance ();
					String msg = (pad (name, 52).getMono ()
						+ "(v " + driver.getMajorVersion ()
						+ "." + driver.getMinorVersion () + ")"
						+ (driver.jdbcCompliant () ? "" : "[noncompliant]"));
					if (driver.jdbcCompliant ())
						output (msg);
					else
						output (color ().red (msg));
				}
				catch (Throwable t)
				{
					output (color ().red (name)); // error with driver
				}
			}
		}


		public void save (String line)
			throws IOException
		{
			opts.save ();
		}
	
	
		public void load (String line)
			throws IOException
		{
			opts.load ();
			output ("Loaded preferences from " + opts.rcFile);
		}
	
	
		public void config (String line)
		{
			try
			{
				Properties props = opts.toProperties ();
				Set keys = new TreeSet (props.keySet ());
				for (Iterator i = keys.iterator (); i.hasNext (); )
				{
					String key = (String)i.next ();
					output (color ()
						.green (pad (key, 20).getMono ())
						.append (props.getProperty (key)));
				}
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}
	
	
		public void set (String line)
		{
			if (line == null || line.trim ().equals ("set")
				|| line.length () == 0)
			{
				config (null);
				return;
			}
	
			String [] parts = split (line, 3, "Usage: set <key> <value>");
			if (parts == null)
				return;
	
			String key = parts [1];
			String value = parts [2];
			opts.set (key, value);
		}
	
	
		public void commit (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return;
			if (!(assertAutoCommit ()))
				return;
	
			con ().connection.commit ();
			showWarnings ();
			output ("Commit complete");
		}
	
	
		public void rollback (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return;
			if (!(assertAutoCommit ()))
				return;
	
			con ().connection.rollback ();
			showWarnings ();
			output ("Rollback complete");
		}
	
	
		public void autocommit (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return;
	
			if (line.endsWith ("on"))
			{
				con ().connection.setAutoCommit (true);
				output ("Autocommit is "
					+ (con ().connection.getAutoCommit () ? "on" : "off"));
			}
			else if (line.endsWith ("off"))
			{
				con ().connection.setAutoCommit (false);
				output ("Autocommit is "
					+ (con ().connection.getAutoCommit () ? "on" : "off"));
			}
			else
				error ("Usage: autocommit <on/off>");
	
			showWarnings ();
		}
	
	
		public void dbinfo (String line)
		{
			if (!(assertConnection ()))
				return;
	
			try
			{
				showWarnings ();
				int padlen = 50;
				output (pad ("allProceduresAreCallable", padlen)
					.append ("" + con ().meta.allProceduresAreCallable ()));
				output (pad ("allTablesAreSelectable", padlen)
					.append ("" + con ().meta.allTablesAreSelectable ()));
				output (pad ("dataDefinitionCausesTransactionCommit", padlen)
					.append (""
						+ con ().meta.dataDefinitionCausesTransactionCommit ()));
				output (pad ("dataDefinitionIgnoredInTransactions", padlen)
					.append (""
						+ con ().meta.dataDefinitionIgnoredInTransactions ()));
				output (pad ("doesMaxRowSizeIncludeBlobs", padlen)
					.append ("" + con ().meta.doesMaxRowSizeIncludeBlobs ()));
				output (pad ("getCatalogSeparator", padlen)
					.append ("" + con ().meta.getCatalogSeparator ()));
				output (pad ("getCatalogTerm", padlen)
					.append ("" + con ().meta.getCatalogTerm ()));
				output (pad ("getDatabaseProductName", padlen)
					.append ("" + con ().meta.getDatabaseProductName ()));
				output (pad ("getDatabaseProductVersion", padlen)
					.append ("" + con ().meta.getDatabaseProductVersion ()));
				output (pad ("getDefaultTransactionIsolation", padlen)
					.append (""
						+ con ().meta.getDefaultTransactionIsolation ()));
				output (pad ("getDriverMajorVersion", padlen)
					.append ("" + con ().meta.getDriverMajorVersion ()));
				output (pad ("getDriverMinorVersion", padlen)
					.append ("" + con ().meta.getDriverMinorVersion ()));
				output (pad ("getDriverName", padlen)
					.append ("" + con ().meta.getDriverName ()));
				output (pad ("getDriverVersion", padlen)
					.append ("" + con ().meta.getDriverVersion ()));
				output (pad ("getExtraNameCharacters", padlen)
					.append ("" + con ().meta.getExtraNameCharacters ()));
				output (pad ("getIdentifierQuoteString", padlen)
					.append ("" + con ().meta.getIdentifierQuoteString ()));
				output (pad ("getMaxBinaryLiteralLength", padlen)
					.append ("" + con ().meta.getMaxBinaryLiteralLength ()));
				output (pad ("getMaxCatalogNameLength", padlen)
					.append ("" + con ().meta.getMaxCatalogNameLength ()));
				output (pad ("getMaxCharLiteralLength", padlen)
					.append ("" + con ().meta.getMaxCharLiteralLength ()));
				output (pad ("getMaxColumnNameLength", padlen)
					.append ("" + con ().meta.getMaxColumnNameLength ()));
				output (pad ("getMaxColumnsInGroupBy", padlen)
					.append ("" + con ().meta.getMaxColumnsInGroupBy ()));
				output (pad ("getMaxColumnsInIndex", padlen)
					.append ("" + con ().meta.getMaxColumnsInIndex ()));
				output (pad ("getMaxColumnsInOrderBy", padlen)
					.append ("" + con ().meta.getMaxColumnsInOrderBy ()));
				output (pad ("getMaxColumnsInSelect", padlen)
					.append ("" + con ().meta.getMaxColumnsInSelect ()));
				output (pad ("getMaxColumnsInTable", padlen)
					.append ("" + con ().meta.getMaxColumnsInTable ()));
				output (pad ("getMaxConnections", padlen)
					.append ("" + con ().meta.getMaxConnections ()));
				output (pad ("getMaxCursorNameLength", padlen)
					.append ("" + con ().meta.getMaxCursorNameLength ()));
				output (pad ("getMaxIndexLength", padlen)
					.append ("" + con ().meta.getMaxIndexLength ()));
				output (pad ("getMaxProcedureNameLength", padlen)
					.append ("" + con ().meta.getMaxProcedureNameLength ()));
				output (pad ("getMaxRowSize", padlen)
					.append ("" + con ().meta.getMaxRowSize ()));
				output (pad ("getMaxSchemaNameLength", padlen)
					.append ("" + con ().meta.getMaxSchemaNameLength ()));
				output (pad ("getMaxStatementLength", padlen)
					.append ("" + con ().meta.getMaxStatementLength ()));
				output (pad ("getMaxStatements", padlen)
					.append ("" + con ().meta.getMaxStatements ()));
				output (pad ("getMaxTableNameLength", padlen)
					.append ("" + con ().meta.getMaxTableNameLength ()));
				output (pad ("getMaxTablesInSelect", padlen)
					.append ("" + con ().meta.getMaxTablesInSelect ()));
				output (pad ("getMaxUserNameLength", padlen)
					.append ("" + con ().meta.getMaxUserNameLength ()));
				output (pad ("getNumericFunctions", padlen)
					.append ("" + con ().meta.getNumericFunctions ()));
				output (pad ("getProcedureTerm", padlen)
					.append ("" + con ().meta.getProcedureTerm ()));
				output (pad ("getSchemaTerm", padlen)
					.append ("" + con ().meta.getSchemaTerm ()));
				output (pad ("getSearchStringEscape", padlen)
					.append ("" + con ().meta.getSearchStringEscape ()));
				output (pad ("getSQLKeywords", padlen)
					.append ("" + con ().meta.getSQLKeywords ()));
				output (pad ("getStringFunctions", padlen)
					.append ("" + con ().meta.getStringFunctions ()));
				output (pad ("getSystemFunctions", padlen)
					.append ("" + con ().meta.getSystemFunctions ()));
				output (pad ("getTimeDateFunctions", padlen)
					.append ("" + con ().meta.getTimeDateFunctions ()));
				output (pad ("getURL", padlen)
					.append ("" + con ().meta.getURL ()));
				output (pad ("getUserName", padlen)
					.append ("" + con ().meta.getUserName ()));
				output (pad ("isCatalogAtStart", padlen)
					.append ("" + con ().meta.isCatalogAtStart ()));
				output (pad ("isReadOnly", padlen)
					.append ("" + con ().meta.isReadOnly ()));
				output (pad ("nullPlusNonNullIsNull", padlen)
					.append ("" + con ().meta.nullPlusNonNullIsNull ()));
				output (pad ("nullsAreSortedAtEnd", padlen)
					.append ("" + con ().meta.nullsAreSortedAtEnd ()));
				output (pad ("nullsAreSortedAtStart", padlen)
					.append ("" + con ().meta.nullsAreSortedAtStart ()));
				output (pad ("nullsAreSortedHigh", padlen)
					.append ("" + con ().meta.nullsAreSortedHigh ()));
				output (pad ("nullsAreSortedLow", padlen)
					.append ("" + con ().meta.nullsAreSortedLow ()));
				output (pad ("storesLowerCaseIdentifiers", padlen)
					.append ("" + con ().meta.storesLowerCaseIdentifiers ()));
				output (pad ("storesLowerCaseQuotedIdentifiers", padlen)
					.append (""
						+ con ().meta.storesLowerCaseQuotedIdentifiers ()));
				output (pad ("storesMixedCaseIdentifiers", padlen)
					.append (""
						+ con ().meta.storesMixedCaseIdentifiers ()));
				output (pad ("storesMixedCaseQuotedIdentifiers", padlen)
					.append (""
						+ con ().meta.storesMixedCaseQuotedIdentifiers ()));
				output (pad ("storesUpperCaseIdentifiers", padlen)
					.append ("" + con ().meta.storesUpperCaseIdentifiers ()));
				output (pad ("storesUpperCaseQuotedIdentifiers", padlen)
					.append (""
						+ con ().meta.storesUpperCaseQuotedIdentifiers ()));
				output (pad ("supportsAlterTableWithAddColumn", padlen)
					.append (""
						+ con ().meta.supportsAlterTableWithAddColumn ()));
				output (pad ("supportsAlterTableWithDropColumn", padlen)
					.append (""
						+ con ().meta.supportsAlterTableWithDropColumn ()));
				output (pad ("supportsANSI92EntryLevelSQL", padlen)
					.append ("" + con ().meta.supportsANSI92EntryLevelSQL ()));
				output (pad ("supportsANSI92FullSQL", padlen)
					.append ("" + con ().meta.supportsANSI92FullSQL ()));
				output (pad ("supportsANSI92IntermediateSQL", padlen)
					.append ("" + con ().meta.supportsANSI92IntermediateSQL ()));
				output (pad ("supportsBatchUpdates", padlen)
					.append ("" + con ().meta.supportsBatchUpdates ()));
				output (pad ("supportsCatalogsInDataManipulation", padlen)
					.append (""
						+ con ().meta.supportsCatalogsInDataManipulation ()));
				output (pad ("supportsCatalogsInIndexDefinitions", padlen)
					.append (""
						+ con ().meta.supportsCatalogsInIndexDefinitions ()));
				output (pad ("supportsCatalogsInPrivilegeDefinitions", padlen)
					.append (""
						+ con ().meta.supportsCatalogsInPrivilegeDefinitions()));
				output (pad ("supportsCatalogsInProcedureCalls", padlen)
					.append (""
						+ con ().meta.supportsCatalogsInProcedureCalls ()));
				output (pad ("supportsCatalogsInTableDefinitions", padlen)
					.append (""
						+ con ().meta.supportsCatalogsInTableDefinitions ()));
				output (pad ("supportsColumnAliasing", padlen)
					.append ("" + con ().meta.supportsColumnAliasing ()));
				output (pad ("supportsConvert", padlen)
					.append ("" + con ().meta.supportsConvert ()));
				output (pad ("supportsCoreSQLGrammar", padlen)
					.append ("" + con ().meta.supportsCoreSQLGrammar ()));
				output (pad ("supportsCorrelatedSubqueries", padlen)
					.append ("" + con ().meta.supportsCorrelatedSubqueries ()));
				output (pad (
					"supportsDataDefinitionAndDataManipulationTransactions", padlen)
					.append ("" + con ().meta.supportsDataDefinitionAndDataManipulationTransactions ())
						);
				output (pad ("supportsDataManipulationTransactionsOnly", padlen)
					.append ("" + con ().meta.supportsDataManipulationTransactionsOnly ()));
				output (pad ("supportsDifferentTableCorrelationNames", padlen)
					.append ("" + con ().meta.supportsDifferentTableCorrelationNames ()));
				output (pad ("supportsExpressionsInOrderBy", padlen)
					.append ("" + con ().meta.supportsExpressionsInOrderBy ()));
				output (pad ("supportsExtendedSQLGrammar", padlen)
					.append ("" + con ().meta.supportsExtendedSQLGrammar ()));
				output (pad ("supportsFullOuterJoins", padlen)
					.append ("" + con ().meta.supportsFullOuterJoins ()));
				output (pad ("supportsGroupBy", padlen)
					.append ("" + con ().meta.supportsGroupBy ()));
				output (pad ("supportsGroupByBeyondSelect", padlen)
					.append ("" + con ().meta.supportsGroupByBeyondSelect ()));
				output (pad ("supportsGroupByUnrelated", padlen)
					.append ("" + con ().meta.supportsGroupByUnrelated ()));
				output (pad ("supportsIntegrityEnhancementFacility", padlen)
					.append ("" + con ().meta.supportsIntegrityEnhancementFacility ()));
				output (pad ("supportsLikeEscapeClause", padlen)
					.append ("" + con ().meta.supportsLikeEscapeClause ()));
				output (pad ("supportsLimitedOuterJoins", padlen)
					.append ("" + con ().meta.supportsLimitedOuterJoins ()));
				output (pad ("supportsMinimumSQLGrammar", padlen)
					.append ("" + con ().meta.supportsMinimumSQLGrammar ()));
				output (pad ("supportsMixedCaseIdentifiers", padlen)
					.append ("" + con ().meta.supportsMixedCaseIdentifiers ()));
				output (pad ("supportsMixedCaseQuotedIdentifiers", padlen)
					.append ("" + con ().meta.supportsMixedCaseQuotedIdentifiers ()));
				output (pad ("supportsMultipleResultSets", padlen)
					.append ("" + con ().meta.supportsMultipleResultSets ()));
				output (pad ("supportsMultipleTransactions", padlen)
					.append ("" + con ().meta.supportsMultipleTransactions ()));
				output (pad ("supportsNonNullableColumns", padlen)
					.append ("" + con ().meta.supportsNonNullableColumns ()));
				output (pad ("supportsOpenCursorsAcrossCommit", padlen)
					.append ("" + con ().meta.supportsOpenCursorsAcrossCommit ()));
				output (pad ("supportsOpenCursorsAcrossRollback", padlen)
					.append ("" + con ().meta.supportsOpenCursorsAcrossRollback ()));
				output (pad ("supportsOpenStatementsAcrossCommit", padlen)
					.append ("" + con ().meta.supportsOpenStatementsAcrossCommit ()));
				output (pad ("supportsOpenStatementsAcrossRollback", padlen)
					.append ("" + con ().meta.supportsOpenStatementsAcrossRollback ()));
				output (pad ("supportsOrderByUnrelated", padlen)
					.append ("" + con ().meta.supportsOrderByUnrelated ()));
				output (pad ("supportsOuterJoins", padlen)
					.append ("" + con ().meta.supportsOuterJoins ()));
				output (pad ("supportsPositionedDelete", padlen)
					.append ("" + con ().meta.supportsPositionedDelete ()));
				output (pad ("supportsPositionedUpdate", padlen)
					.append ("" + con ().meta.supportsPositionedUpdate ()));
				output (pad ("supportsSchemasInDataManipulation", padlen)
					.append ("" + con ().meta.supportsSchemasInDataManipulation ()));
				output (pad ("supportsSchemasInIndexDefinitions", padlen)
					.append ("" + con ().meta.supportsSchemasInIndexDefinitions ()));
				output (pad ("supportsSchemasInPrivilegeDefinitions", padlen)
					.append ("" + con ().meta.supportsSchemasInPrivilegeDefinitions ()));
				output (pad ("supportsSchemasInProcedureCalls", padlen)
					.append ("" + con ().meta.supportsSchemasInProcedureCalls ()));
				output (pad ("supportsSchemasInTableDefinitions", padlen)
					.append ("" + con ().meta.supportsSchemasInTableDefinitions ()));
				output (pad ("supportsSelectForUpdate", padlen)
					.append ("" + con ().meta.supportsSelectForUpdate ()));
				output (pad ("supportsStoredProcedures", padlen)
					.append ("" + con ().meta.supportsStoredProcedures ()));
				output (pad ("supportsSubqueriesInComparisons", padlen)
					.append ("" + con ().meta.supportsSubqueriesInComparisons ()));
				output (pad ("supportsSubqueriesInExists", padlen)
					.append ("" + con ().meta.supportsSubqueriesInExists ()));
				output (pad ("supportsSubqueriesInIns", padlen)
					.append ("" + con ().meta.supportsSubqueriesInIns ()));
				output (pad ("supportsSubqueriesInQuantifieds", padlen)
					.append ("" + con ().meta.supportsSubqueriesInQuantifieds ()));
				output (pad ("supportsTableCorrelationNames", padlen)
					.append ("" + con ().meta.supportsTableCorrelationNames ()));
				output (pad ("supportsTransactions", padlen)
					.append ("" + con ().meta.supportsTransactions ()));
				output (pad ("supportsUnion", padlen)
					.append ("" + con ().meta.supportsUnion ()));
				output (pad ("supportsUnionAll", padlen)
					.append ("" + con ().meta.supportsUnionAll ()));
				output (pad ("usesLocalFilePerTable", padlen)
					.append ("" + con ().meta.usesLocalFilePerTable ()));
				output (pad ("usesLocalFiles", padlen)
					.append ("" + con ().meta.usesLocalFiles ()));
			}
			catch (SQLException sqle)
			{
				handleException (sqle);
				return;
			}
	
			showWarnings ();
		}
	
	
		public void verbose (String line)
		{
			set ("set verbose true");
			output ("verbose: on");
		}
	
	
		public void brief (String line)
		{
			set ("set verbose false");
			output ("verbose: off");
		}
	
	
		public void isolation (String line)
			throws SQLException
		{
			if (!(assertConnection ()))
				return;
	
			int i;
	
			if (line.endsWith ("TRANSACTION_NONE"))
				i = Connection.TRANSACTION_NONE;
			else if (line.endsWith ("TRANSACTION_READ_COMMITTED"))
				i = Connection.TRANSACTION_READ_COMMITTED;
			else if (line.endsWith ("TRANSACTION_READ_UNCOMMITTED"))
				i = Connection.TRANSACTION_READ_UNCOMMITTED;
			else if (line.endsWith ("TRANSACTION_REPEATABLE_READ"))
				i = Connection.TRANSACTION_REPEATABLE_READ;
			else if (line.endsWith ("TRANSACTION_SERIALIZABLE"))
				i = Connection.TRANSACTION_SERIALIZABLE;
			else
			{
				error ("Usage: isolation <TRANSACTION_NONE "
					+ "| TRANSACTION_READ_COMMITTED "
					+ "| TRANSACTION_READ_UNCOMMITTED "
					+ "| TRANSACTION_REPEATABLE_READ "
					+ "| TRANSACTION_SERIALIZABLE>");
				return;
			}
	
			con ().connection.setTransactionIsolation (i);
	
			int isol = con ().connection.getTransactionIsolation ();
			final String isoldesc;
			switch (i)
			{
				case Connection.TRANSACTION_NONE:
					isoldesc = "TRANSACTION_NONE";
					break;
				case Connection.TRANSACTION_READ_COMMITTED:
					isoldesc = "TRANSACTION_READ_COMMITTED";
					break;
				case Connection.TRANSACTION_READ_UNCOMMITTED:
					isoldesc = "TRANSACTION_READ_UNCOMMITTED";
					break;
				case Connection.TRANSACTION_REPEATABLE_READ:
					isoldesc = "TRANSACTION_REPEATABLE_READ";
					break;
				case Connection.TRANSACTION_SERIALIZABLE:
					isoldesc = "TRANSACTION_SERIALIZABLE";
					break;
				default:
					isoldesc = "UNKNOWN";
			}
	
			output ("Transaction isolation set to: " + isoldesc);
		}
	
	
		public void sql (String line)
		{
			if (line == null || line.length () == 0)
				return;
	
			// get rid of the common delimiter
			if (line.endsWith (";"))
				line = line.substring (0, line.length () - 1);
	
			if (!(assertConnection ()))
				return;
	
			String sql = line;
			
			if (sql.startsWith (COMMAND_PREFIX))
				sql = sql.substring (1);
	
			if (sql.startsWith ("sql"))
				sql = sql.substring (4);
	
			try
			{
				Statement stmnt = con ().connection.createStatement ();
				try
				{
					boolean ret = stmnt.execute (sql);
					showWarnings ();
					int count = stmnt.getUpdateCount ();
					if (count > -1)
						output (loc ("rows-affected", count));
	
					if (ret)
					{
						ResultSet rs = stmnt.getResultSet ();
						try
						{
							print (rs);
						}
						finally
						{
							rs.close ();
						}
					}
				}
				finally
				{
					stmnt.close ();
				}
			}
			catch (Exception e)
			{
				handleException (e);
			}
	
			showWarnings ();
		}
	
	
		public void quit (String line)
		{
			exit = true;
	
			close (null);
		}
	
	
		public void close (String line)
		{
			if (con () == null)
				return;
	
			try
			{
				if (!(con ().connection.isClosed ()))
				{
					output (loc ("closing",
						con ().connection.getClass ().getName ()));
					con ().connection.close ();
				}
			}
			catch (Exception e)
			{
				handleException (e);
			}
	
			connections.remove ();
		}
	
	
		public void connect (String line)
		{
			String example = "";
			example += "Usage: connect <url> <username> <password> [driver]"
				+ sep;
	
			String [] parts = split (line);
			if (parts == null)
				return;
	
			if (parts.length < 2)
			{
				error (example);
				return;
			}
	
			String url = parts.length < 2 ? "" : parts [1];
			String user = parts.length < 3 ? "" : parts [2];
			String pass = parts.length < 4 ? "" : parts [3];
			String driver = parts.length < 5 ? "" : parts [4];
	
			try
			{
				// clear old completions
				completions.clear ();
	
				connections.setConnection (
					new DatabaseConnection (driver, url, user, pass));
				con ().getConnection ();
	
				setCompletions ();
			}
			catch (SQLException sqle)
			{
				handleException (sqle);
			}
			catch (IOException ioe)
			{
				handleException (ioe);
			}
		}
	
	
		public void rehash (String line)
		{
			try
			{
				if (!(assertConnection ()))
					return;
	
				completions.clear ();
				if (con () != null)
					con ().setCompletions (false);
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}


		/** 
		 *  List the current connections
		 */
		public void list (String line)
		{
			int index = 0;
			output (loc ("active-connections", connections.size ()));

			for (Iterator i = connections.iterator (); i.hasNext (); index++)
			{
				DatabaseConnection c = (DatabaseConnection)i.next ();
				boolean closed = false;
				try
				{
					closed = c.connection.isClosed ();
				}
				catch (Exception e)
				{
					closed = true;
				}

				output (pad (" #" + index + "", 5).getMono ()
					+ pad (closed ? loc ("closed") : loc ("open"), 9).getMono ()
					+ c.url);
			}
		}


		public void all (String line)
		{
			int index = connections.getIndex ();

			for (int i = 0; i < connections.size (); i++)
			{
				connections.setIndex (i);
				output (loc ("executing-con", con ()));
				sql (line.substring ("all ".length ()));
			}

			// restore index
			connections.setIndex (index);
		}


		public void go (String line)
		{
			String [] parts = split (line, 2, "Usage: go <connection index>");
			if (parts == null)
				return;

			int index = Integer.parseInt (parts [1]);
			if (!(connections.setIndex (index)))
			{
				error ("Invalid connection: " + index);
				list (""); // list the current connections
			}
		}
	
	
		/** 
		 *  Save or stop saving a script to a file
		 */
		public void script (String line)
		{
			if (script == null)
				startScript (line);
			else
				stopScript (line);
		}


		/** 
		 *  Stop writing to the script file and close the script.
		 */
		private void stopScript (String line)
		{
			try
			{
				script.close ();
			}
			catch (Exception e)
			{
				handleException (e);
			}

			output ("Script closed: " + script);
			output ("Enter \"run " + script + "\" to replay it");
			script = null;
		}


		/** 
		 *  Start writing to the specified script file.
		 */
		private void startScript (String line)
		{
			if (script != null)
			{
				error ("Script is already running: " + script);
				error ("Enter \"script\" with no arguments to stop it.");
				return;
			}


			String [] parts = split (line, 2, "Usage: script <filename>");
			if (parts == null)
				return;
	
			try
			{
				script = new Script (parts [1]);
				output ("Saving command script to: " + script);
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}


		/** 
		 *  Run a script from the specified file.
		 */
		public void run (String line)
		{
			String [] parts = split (line, 2, "Usage: run <scriptfile>");
			if (parts == null)
				return;
	
			try
			{
				BufferedReader reader = new BufferedReader (new FileReader (
					parts [1]));
				try
				{
					String cmd = null;
					while ((cmd = reader.readLine ()) != null)
						dispatch (cmd);
				}
				finally
				{
					reader.close ();
				}
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}
	
	
		public void describe (String line)
			throws SQLException
		{
			String [] table = split (line, 2, "Usage: describe <table name>");
			if (table == null)
				return;
	
			ResultSet rs;
	
			if (table [1].equals ("tables"))
			{
				rs = getTables ();
			}
			else
			{
				rs = getColumns (table [1]);
			}
	
			if (rs == null)
				return;
	
			if (rs.isAfterLast ())
			{
				error ("No entries found for " + table [1]);
				return;
			}
	
			print (rs);
			rs.close ();
		}
	
	
		public void help (String line)
		{
			String [] parts = split (line);
			String cmd = parts.length > 1 ? parts [1] : "";
			int count = 0;
	
			for (int i = 0; i < commands.length; i++)
			{
				if (cmd.length () == 0 ||
					Arrays.asList (commands [i].getNames ()).contains (cmd))
				{
					output (pad (commands [i].getName (), 20)
						+ wrap (commands [i].getHelpText (), 60, 20));
				}
			}
	
			if (cmd.length () == 0)
			{
				output ("");
				output (loc ("comments", APP_AUTHOR_EMAIL));
			}
		}
	}
	
	


	private void setCompletions ()
		throws SQLException, IOException
	{
		if (con () != null)
			con ().setCompletions (opts.getFastConnect ());
	}


	/** 
	 *  Completor for SQLLine. It dispatches to sub-completors based on the
	 *  current arguments.
	 *  
	 *  @author  <a href="mailto:marc@apocalypse.org">Marc Prud'hommeaux</a>
	 */
	class SQLLineCompletor
		implements Completor
	{
		public int complete (String buf, int pos, List cand)
		{
			if (buf != null && buf.startsWith (COMMAND_PREFIX)
				&& !buf.startsWith (COMMAND_PREFIX + "all")
				&& !buf.startsWith (COMMAND_PREFIX + "sql"))
			{
				return sqlLineCommandCompletor.complete (buf, pos, cand);
			}
			else
			{
				if (con () != null && con ().sqlLineSQLCompletor != null)
					return con ().sqlLineSQLCompletor.complete (buf, pos, cand);
				else
					return -1;
			}
		}
	}


	class SQLLineCommandCompletor
		extends SimpleCompletor
	{
		public SQLLineCommandCompletor ()
		{
			super ("");

			for (int i = 0; i < commands.length; i++)
			{
				String [] cmds = commands [i].getNames ();
				for (int j = 0; cmds != null && j < cmds.length; j++)
				{
					addCandidateString (COMMAND_PREFIX + cmds [j]);
				}
			}
		}
	}


	class SQLLineSQLCompletor
		extends SimpleCompletor
	{
		public SQLLineSQLCompletor (boolean skipmeta)
			throws IOException, SQLException
		{
			super (new String [0]);

			Set completions = new TreeSet ();

			// add the default SQL completions
			String keywords = new BufferedReader (new InputStreamReader (
				SQLLineSQLCompletor.class.getResourceAsStream (
					"sql-keywords.properties"))).readLine ();

			// now add the keywords from the current connection

			try 
			{
				keywords += "," + con ().meta.getSQLKeywords ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getStringFunctions ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getNumericFunctions ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getSystemFunctions ();
			}
			catch (Throwable t) { }
			try 
			{
				keywords += "," + con ().meta.getTimeDateFunctions ();
			}
			catch (Throwable t) { }


			// also allow lower-case versions of all the keywords
			keywords += "," + keywords.toLowerCase ();

			for (StringTokenizer tok = new StringTokenizer (keywords, ", ");
				tok.hasMoreTokens ();
				completions.add (tok.nextToken ()));

			// now add the tables and columns from the current connection
			if (!(skipmeta))
			{
				String [] columns = getColumnNames (con ().meta);
				for (int i = 0; columns != null && i < columns.length; i++)
					completions.add (columns [i++]);
			}

			// set the Strings that will be completed
			setCandidateStrings (
				(String [])completions.toArray (new String [0]));
		}
	}


	private static class Connections
	{
		private final List connections = new ArrayList ();
		private int index = -1;

		public DatabaseConnection current ()
		{
			if (index != -1)
				return (DatabaseConnection)connections.get (index);

			return null;
		}


		public int size ()
		{
			return connections.size ();
		}


		public Iterator iterator ()
		{
			return connections.iterator ();
		}


		public void remove ()
		{
			if (index != -1)
			{
				connections.remove (index);
			}

			while (index >= connections.size ())
				index--;
		}


		public void addConnection (DatabaseConnection connection)
		{
			connections.add (connection);
		}


		public void setConnection (DatabaseConnection connection)
		{
			if (connections.indexOf (connection) == -1)
				connections.add (connection);

			index = connections.indexOf (connection);
		}


		public int getIndex ()
		{
			return index;
		}


		public boolean setIndex (int index)
		{
			if (index < 0 || index >= connections.size ())
				return false;

			this.index = index;
			return true;
		}
	}


	private class DatabaseConnection
	{
		Connection connection;
		DatabaseMetaData meta;
		private final String driver;
		private final String url;
		private final String username;
		private final String password;
		private Schema schema = null;
		private Completor sqlLineSQLCompletor = null;


		public DatabaseConnection (String driver, String url,
			String username, String password)
			throws SQLException
		{
			this.driver = driver;
			this.url = url;
			this.username = username;
			this.password = password;
		}


		public String toString ()
		{
			return url + "";
		}


		private void setCompletions (boolean skipmeta)
			throws SQLException, IOException
		{
			// setup the completor for the database
			sqlLineSQLCompletor = new ArgumentCompletor (
				new SQLLineSQLCompletor (skipmeta));
		}


		/** 
		 *  Connection to the specified data source.
		 *  
		 *  @param  driver		the driver class
		 *  @param  url			the connection URL
		 *  @param  username	the username
		 *  @param  password	the password
		 */
		void connect ()
			throws SQLException
		{
			try
			{
				if (driver != null && driver.length () != 0)
					Class.forName (driver);
			}
			catch (ClassNotFoundException cnfe)
			{
				handleException (cnfe);
				return;
			}
	
			try
			{
				close ();
			}
			catch (Exception e)
			{
				handleException (e);
			}

			connection = DriverManager.getConnection (url, username, password);
			meta = connection.getMetaData ();
	
			try
			{
				output ("  Connected to: " + meta.getDatabaseProductName ()
					+ " (version " + meta.getDatabaseProductVersion () + ")");
				output ("  Driver: " + meta.getDriverName ()
					+ " (version " + meta.getDriverVersion () + ")");
	
				connection.setAutoCommit (opts.getAutoCommit ());
				output ("  Autocommit: " + connection.getAutoCommit ());
	
				try
				{
					command.isolation ("isolation: " + opts.getIsolation ());
				}
				catch (Exception e)
				{
					handleException (e);
				}
			}
			catch (SQLException sqle)
			{
				handleException (sqle);
			}
		}
	

		public Connection getConnection ()
			throws SQLException
		{
			if (connection != null)
				return connection;

			connect ();

			return connection;
		}


		public void reconnect ()
			throws Exception
		{
			close ();
			getConnection ();
		}


		public void close ()
		{
			try
			{
				try
				{
					if (connection != null && !connection.isClosed ())
					{
						output ("Closing: " + connection);
						connection.close ();
					}
				}
				catch (Exception e)
				{
					handleException (e);
				}
			}
			finally
			{
				connection = null;
				meta = null;
			}
		}


		Schema getSchema ()
		{
			if (schema == null)
				schema = new Schema ();

			return schema;
		}


		class Schema
		{
			private Table [] tables = null;


			Table [] getTables ()
			{
				if (tables == null)
				{
				}

				return tables;
			}


			Table getTable (String name)
			{
				Table [] t = getTables ();
				for (int i = 0; t != null && i < t.length; i++)
				{
					if (name.equalsIgnoreCase (t [i].getName ()))
						return t [i];
				}

				return null;
			}


			class Table
			{
				String name;
				Column [] columns;


				public String getName ()
				{
					return name;
				}


				class Column
				{
					String name;
					boolean isPrimaryKey;


					public Column (String name)
					{
					}
				}
			}
		}
	}


	class Opts
	{
		private boolean color = true;
		private boolean showHeader = true;
		private int headerInterval = 100;
		private boolean fastConnect = true;
		private boolean autoCommit = true;
		private boolean verbose = true;
		private boolean showWarnings = false;
		private int maxWidth = Terminal.setupTerminal ().getTerminalWidth ();
		private int maxColumnWidth = 15;
		private String isolation = "TRANSACTION_REPEATABLE_READ";

		public static final String PROPERTY_PREFIX = "sqlline.";

		private String rcFile = System.getProperty ("sqlline.rcfile",
			new File (System.getProperty ("user.home"), ".sqllinerc")
				.getAbsolutePath ());
		private String historyFile = System.getProperty ("sqlline.historyfile",
			new File (System.getProperty ("user.home"), ".sqlline_history")
				.getAbsolutePath ());


		public Opts (Properties props)
		{
			loadProperties (props);
		}


		public void save ()
			throws IOException
		{
			OutputStream out = new FileOutputStream (rcFile);
			save (out);
			out.close ();
		}


		public void save (OutputStream out)
			throws IOException
		{
			try
			{
				toProperties ().store (out, new java.util.Date () + "");
			}
			catch (Exception e)
			{
				handleException (e);
			}
		}


		public Properties toProperties ()
			throws IllegalAccessException, InvocationTargetException
		{
			Properties props = new Properties ();

			// get all the values from getXXX methods
			Method [] m = getClass ().getDeclaredMethods ();
			for (int i = 0; m != null && i < m.length; i++)
			{
				if (!(m [i].getName ().startsWith ("get")))
					continue;

				if (m [i].getParameterTypes ().length != 0)
					continue;

				String propName = m [i].getName ().substring (3).toLowerCase ();
				Object val = m [i].invoke (this, null);
				if (val != null)
					props.setProperty (propName, val.toString ());
			}

			debug ("properties: " + props.toString ());
			return props;
		}


		public void load ()
			throws IOException
		{
			InputStream in = new FileInputStream (rcFile);
			load (in);
			in.close ();
		}


		public void load (InputStream fin)
			throws IOException
		{
			Properties p = new Properties ();
			p.load (fin);
			loadProperties (p);
		}


		public void loadProperties (Properties props)
		{
			for (Iterator i = props.keySet ().iterator (); i.hasNext (); )
			{
				String key = i.next ().toString ();
				if (key.startsWith (PROPERTY_PREFIX))
				{
					set (key.substring (PROPERTY_PREFIX.length ()),
						props.getProperty (key));
				}
			}
		}


		public void set (String key, String value)
		{
			set (key, value, false);
		}


		public void set (String key, String value, boolean quiet)
		{
			try
			{
				Reflector.invoke (this, "set" + key, new Object [] { value });
			}
			catch (Exception e)
			{
				if (!quiet)
					error ("Error setting configuration: " + key + ": " + e);
			}
		}


		public void setFastConnect (boolean fastConnect)
		{
			this.fastConnect = fastConnect;
		}


		public boolean getFastConnect ()
		{
			return this.fastConnect;
		}




		public void setAutoCommit (boolean autoCommit)
		{
			this.autoCommit = autoCommit;
		}


		public boolean getAutoCommit ()
		{
			return this.autoCommit;
		}




		public void setVerbose (boolean verbose)
		{
			this.verbose = verbose;
		}


		public boolean getVerbose ()
		{
			return this.verbose;
		}




		public void setShowWarnings (boolean showWarnings)
		{
			this.showWarnings = showWarnings;
		}


		public boolean getShowWarnings ()
		{
			return this.showWarnings;
		}




		public void setMaxWidth (int maxWidth)
		{
			this.maxWidth = maxWidth;
		}


		public int getMaxWidth ()
		{
			return this.maxWidth;
		}




		public void setMaxColumnWidth (int maxColumnWidth)
		{
			this.maxColumnWidth = maxColumnWidth;
		}


		public int getMaxColumnWidth ()
		{
			return this.maxColumnWidth;
		}




		public void setIsolation (String isolation)
		{
			this.isolation = isolation;
		}


		public String getIsolation ()
		{
			return this.isolation;
		}


		public void setHistoryFile (String historyFile)
		{
			this.historyFile = historyFile;
		}


		public String getHistoryFile ()
		{
			return this.historyFile;
		}




		public void setColor (boolean color)
		{
			this.color = color;
		}


		public boolean getColor ()
		{
			return this.color;
		}



		public void setShowHeader (boolean showHeader)
		{
			this.showHeader = showHeader;
		}


		public boolean getShowHeader ()
		{
			return this.showHeader;
		}


		public void setHeaderInterval (int headerInterval)
		{
			this.headerInterval = headerInterval;
		}


		public int getHeaderInterval ()
		{
			return this.headerInterval;
		}
	}


	static class Reflector
	{
		public static Object invoke (Object on, String method, Object [] args)
			throws InvocationTargetException, IllegalAccessException,
				ClassNotFoundException
		{
			return invoke (on, method, Arrays.asList (args));
		}


		public static Object invoke (Object on, String method, List args)
			throws InvocationTargetException, IllegalAccessException,
				ClassNotFoundException
		{
			Class c = on.getClass ();
			List candidateMethods = new LinkedList ();

			Method [] m = c.getMethods ();
			for (int i = 0; i < m.length; i++)
			{
				if (m [i].getName ().equalsIgnoreCase (method))
					candidateMethods.add (m [i]);
			}

			if (candidateMethods.size () == 0)
				throw new IllegalArgumentException ("No method matching "
					+ "\"" + method + "\" was found in "
					+ c.getName ());

			for (Iterator i = candidateMethods.iterator (); i.hasNext (); )
			{
				Method meth = (Method)i.next ();
				Class [] ptypes = meth.getParameterTypes ();
				if (!(ptypes.length == args.size ()))
					continue;

				Object [] converted = convert (args, ptypes);
				if (converted == null)
					continue;

				return meth.invoke (on, converted);
			}

			return null;
		}


		public static Object [] convert (List objects, Class [] toTypes)
			throws ClassNotFoundException
		{
			Object [] converted = new Object [objects.size ()];
			for (int i = 0; i < converted.length; i++)
				converted [i] = convert (objects.get (i), toTypes [i]);
			return converted;
		}


		public static Object convert (Object ob, Class toType)
			throws ClassNotFoundException
		{
			if (ob == null || ob.toString ().equals ("null"))
				return null;

			if (toType == String.class)
				return new String (ob.toString ());
			else if (toType == Byte.class || toType == byte.class)
				return new Byte (ob.toString ());
			else if (toType == Character.class || toType == char.class)
				return new Character (ob.toString ().charAt (0));
			else if (toType == Short.class || toType == short.class)
				return new Short (ob.toString ());
			else if (toType == Integer.class || toType == int.class)
				return new Integer (ob.toString ());
			else if (toType == Long.class || toType == long.class)
				return new Long (ob.toString ());
			else if (toType == Double.class || toType == double.class)
				return new Double (ob.toString ());
			else if (toType == Float.class || toType == float.class)
				return new Float (ob.toString ());
			else if (toType == Boolean.class || toType == boolean.class)
				return new Boolean (ob.toString ().equals ("true")
					|| ob.toString ().equals (true + "")
					|| ob.toString ().equals ("1")
					|| ob.toString ().equals ("on")
					|| ob.toString ().equals ("yes"));
			else if (toType == Class.class)
				return Class.forName (ob.toString ());

			return null;
		}
	}


	public class Script
	{
		final File file;
		final PrintWriter out;

		public Script (String filename)
			throws IOException
		{
			file = new File (filename);
			out = new PrintWriter (new FileWriter (file));
		}


		public String toString ()
		{
			return file.getAbsolutePath ();
		}


		public void addCommand (String command)
		{
			out.println (command);
		}


		public void close ()
			throws IOException
		{
			out.close ();
		}
	}
}

