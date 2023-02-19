#!/bin/sh

# Script to build a DEB (Debian/Ubuntu/etc) installer package
# This procedure might not delivere a package that is ready to be added to one of these distributions
# But it will deliver an installable DEB packager

# install the required software
sudo apt-get install maven

# Build the actual 'a' software
(cd ../..; mvn clean install -DskipTests)

# Add the full jar file to the distribution
mkdir --parents fmtn-a/usr/share/java
cp ../../target/a-*-jar-with-dependencies.jar fmtn-a/usr/share/java/

# Add a helper script to the distribution
mkdir --parents fmtn-a/usr/bin
cp a.sh fmtn-a/usr/bin/a
chmod a+rx fmtn-a/usr/bin/a

# Build the package
dpkg-deb --build --root-owner-group fmtn-a

# (sample) Install the package
#sudo dpkg -i fmtn-a.deb

# End
