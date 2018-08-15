# Makefile for hiveserver2-service-check.py
# Author: Michael DeGuzis <mtdeguzis@geisinger.edu>
# https://udajira:8443/browse/HO-1713
# https://udajira:8443/browse/HO-1806
#
# NOTE: For Hive functionality, you will need the Simba Hive JDBC drivers
# This can be installed/built from the rpm GitHub repo -> simba-hive-jdbc

CURRENT_USER = $(shell echo $whoami)
SIMBA_DRIVERS = 'https://s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/hive-jdbc4/1.0.42.1054/Simba_HiveJDBC41_1.0.42.1054.zip'

all: build install

build: clean
	mvn package

install: build
	mvn package
	# Until repo is up, we need to make sure the drivers are pushed manually
	rm -rf $(CURDIR)/hivejars && mkdir $(CURDIR)/hivejars
	cd  $(CURDIR)/hivejars && curl -O $(SIMBA_DRIVERS) && unzip Simba_HiveJDBC*.zip
	# Update for install dir
	cp $(CURDIR)/bin/sqlline.template $(CURDIR)/bin/sqlline
	sed -i "s|@install_dir@|$(CURDIR)|g" $(CURDIR)/bin/sqlline
	# Link bin file
	sudo ln -s $(CURDIR)/bin/sqlline /usr/local/bin/sqlline
	# Needed due to Tidal checks using old Icinga script that requires nagiosplugin library
	pipenv install

install-icinga: build
	# Build pipenv environment for python wrapper
	# TODO,this needs to be adjusted or removed entirely for icing
	export HOME=/home/icinga && \
	pipenv install
	# Install symlinks
	rm -f /usr/lib64/nagios/plugins/sqlline-service-check
	ln -s $(CURDIR)/bin/sqlline-service-check /usr/local/bin/sqlline-service-check
	ln -s $(CURDIR)/bin/sqlline-service-check /usr/lib64/nagios/plugins/sqlline-service-check
	ln -s $(CURDIR)/bin/sqlline /usr/local/bin/sqlline

clean:
	# If needed, for local .m2 repo
	# rm -rf ~/.m2/repository/{net,de,asm,io,tomcat,javaolution,commons-pool,commons-dbcp,javax,co,ch,org,com,it}
	@echo "Cleaning and purging project files and local repository artifacts..."
	mvn clean
	mvn build-helper:remove-project-artifact
