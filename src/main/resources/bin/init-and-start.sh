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

check_running() {
  echo "Making sure nothing is currently running"
  jps -lm | grep NameNode || jps -lm | grep QuorumPeer || jps -lm | grep org.apache.accumulo
  ret=$?
  if [ "$ret" -eq 0 ]; then
    echo "Found stuff, it is likely there will be port conflicts.  Please stop what you have running and execute './bin/sbt initAndStart' to finish the install"
    ret=1
  else
    # nothing found running
    ret=0
  fi
  return $ret
}

format_namenode() {
  cd $HADOOP_PREFIX
  echo "Formatting namenode"
  ./bin/hadoop namenode -format
}

start_hadoop() {
  echo "Starting Hadoop"
  mkdir -p "${CLOUD_INSTALL_HOME}/hdfs/namesecondary" #unsure why this doesn't get created
  ./bin/start-all.sh
  echo "Waiting for hadoop to come out of safeMode"
  ./bin/hadoop dfsadmin -safemode wait
}

start_zookeeper() {
  cd $ZOOKEEPER_HOME
  mkdir -p "${CLOUD_INSTALL_HOME}/zk-data" #how can I pass this in?
  echo "Starting Zookeeper"
  ./bin/zkServer.sh start
}

init_accumulo() {
  # how can I pass in the instancename, password and username?
  cd $ACCUMULO_HOME
  echo "Initing Accumulo as root@accumulo, password is secret"
  ./bin/accumulo init --clear-instance-name --instance-name accumulo --password secret --username root
}

start_accumulo() {
   echo "Starting Accumulo"
  ./bin/start-all.sh
}

finish() {
  echo "Accumulo is now running from ${CLOUD_INSTALL_HOME}"
  echo -e "You should run \n  source ${CLOUD_INSTALL_HOME}/bin/cloud-env\n and get to work.  The monitor page should be available at http://localhost:50095"
  # make this script unexecutable
  chmod 644 "$(_script_dir)/$(basename $0)"
}

check_running && format_namenode && start_hadoop && start_zookeeper && init_accumulo && start_accumulo && finish
