# Makefile for hiveserver2-service-check.py
# Author: Michael DeGuzis <mtdeguzis@geisinger.edu>
# https://udajira:8443/browse/HO-1713
# https://udajira:8443/browse/HO-1806
#
# NOTE: For Hive functionality, you will need the Simba Hive JDBC drivers
# This can be installed/built from the rpm GitHub repo -> simba-hive-jdbc

CURRENT_USER = $(shell echo $whoami)

build: clean
	mvn package

install: build
	@echo "Installing RPM"
	find $(CURDIR) -name "*.rpm" -exec sudo rpm -i {} \;

install-icinga: install
	# Perform regular install + pipenv wrapper for nagiosplugin dependency
	# Build pipenv environment for python wrapper
	export HOME=/home/icinga && \
	pipenv install
	# Install symlinks
	rm -f /usr/lib64/nagios/plugins/sqlline-service-check
	rm -f /usr/local/bin/sqlline-service-check
	ln -s $(CURDIR)/bin/sqlline-service-check /usr/local/bin/sqlline-service-check
	ln -s $(CURDIR)/bin/sqlline-service-check /usr/lib64/nagios/plugins/sqlline-service-check

clean:
	# If needed, for local .m2 repo
	# rm -rf ~/.m2/repository/{net,de,asm,io,tomcat,javaolution,commons-pool,commons-dbcp,javax,co,ch,org,com,it}
	@echo "Cleaning and purging project files and local repository artifacts..."
	mvn clean
	mvn build-helper:remove-project-artifact
	@-sudo rpm -e sqlline
	# If RPM was inspected/unpacked for testing
	rm -rf usr/
