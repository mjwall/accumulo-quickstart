#! /usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

###
### Configure these environment variables to point to your local installations.
###
### The functional tests require conditional values, so keep this style:
###
### test -z "$JAVA_HOME" && export JAVA_HOME=/usr/local/lib/jdk-1.6.0
###
###
### Note that the -Xmx -Xms settings below require substantial free memory:
### you may want to use smaller values, especially when running everything
### on a single machine.
###
if [ -z "$HADOOP_HOME" ]
then
  #test -z "$HADOOP_PREFIX"      && export HADOOP_PREFIX=REPLACE_HADOOP_PREFIX
  test -z "$HADOOP_PREFIX"      && export HADOOP_PREFIX=/Users/mjwall/Desktop/cloud/hadoop-1.0.4
else
   HADOOP_PREFIX="$HADOOP_HOME"
   unset HADOOP_HOME
fi
test -z "$HADOOP_CONF_DIR"       && export HADOOP_CONF_DIR="$HADOOP_PREFIX/conf"
# hadoop-2.0:
# test -z "$HADOOP_CONF_DIR"     && export HADOOP_CONF_DIR="$HADOOP_PREFIX/etc/hadoop"


#test -z "$JAVA_HOME"             && export JAVA_HOME=REPLACE_JAVA_HOME
test -z "$JAVA_HOME"             && export JAVA_HOME=/Library/Java/Home
#test -z "$ZOOKEEPER_HOME"        && export ZOOKEEPER_HOME=REPLACE_ZOOKEEPER_HOME
test -z "$ZOOKEEPER_HOME"        && export ZOOKEEPER_HOME=/Users/mjwall/Desktop/cloud/zookeeper-3.3.6
test -z "$ACCUMULO_LOG_DIR"      && export ACCUMULO_LOG_DIR=$ACCUMULO_HOME/logs
if [ -f ${ACCUMULO_HOME}/conf/accumulo.policy ]
then
   POLICY="-Djava.security.manager -Djava.security.policy=${ACCUMULO_HOME}/conf/accumulo.policy"
fi
test -z "$ACCUMULO_TSERVER_OPTS" && export ACCUMULO_TSERVER_OPTS="${POLICY} -Xmx768m -Xms768m "
test -z "$ACCUMULO_MASTER_OPTS"  && export ACCUMULO_MASTER_OPTS="${POLICY} -Xmx256m -Xms256m"
test -z "$ACCUMULO_MONITOR_OPTS" && export ACCUMULO_MONITOR_OPTS="${POLICY} -Xmx128m -Xms64m"
test -z "$ACCUMULO_GC_OPTS"      && export ACCUMULO_GC_OPTS="-Xmx128m -Xms128m"
test -z "$ACCUMULO_GENERAL_OPTS" && export ACCUMULO_GENERAL_OPTS="-XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75"
test -z "$ACCUMULO_OTHER_OPTS"   && export ACCUMULO_OTHER_OPTS="-Xmx256m -Xms64m"
export ACCUMULO_LOG_HOST=`(grep -v '^#' $ACCUMULO_HOME/conf/monitor ; echo localhost ) 2>/dev/null | head -1`

if [ "$(uname)" == "Darwin" ]; then
  # https://issues.apache.org/jira/browse/HADOOP-7489
  export ACCUMULO_OTHER_OPTS="${ACCUMULO_OTHER_OPTS} -Djava.security.krb5.realm=OX.AC.UK -Djava.security.krb5.kdc=kdc0.ox.ac.uk:kdc1.ox.ac.uk"
  # http://stackoverflow.com/questions/17460777/stop-java-coffee-cup-icon-from-appearing-in-doc-on-mac-osx
  export ACCUMULO_OTHER_OPTS="${ACCUMULO_OTHER_OPTS} -Dapple.awt.UIElement=true"
  export ACCUMULO_GENERAL_OPTS="${ACCUMULO_GENERAL_OPTS} -Dapple.awt.UIElement=true"
  export ACCUMULO_MASTER_OPTS="${ACCUMULO_MASTER_OPTS} -Dapple.awt.UIElement=true"
  export ACCUMULO_GC_OPTS="${ACCUMULO_GC_OPTS} -Dapple.awt.UIElement=true"
fi
