# Makefile for hiveserver2-service-check.py
# Author: Michael DeGuzis <mtdeguzis@geisinger.edu>
# https://udajira:8443/browse/HO-1713
# https://udajira:8443/browse/HO-1806

GIT_DIR ?= "/opt/sqlline"

# https://hortonworks.com/downloads/
SIMBA_DRIVERS = 'https://s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/hive-jdbc4/1.0.42.1054/Simba_HiveJDBC41_1.0.42.1054.zip'

all: build install

build:
	rm -fv /usr/local/bin/sqlline-service-check
	rm -fv /usr/local/bin/sqlline
	rm -fv /usr/lib64/nagios/plugins/sqlline
	# sqlline build
	mvn package
	rm -rf $(GIT_DIR)/hivejars && mkdir $(GIT_DIR)/hivejars
	cd  $(GIT_DIR)/hivejars && curl -O $(SIMBA_DRIVERS) && unzip Simba_HiveJDBC*.zip

install: build
	ln -s /opt/sqlline/bin/sqlline /usr/local/bin/sqlline

install-icinga: build
	# Build pipenv environment for python wrapper
	export HOME=/home/icinga && \
	pipenv install
	# Install symlinks
	ln -s /opt/sqlline/bin/sqlline-service-check /usr/local/bin/sqlline-service-check
	ln -s /opt/sqlline/bin/sqlline-service-check /usr/lib64/nagios/plugins/sqlline-service-check
	ln -s /opt/sqlline/bin/sqlline /usr/local/bin/sqlline

uninstall:
	rm -fv /usr/local/bin/sqlline
	rm -fv /usr/local/bin/sqlline-service-check
	rm -fv /usr/lib64/nagios/plugins/sqlline-service-check

