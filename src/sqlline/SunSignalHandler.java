/*
 *	Copyright (c) 2004 Marc Prud'hommeaux
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

import java.sql.*;
import sun.misc.*;

/**
 *  A signal handler for SqlLine which interprets Ctrl+C as a
 *  request to cancel the currently executing query.  Adapted from
 *  <a href="http://www.smotricz.com/kabutz/Issue043.html">TJSN</a>.
 */
class SunSignalHandler implements SqlLineSignalHandler, SignalHandler
{
	private Statement stmt = null;
    
	SunSignalHandler ()
	{
		Signal.handle (new Signal ("INT"), this);
	}

	// implement SqlLineSignalHandler
	public void setStmt (Statement stmt)
	{
		this.stmt = stmt;
	}

	// implement sun.misc.SignalHandler
	public void handle (Signal sig)
	{
		try
		{
			if (stmt != null)
				stmt.cancel ();
		}
		catch (SQLException ex)
		{
			// ignore?
		}
	}
}
