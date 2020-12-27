FROM debian:stable

MAINTAINER "Petter Nordlander"

ARG LOCATION=/usr/local/a

# Update the debian package catalog
RUN apt-get update

# Upgrade any existing package, typically security patches and tzdata
RUN apt-get upgrade -y

# Install maven
# all further tools and libraries will be pulled by MVN
# explicitly install default-jre-headless so that it will not be removed later
RUN apt-get install -y maven default-jre-headless

# Copy all required source code into the image
RUN mkdir --parents ${LOCATION}/
COPY pom.xml ${LOCATION}/
COPY LICENSE ${LOCATION}/
COPY README.md ${LOCATION}/
COPY src ${LOCATION}/src

# The default hostname is 'localhost'
# But in a docker container that is still inside the container only
# We need to replace 'localhost' with 'host.docker.internal'
# and otherwise, user still needs to specify the alternative
RUN sed --in-place \
	-e 's/localhost:/host.docker.internal:/' \
	${LOCATION}/src/main/java/co/nordlander/a/A.java

# Build the A software in the usual way
RUN cd /usr/local/a && mvn package -DskipTests

# Create a new command that is always in the PATH
RUN echo "#!/bin/sh" > /usr/bin/a && \
	echo "java \
		-Dnashorn.args=--no-deprecation-warning \
		-cp ${LOCATION}/target/a-1.5.0-SNAPSHOT-jar-with-dependencies.jar \
		co.nordlander.a.A \"\$@\"" >> /usr/bin/a && \
	chmod a+rx /usr/bin/a
RUN cat /usr/bin/a

# Ready
# let's shrink the image
RUN apt-get remove -y maven
RUN apt autoremove -y

# This will only show the usage
CMD a

# a more useful use is:
#	docker run a a --get queue1
# note that 'a' has to be specified twice
# the first one is the image name
# the second one is the 'alternative' commands that we want to run
