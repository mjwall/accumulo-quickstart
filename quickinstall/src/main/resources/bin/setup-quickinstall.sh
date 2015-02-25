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
  JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\.\2/; 1q')
  echo "Using java version ${JAVA_VERSION}"
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
  HADOOP_PREFIX="${HADOOP_HOME}"
  HADOOP_YARN_HOME="${HADOOP_HOME}"
  HADOOP_COMMON_HOME="${HADOOP_HOME}"
  HADOOP_HDFS_HOME="${HADOOP_HOME}"
  HADOOP_MAPRED_HOME="${HADOOP_HOME}"
  HADOOP_CONF_DIR="${HADOOP_PREFIX}/etc/hadoop"
  _replace_stuff "QI_HADOOP_HOME" "${HADOOP_HOME}" ${HADOOP_HOME}/etc/hadoop/hadoop-env.sh
  _replace_stuff "QI_JAVA_HOME" "${JAVA_HOME}" ${HADOOP_HOME}/etc/hadoop/hadoop-env.sh
  HDFS_DIR="${QI_HOME}/hdfs"
  mkdir -p ${HDFS_DIR}/name
  mkdir -p ${HDFS_DIR}/data
  _replace_stuff "QI_HDFS_DIR" "${HDFS_DIR}" ${HADOOP_HOME}/etc/hadoop/hdfs-site.xml
  # move existing native libraries to linux-32-native
  mv "${HADOOP_HOME}/lib/native" "${HADOOP_HOME}/lib/linux-32-native"
  # there is a bug in this hadoop that requires them under /lib
  # but this version of bin/accumulo is setting lib/native as part of the
  # java.library.path so it is getting confused.  Relink below
  # for the correct version of your OS
  if [ "$(uname)" == "Darwin" ]; then
    if [ "$JAVA_VERSION" == "1.7" ] || [ "$JAVA_VERSION" == "1.8" ]; then
      echo "Using Mac OSX native libraries built on OSX 10.8.5 with Java 1.7.0_60 using the instructions at"
      echo "http://gauravkohli.com/2014/09/28/building-native-hadoop-v-2-4-1-libraries-for-os-x/"
      echo "Remove the symlink in \${HADOOP_HOME}/lib/ if you have problems"
      for f in ${HADOOP_HOME}/lib/darwin-native/*; do
        ln -s "${f}" "${HADOOP_HOME}/lib/$(basename $f)"
      done
    else
      echo "Sorry, you are not running Java 1.7 or 1.8.  Unable to provide native hadoop libraries"
    fi
  elif [ "$(uname)" == "Linux" ]; then
    if [ "$(uname -m)" == "x86_64" ]; then
      echo "Using 64 bit native libraries built on CentOS 6.6 with Java 1.7 using the instructions at"
      echo "http://hadoop.apache.org/docs/r2.4.1/hadoop-project-dist/hadoop-common/NativeLibraries.html"
      echo "Remove symlink in \${HADOOP_HOME}/lib/ directory if you have a problem"
      for f in ${HADOOP_HOME}/lib/linux-64-native/*; do
        ln -s "${f}" "${HADOOP_HOME}/lib/$(basename $f)"
      done
    else
      echo "Using 32 bit native libraries packaged with Hadoop"
      echo "Remove symlinks in \${HADOOP_HOME}/lib directory if you have a problem"
      for f in ${HADOOP_HOME}/lib/linux-32-native/*; do
        ln -s "${f}" "${HADOOP_HOME}/lib/$(basename $f)"
      done
    fi
  else
    echo "Unknown OS, not trying to use any native libraries"
  fi
  export HADOOP_HOME HADOOP_PREFIX HADOOP_CONF_DIR HADOOP_COMMON_HOME HADOOP_YARN_HOME HADOOP_HDFS_HOME HADOOP_MAPRED_HOME
  export HADOOP_CLASSPATH=""
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
    # try 5 times, for slow computers
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
  echo "Attempting to build Accumulo native libraries"
  EXAMPLE_CONFIG="2GB/native-standalone"
  local return_dir=${PWD}
  cd ${ACCUMULO_HOME}
  ./bin/build_native_library.sh || EXAMPLE_CONFIG="2BG/standalone"
  cd ${return_dir}
  echo "Starting configs are from ${EXAMPLE_CONFIG}"
  cp -R ${ACCUMULO_HOME}/conf/examples/${EXAMPLE_CONFIG}/* ${ACCUMULO_HOME}/conf/.
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
  # Hadoop native libraries
  export DYLD_LIBRARY_PATH=${HADOOP_PREFIX}/lib:${DYLD_LIBRARY_PATH}
else
  export LD_LIBRARY_PATH=${HADOOP_PREFIX}/lib:${LD_LIBRARY_PATH}
fi
EOF
  sed -i.bak 's!<value>root</value>!<value>trace</value>!g' ${ACCUMULO_HOME}/conf/accumulo-site.xml
  sed -i.bak 's!<value>secret</value>!<value>trace</value>!g' ${ACCUMULO_HOME}/conf/accumulo-site.xml
  export ACCUMULO_HOME
}

init_accumulo() {
  ROOT_PASS="secret" #need to change the trace user pass if this is different in accumulo-site.xml
  echo "Initing Accumulo as root@accumulo, password is ${ROOT_PASS}"
  ${ACCUMULO_HOME}/bin/accumulo init --clear-instance-name --instance-name accumulo --password ${ROOT_PASS}
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
  _replace_stuff "QI_HADOOP_CONF_DIR" "${HADOOP_CONF_DIR}" ${ENV_FILE}
  _replace_stuff "QI_ZOOKEEPER_HOME" "${ZOOKEEPER_HOME}" ${ENV_FILE}
  _replace_stuff "QI_ACCUMULO_HOME" "${ACCUMULO_HOME}" ${ENV_FILE}
  # make all files executable
  chmod 755 ${QI_HOME}/bin/*
  # leave this script 400 and rename it
  chmod 400 $(_script_dir)/setup-quickinstall.sh
  mv $(_script_dir)/setup-quickinstall.sh $(_script_dir)/setup-quickinstall.sh.already.run
  cat <<-EOF

************************************************

Accumulo is now running from ${QI_HOME}
You should run

    source ${ENV_FILE}

and get to work.  The root accumulo user password is "${ROOT_PASS}".

After sourcing the env file, your PATH will be setup.  Source that
file whenever you want to work with the Accumulo Quickinstall

To stop Accumulo, Zookeeper and Hadoop run

    qi-stop

To start it up again, run

    qi-start

There will also be a couple of shortcut commands to open the local docs.
These pages link to local copies of the API docs.  Run

    accumulo-doc

or

    hadoop-doc

Here are some useful links.
  The Accumulo monitor page should be available at http://localhost:50095
  HDFS should be available at http://localhost:50070
  The Yarn resource manager should be available at http://localhost:8088

You can also reference the README in the quickinstall-home directory for
more information.

Happy Accumulating

EOF
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
  TMPFILE=`mktemp /tmp/${tempfoo}.XXXXXX` || exit 1
  cat <<EOF > ${TMPFILE}
createuser trace
createtable trace
grant -u trace Table.WRITE -t trace
grant -u trace Table.READ -t trace
grant -u trace Table.ALTER_TABLE -t trace
EOF

  echo -e 'trace\ntrace\n' | ${ACCUMULO_HOME}/bin/accumulo shell -u root -p secret -f ${TMPFILE} && rm ${TMPFILE}
}

run_checks && setup_hadoop && setup_zookeeper && setup_accumulo && finish
