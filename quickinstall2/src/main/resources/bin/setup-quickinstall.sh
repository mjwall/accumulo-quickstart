#!/bin/bash

# this script is meant to be run once.  It could be bad to run this again

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

# just cut off /bin/
QI_HOME="$(_script_dir | rev | cut -c5- | rev)"

check_java_home() {
  echo "Checking that JAVA_HOME is set and exists"
  if [ "${JAVA_HOME}x" == "x" ]; then
    echo "JAVA_HOME is not set, export JAVA_HOME and try again"
    exit 1
  elif [ ! -d ${JAVA_HOME} ]; then
    echo "JAVA_HOME does not point to an existing directory but to $JAVA_HOME.  Fix and try again"
    exit 1
  fi
}

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

_ssh() {
    # this function sets publickey as the only authentication, which is
    # the passwordless way Hadoop communicates in psuedo distributed mode
    echo $(ssh -o 'PreferredAuthentications=publickey' localhost "hostname")
}

check_ssh() {
  # check ssh localhost
  echo "Checking passwordless SSH (for Hadoop)"
  local ret=0
  local HOSTNAME=$(hostname)
  local SSH_HOST=$(_ssh)
  if [[ "${HOSTNAME}" != "${SSH_HOST}" ]]; then
    echo "Problem with SSH, ran ssh -o 'PreferredAuthentications=publickey' localhost \"hostname\". Expected ${HOSTNAME}, but got ${SSH_HOST}. Please see file:///$(_script_dir)/hadoop-2.4.1/share/doc/hadoop/hadoop-project-dist/hadoop-common/SingleCluster.html#Setup_passphraseless_ssh.  Once ssh without a password is working, execute './bin/sbt initAndStart' to finish the install"
    ret=1
  fi
  return $ret
}

setup_hadoop_conf() {
  echo "Setting up Hadoop conf"
  HADOOP_HOME="${QI_HOME}/hadoop-2.4.1"
  sed -i '' -e "s|QI_HADOOP_HOME|${HADOOP_HOME}|" ${HADOOP_HOME}/etc/hadoop/hadoop-env.sh
  sed -i '' -e "s|QI_JAVA_HOME|${JAVA_HOME}|" ${HADOOP_HOME}/etc/hadoop/hadoop-env.sh
  HDFS_DIR="${QI_HOME}/hdfs"
  mkdir -p ${HDFS_DIR}/name
  mkdir -p ${HDFS_DIR}/data
  sed -i '' -e "s|QI_HDFS_DIR|${HDFS_DIR}|g" ${HADOOP_HOME}/etc/hadoop/hdfs-site.xml
  export HADOOP_HOME
}

format_namenode() {
  echo "Formatting namenode"
  ${HADOOP_HOME}/bin/hdfs namenode -format
}

start_hadoop() {
  echo "Starting Hadoop"
  ${HADOOP_HOME}/sbin/start-dfs.sh
  echo "Waiting for hadoop to come out of safeMode"
  local x=0
  while [ $x -lt 5 ]
  do
    # try 2 times, for slow computers
    ${HADOOP_HOME}/bin/hdfs dfsadmin -safemode wait && x=5
    x=$(( $x + 1 ))
  done
  echo "Starting Yarn"
  ${HADOOP_HOME}/sbin/start-yarn.sh
}

setup_zookeeper_conf() {
  echo "Setting up Zookeeper conf"
  ZOOKEEPER_HOME="${QI_HOME}/zookeeper-3.4.6"
  sed -i '' -e "s|QI_ZOOKEEPER_HOME|${ZOOKEEPER_HOME}|" ${ZOOKEEPER_HOME}/bin/zkEnv.sh
  ZOO_DATA="${QI_HOME}/zk-data"
  mkdir -p ${ZOO_DATA}
  sed -i '' -e "s|QI_ZOO_DATA|${ZOO_DATA}|" ${ZOOKEEPER_HOME}/conf/zoo.cfg
  export ZOOKEEPER_HOME
}

start_zookeeper() {
  echo "Starting Zookeeper"
  $ZOOKEEPER_HOME/bin/zkServer.sh start
}

setup_accumulo_conf() {
  echo "Replacing accumulo conf"
}

create_cloud_env() {
  echo "Creating the cloud-env file"
####
##!/bin/bash
#export JAVA_HOME=REPLACE_JAVA_HOME
#export CLOUD_INSTALL_HOME=REPLACE_CLOUD_INSTALL_HOME
#export HADOOP_PREFIX=REPLACE_HADOOP_PREFIX
#if [ ! -z $HADOOP_HOME ]; then
#  echo "Unsetting HADOOP_HOME, we only use HADOOP_PREFIX"
#  unset HADOOP_HOME
#fi
#export ZOOKEEPER_HOME=REPLACE_ZOOKEEPER_HOME
#export ACCUMULO_HOME=REPLACE_ACCUMULO_HOME
#export PATH=$CLOUD_INSTALL_HOME/bin:$HADOOP_PREFIX/bin:$ZOOKEEPER_HOME/bin:$ACCUMULO_HOME/bin:$PATH
#source "$(_script_dir)/cloud-env"
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
  create_cloud_env
  echo "Accumulo is now running from ${CLOUD_INSTALL_HOME}"
  echo -e "You should run \n  source ${CLOUD_INSTALL_HOME}/bin/cloud-env\n and get to work.  The monitor page should be available at http://localhost:50095"
  # make this script unexecutable
  chmod 644 "$(_script_dir)/$(basename $0)"
}

run_checks() {
  check_java_home && check_running && check_ssh && echo "Looks good, installing to ${QI_HOME}"
}

setup_hadoop() {
  setup_hadoop_conf && format_namenode && start_hadoop
}

setup_zookeeper() {
  setup_zookeeper_conf && start_zookeeper
}

setup_accumulo() {
  setup_accumulo_conf && init_accumulo && start_accumulo
}

run_checks && setup_hadoop && setup_zookeeper #&& setup_accumulo && finish
