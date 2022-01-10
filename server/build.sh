#!/bin/bash
export JAVA_HOME=/usr/local/java
export JRE_HOME=${JAVA_HOME}/jre 
export CLASSPATH=.:${JAVA_HOME}/lib:${JRE_HOME}/lib 
export PATH=${JAVA_HOME}/bin:$PATH 

#
set -e
        mvn clean package -DskipTests
        docker build -t qgbest/flowable .
        docker tag qgbest/flowable 121.36.194.218:4011/flowable
        docker push 121.36.194.218:4011/flowable
