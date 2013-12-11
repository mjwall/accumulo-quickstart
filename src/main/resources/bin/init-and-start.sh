#!/bin/bash

# this script is meant to be run once, typically by sbt.  It could be bad to run this again

_script_dir() {
    if [ -z "${SCRIPT_DIR}" ]; then
    # even resolves symlinks, see
    # http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
        local SOURCE="${BASH_SOURCE[0]}"
        while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
        SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
    fi
    echo "${SCRIPT_DIR}"
}

source "$(_script_dir)/cloud-env"

# format namenode and start hadoop
cd $HADOOP_PREFIX
echo "Formatting namenode"
./bin/hadoop namenode -format
echo "Starting Hadoop"
mkdir -p "${CLOUD_INSTALL_HOME}/hdfs/namesecondary" #unsure why this doesn't run
./bin/start-all.sh
echo "Waiting 30 seconds for HDFS to come out of safe mode"
sleep 30

# start zookeeper
cd $ZOOKEEPER_HOME
mkdir -p "${CLOUD_INSTALL_HOME}/zk-data" #how can I pass this in?
echo "Starting Zookeeper"
./bin/zkServer.sh start

# setup and start accumulo
# how can I pass in the instancename, password and username?
cd $ACCUMULO_HOME
echo "Initing Accumulo as root@accumulo, password is secret"
./bin/accumulo init --clear-instance-name --instance-name accumulo --password secret --username root
echo "Starting Accumulo"
./bin/start-all.sh

echo "Accumulo is now running from ${CLOUD_INSTALL_HOME}"
echo -e "You should run \n  source ${CLOUD_INSTALL_HOME}/bin/cloud-env\n and get to work.  The monitor page should be available at http://localhost:50095"
