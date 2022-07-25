#!/bin/bash
if [ -z "$MAVEN_HOME" ]
then
  mvn clean package -DskipTests
else
  $MAVEN_HOME/bin/mvn clean package -DskipTests
fi
