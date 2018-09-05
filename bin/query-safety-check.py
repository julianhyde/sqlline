#!/bin/python
# Description: Nagios/Icinga plugin for Hiveserver2 checks
#	       Uses sqlline. Intended for Tidal jobs
#		   This script does not depend on external libraries so
#		   it can be used with Tidal.
# Metrics:
#	Returns query completion time in seconds	

import argparse
import getpass
import logging
import os
import random
import re
import string
import subprocess
import sys
import time

# Notes
# probe seems to be a special function, required

class QuerySafetyCheck():
	""" Construct the class for Icinga """
	def __init__(self, database, adserver, hostname, mode, PASSWORD, port, query, queryfile, realm, username):
		self.adserver = adserver
		self.database = database
		self.hostname = hostname
		self.mode = mode
		self.PASSWORD = PASSWORD
		self.port = port
		self.query = query
		self.queryfile = queryfile
		self.realm = realm
		self.username = username

	def execute_sqlline(self, execute_type):
		"""Executes the query of file"""

		if self.mode == "kerberos":
			jdbc = '"jdbc:hive2://' + self.hostname + ':' + self.port \
				+ '/;AuthMech=1;KrbHostFQDN=' + self.adserver + ';KrbServiceName=hive;KrbHostFQDN=' \
				+ self.hostname + ';KrbRealm=' + self.realm + '"'
		elif self.mode == "ssl":
			jdbc = '"jdbc:hive2://' + self.hostname + ':' + self.port + '/;ssl=1;AuthMech=3"'

		logging.debug("JDBC used: " + str(jdbc))

		# sqlline does not support the beeline "-e" parameter. Build a file instead
		if execute_type == "file":
			queryfile = self.queryfile
		elif execute_type == "query":
			queryfile = '/tmp/' + ''.join(random.choice(string.ascii_lowercase) for _ in range(10))
			with open(queryfile, 'w') as q:
				q.write('USE ' + self.database + ';\n')
				q.write(self.query + ';\n')
		else:
			sys.exit("Invalid execution type specified")	

		# Execute sqlline
		logging.info("Execute sqlline")
		sqlline_cmd_stream = subprocess.Popen(["sqlline", "-u", jdbc, "-f", \
			queryfile, "-n", self.username, "-p", self.PASSWORD], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

		# Poll for status on the process to check if it's running
		try:	
			logging.info("Getting stdout/stderr")
			stdout, stderr = sqlline_cmd_stream.communicate()
			logging.info("Retrieved stdout/stderr")
		except:
			logging.error('Process was killed by timeout.')
			raise
		finally:
			if sqlline_cmd_stream.poll() is None:
				logging.debug("Poll is none, ending process")
				sqlline_cmd_stream.kill()
				stdout, stderr = sqlline_cmd_stream.communicate()

		logging.info("Analyzing output")
		# Parse stdout and stderr for metrics
		# Right now, stats are trapper in stderr
		perfstat = 0
		if stdout:
			logging.info("...checking stdout")
			# Iterate for debugging
			for line in stdout.split('\n'):
				logging.debug(line)
				norows_match = re.search('.*No rows affected.*',line)
				rowselect_match = re.search('.*rows selected.*',line)
				if norows_match:
					perfstat += float(re.sub('.*No rows affected \(', '', line).strip(') seconds'))
				elif rowselect_match:
					perfstat += float(re.sub('.*rows selected \(', '', line).strip(') seconds'))

		if stderr:
			logging.info("...checking stderr")
			for line in stderr.split('\n'):
				logging.debug(line)
				# Check for some common traps for better stderr handling
				# Authentication:
				if "javax.naming.AuthenticationException" in line:
					logging.error(str(re.sub('.*FAILED', '', line)))
					sys.exit(2)
				if "com.simba.hiveserver2.support.exceptions.GeneralException" in line:
					logging.error(str(re.sub('.*FAILED', '', line)))
					sys.exit(2)
				norows_match = re.search('.*No rows affected.*',line)
				rowselect_match = re.search('.*rows selected.*',line)
				if norows_match:
					perfstat += float(re.sub('.*No rows affected \(', '', line).strip(') seconds'))
				elif rowselect_match:
					perfstat += float(re.sub('.*rows selected \(', '', line).strip(') seconds'))

		logging.debug("Total runtime: " + str(perfstat))
		logging.info("Query execution complete.")

		# Check if perfstat was not updated at all
		if perfstat == 0:
			logging.error("No performance data was gathered.")	
			sys.exit(2)

		return perfstat

	def probe(self):
		try:
			if self.queryfile:
				perfstat = self.execute_sqlline("file")
			elif self.query:
				perfstat = self.execute_sqlline("query")

			# Check the time metric with defaults for min/max set
			logging.info("Query time: " + str(perfstat))
		except:
			raise

def initialize_logger(debug, log_filename, log_filename_debug):
	logger = logging.getLogger()
	logger.setLevel(logging.DEBUG)
	 
	# create console handler and set level
	handler = logging.StreamHandler()
	formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
	if debug:
		handler.setLevel(logging.DEBUG)
	else:
		handler.setLevel(logging.INFO)
	handler.setFormatter(formatter)
	logger.addHandler(handler)

	# create inof file handler and set level to info
	handler = logging.FileHandler(log_filename,"w", encoding=None, delay="true")
	handler.setLevel(logging.INFO)
	formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
	handler.setFormatter(formatter)
	logger.addHandler(handler)

	# create debug file handler and set level to debug
	handler = logging.FileHandler(log_filename_debug,"w")
	handler.setLevel(logging.DEBUG)
	formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
	handler.setFormatter(formatter)
	logger.addHandler(handler)			

def main():
	""" Main function """

	# Set the help_position/width values appropriatly to space our long argument text
	# Avoid setting metavar/dest overrides to control argument output if don't you rely on it's value
	aparser = argparse.ArgumentParser(description="Python wrapper for sqlline service check",
	formatter_class=lambda prog: argparse.HelpFormatter(prog,max_help_position=60,width=90))
	aparser.add_argument('-ad', '--adserver', action='store', help="AD Server (kerberos)")
	aparser.add_argument('--creds', action='store', help="Supply credentials file.")
	aparser.add_argument('-db', '--database', required=False, default='default', \
		help="Hive database to use. The database 'default is used by default'")
	aparser.add_argument('-dbg', '--debug', help="Turn on stdout debugging")
	aparser.add_argument('-f', '--filename', required=False, default=None, help= \
		"Process a .hql file. Separate querie sets with \
		blank lines, and continous lines qith a '\'")
	aparser.add_argument('-hs', '--hostname', required=False, help="Hive server")
	aparser.add_argument('-kt', '--keytab', help="Supply a custom keytab file")
	aparser.add_argument('-ln', '--log-name', action='store', default="sqlline-service-check", help="log filename")
	aparser.add_argument('-lf', '--log-folder', action='store', default="/home/"+ getpass.getuser() +"/sqlline-service-check/", help="log folder name")
	aparser.add_argument('-m', '--mode', action='store', required=True, help="Authentication mode. One of: ssl,kerberos")
	aparser.add_argument('-p', '--port', required=False, help="Hive hostname port")
	aparser.add_argument('-q', '--query', required=False, default=None, action='store', help="Query supplied")
	aparser.add_argument('-w', '--warning', metavar='RANGE', default='',
		help='return warning if load is outside RANGE')
	aparser.add_argument('-c', '--critical', metavar='RANGE', default='',
		help='return critical if load is outside RANGE')
	aparser.add_argument('-r', '--realm', action='store', help="AD Server realm (e.g. DOMAIN.COM)")
	aparser.add_argument('-u', '--username', required=False, help="Username")
	aparser.add_argument('-v', '--verbose', action='count', default=0,
                      help='increase output verbosity (use up to 3 times)')
	args = aparser.parse_args()

	# Function-friendly var assigment
	adserver = args.adserver
	database = args.database
	debug = args.debug
	hostname = args.hostname
	mode = args.mode
	query_cmds = []
	realm = args.realm

	if args.query and args.filename:
		sys.exit("Cannot specify both a query and a file")

	if args.query:
		query = args.query
		queryfile = None
	elif args.filename:
		query = None
		queryfile = args.filename

	log_folder = args.log_folder
	if not os.path.isdir(log_folder):
		os.makedirs(log_folder)
	log_filename = log_folder + args.log_name + "-" + args.hostname + ".log"
	log_filename_debug = log_folder + args.log_name + "-" + args.hostname + "-debug.log"
	initialize_logger(debug, log_filename, log_filename_debug)
	logging.debug("===== Starting Logger =====")

	# Authentication
	# Setup creds for non-kerberos (if required)
	if args.creds and mode != "kerberos":
		# use supplied credentials file
		credentials_file = open(args.creds)
		line = credentials_file.readlines()
		username = line[0].rstrip()
		PASSWORD = line[1].rstrip()
		credentials_file.close()

	elif not args.creds and mode != "kerberos":
		sys.exit("Credentials required for non-kerberos use")

	elif not args.username:
		sys.exit("Username argument is requird")
	else:
		username = args.username
		PASSWORD = ""

	if mode == "kerberos":
		# Check for required args that support kerberos
		if not args.adserver:
			sys.exit("--adserver is required for kerberos usage (e.g. hostname.domain.com)")
		if not args.realm:
			sys.exit("--realm is required for kerberos usage (e.g. DOMAIN.COM)")

		# Test for kerberos ticket
		proc_status = subprocess.call(['klist', '-s'])
		if proc_status is not 0:
			if not args.keytab:
				sys.exit("No kerberos ticket found and no keytab provided!")
			logging.info("No keberos ticket found, calling kinit")	
			proc_status = subprocess.call(['kinit', '-kt', \
				args.keytab, username + '@GEISINGER.EDU'], stdout=open('/dev/null', 'w'))
			if proc_status is not 0:
				sys.exit("Failed to kinit")

	# Port
	if not args.port:
		port = 10000
	elif args.port:
		port = args.port
	else:
		sys.exit('Invalid arg')

	# Get metrics
	try:
		# Instantiate class and check
		logging.debug("Instantiating class")
		check = QuerySafetyCheck(database, adserver, hostname, mode, PASSWORD,port, \
			query, queryfile, realm, username)
			#nagiosplugin.ScalarContext('query_time', args.warning, args.critical))
	except:
		raise
		sys.exit("Failed to run metrics check")

	# Run check
	logging.info("Running check")
	try:
		check.probe()
	except:
		raise

# Start main
if __name__ == '__main__':
	main()

