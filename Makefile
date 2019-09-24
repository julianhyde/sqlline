# Makefile for hiveserver2-service-check.py
# Author: Michael DeGuzis <mtdeguzis@geisinger.edu>
# https://udajira:8443/browse/HO-1713
# https://udajira:8443/browse/HO-1806
#
# NOTE: For Hive functionality, you will need the Simba Hive JDBC drivers
# This can be installed/built from the rpm GitHub repo -> simba-hive-jdbc
# REQUIRES: maven > 3.2.1

CURRENT_USER = $(shell echo $whoami)
# Now packaged as simba-hive-jdbc in Artifactory
#SIMBA_DRIVERS = "https://public-repo-1.hortonworks.com/HDP/hive-jdbc4/2.6.2.1002/SimbaHiveJDBC41-2.6.2.1002.zip"
SQLLINE_VER = $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
MAVEN_DL = "http://www.trieuvan.com/apache/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz"
MVN_BINARY = "$(CURDIR)/maven/bin/mvn"

all: build

build: clean
	# download required maven version
	rm -rf maven && mkdir maven
	cd maven && curl -O $(MAVEN_DL) && tar -xzf apache-maven*.tar.gz --strip-components=1
	$(MVN_BINARY) -version
	$(MVN_BINARY) package
	# Update for install dir
	cp $(CURDIR)/bin/sqlline.template $(CURDIR)/bin/sqlline
	sed -i "s|@install_dir@|$(CURDIR)|g" $(CURDIR)/bin/sqlline
	sed -i "s|@sqlline_ver@|$(SQLLINE_VER)|g" $(CURDIR)/bin/sqlline

install-icinga: build
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
	rm -f /usr/local/bin/sqlline
	rm -f /usr/lib64/nagios/plugins/sqlline-service-check
	rm -f /usr/local/bin/sqlline-service-check
	rm -rf maven

verify:
	@# verify project version via mvn
	$(info SQLLINE_VER is [${SQLLINE_VER}])
