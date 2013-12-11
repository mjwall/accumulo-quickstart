# Accumulo Quickstart

## Description
This script is designed to help you get setup with a local Accumulo
one node development environment.  It is not intended for production installations.  The script will install the following into one directory, configure them for you and start them up:

- Hadoop 1.0.4
- Zookeeper 3.3.6
- Accumulo 1.5.0


## Installation
To run this script, clone the repo and run the following from the root directory.

    ./bin/install

It will take a while to complete, as it must download the tar.gz files
for Accumulo, Hadoop and Zookeper.  Once complete, the cloud-install
directory will contain everything for this particular installation.
From the console, source the cloud-install/bin/cloud-env script to
setup some environment variables.  There are helper scripts in that
bin directory.

##  Prerequisites

This script use SBT version 0.13.0, but a copy is included so you only
need Java >= 1.6. You will also need password-less SSH setup.  There
are many articles on how to do this, including
http://hortonworks.com/kb/generating-ssh-keys-for-passwordless-login/.

## Usage

Hadoop, Zookeeper and Accumulo are full installations.  The typical
usage is to call executables in their respective bin directories.
That will still work with this installation.

This package also creates a set of helpers under the top level bin
directory.  To use these, source the cloud-env script in that
directory to setup some path.  Doing so will put the following on your path.

cloud-helpers

    cloud-env          - sets up the environment variables
    start-all          - starts hadoop, zookeeper and cloudbase/accumulo
    stop-all           - stops cloudbase/accumulo, hadoop and zookeeper

hadoop helpers

    hd-api             - opens the hadoop javadoc in your browser
    hd-start-all       - start hadoop, calls HADOOP_PREFIX/bin/start-all.sh
    hd-stop-all        - stop hadoop

zookeeper helpers

    zkCli              - client for zookeeper
    zkServer           - used to start and stop the zookeeper server
    zk-start           - start zookeeper
    zk-stop            - stop zookeeper

accumulo helpers

    acc-api            - open the accumulo javadoc in firefox
    ashell             - runs accumulo shell, don't forget -u user
    acc-start-all      - start accumulo, call ACCUMULO_HOME/bin/start-all.sh
    acc-stop-all       - stop accumulo
    accumulo           - the accumulo script
