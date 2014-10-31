#!/bin/bash

# this script is meant to be run once.  It could be bad to run this again

# internal functions
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

_replace_stuff() {
  local TO=$1
  local FROM=$2
  local FILE=$3
  if [ "$(uname)" == "Darwin" ]; then
    sed -i '' -e "s|${TO}|${FROM}|g" ${FILE}
  else
    sed -i'' -e "s|${TO}|${FROM}|g" ${FILE}
  fi
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
    echo "Problem with SSH, ran ssh -o 'PreferredAuthentications=publickey' localhost \"hostname\". Expected ${HOSTNAME}, but got ${SSH_HOST}. Please see file:///$(_script_dir)/hadoop-2.4.1/share/doc/hadoop/hadoop-project-dist/hadoop-common/SingleCluster.html#Setup_passphraseless_ssh.  Once ssh without a password is working, execute './bin/setup-quickinstall' again"
    ret=1
  fi
  return $ret
}

setup_hadoop_conf() {
  echo "Setting up Hadoop conf"
  HADOOP_HOME="${QI_HOME}/hadoop-2.4.1"
  _replace_stuff "QI_HADOOP_HOME" "${HADOOP_HOME}" ${HADOOP_HOME}/etc/hadoop/hadoop-env.sh
  _replace_stuff "QI_JAVA_HOME" "${JAVA_HOME}" ${HADOOP_HOME}/etc/hadoop/hadoop-env.sh
  HDFS_DIR="${QI_HOME}/hdfs"
  mkdir -p ${HDFS_DIR}/name
  mkdir -p ${HDFS_DIR}/data
  _replace_stuff "QI_HDFS_DIR" "${HDFS_DIR}" ${HADOOP_HOME}/etc/hadoop/hdfs-site.xml
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
  _replace_stuff "QI_ZOOKEEPER_HOME" "${ZOOKEEPER_HOME}" ${ZOOKEEPER_HOME}/bin/zkEnv.sh
  ZOO_DATA="${QI_HOME}/zk-data"
  mkdir -p ${ZOO_DATA}
  _replace_stuff "QI_ZOO_DATA" "${ZOO_DATA}" ${ZOOKEEPER_HOME}/conf/zoo.cfg
  export ZOOKEEPER_HOME
}

start_zookeeper() {
  echo "Starting Zookeeper"
  $ZOOKEEPER_HOME/bin/zkServer.sh start
}

setup_accumulo_conf() {
  echo "Setting up Accumulo conf"
  ACCUMULO_HOME="${QI_HOME}/accumulo-1.6.1"
  cp -R ${ACCUMULO_HOME}/conf/examples/2GB/standalone/* ${ACCUMULO_HOME}/conf/.
  _replace_stuff "JAVA_HOME=/path/to/java" "JAVA_HOME=${JAVA_HOME}" ${ACCUMULO_HOME}/conf/accumulo-env.sh
  _replace_stuff "ZOOKEEPER_HOME=/path/to/zookeeper" "ZOOKEEPER_HOME=${ZOOKEEPER_HOME}" ${ACCUMULO_HOME}/conf/accumulo-env.sh
  cat <<'EOF' >> ${ACCUMULO_HOME}/conf/accumulo-env.sh
if [ "$(uname)" == "Darwin" ]; then
  # https://issues.apache.org/jira/browse/HADOOP-7489
  export ACCUMULO_GENERAL_OPTS="${ACCUMULO_GENERAL_OPTS} -Djava.security.krb5.realm= -Djava.security.krb5.kdc="
  # same fix, but for java 7, see
  export ACCUMULO_GENERAL_OPTS="${ACCUMULO_GENERAL_OPTS} -Djava.security.krb5.config=/dev/null"
  # http://stackoverflow.com/questions/17460777/stop-java-coffee-cup-icon-from-appearing-in-doc-on-mac-osx
  export ACCUMULO_GENERAL_OPTS="${ACCUMULO_GENERAL_OPTS} -Dapple.awt.UIElement=true"
fi
EOF
  export ACCUMULO_HOME
}

init_accumulo() {
  echo "Initing Accumulo as root@accumulo, password is quickinstall"
  ${ACCUMULO_HOME}/bin/accumulo init --clear-instance-name --instance-name accumulo --password quickinstall
}

start_accumulo() {
  echo "Starting Accumulo"
  ${ACCUMULO_HOME}/bin/start-all.sh
}

finish() {
  ENV_FILE="${QI_HOME}/bin/quickinstall-env"
  echo "Setting up ${ENV_FILE}"
  _replace_stuff "QI_QI_HOME" "${QI_HOME}" ${ENV_FILE}
  _replace_stuff "QI_JAVA_HOME" "${JAVA_HOME}" ${ENV_FILE}
  _replace_stuff "QI_HADOOP_HOME" "${HADOOP_HOME}" ${ENV_FILE}
  _replace_stuff "QI_ZOOKEEPER_HOME" "${ZOOKEEPER_HOME}" ${ENV_FILE}
  _replace_stuff "QI_ACCUMULO_HOME" "${ACCUMULO_HOME}" ${ENV_FILE}
  chmod 755 ${ENV_FILE}
  # make it harder to run the setup again
  chmod 400 $(_script_dir)/setup-quickinstall.sh
  echo "Accumulo is now running from ${QI_HOME}"
  echo "You should run"
  echo "    source ${ENV_FILE}"
  echo "and get to work.  Here are some useful links."
  echo "  The Accumulo monitor page should be available at http://localhost:50095"
  echo "  HDFS should be available at http://localhost:50070"
  echo "  The Yarn resource manager should be available at http://localhost:8088"
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

run_checks && setup_hadoop && setup_zookeeper && setup_accumulo && finish
