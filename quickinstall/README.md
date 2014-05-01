# Accumulo Quickstart Description

[![Build Status](https://travis-ci.org/mjwall/accumulo-quickstart.png?branch=master)](https://travis-ci.org/mjwall/accumulo-quickstart)

## Quickstart

This script is designed to help you get setup with a local Accumulo
one node development environment.  It is not intended for production
installations.  The script will install the following into one
directory, configure them for you and start them up:

- Hadoop 1.2.1
- Zookeeper 3.3.6
- Accumulo 1.5.0

My intent is to provide an easy way for those interested in
Accumulo to get started without having to know much about how
Hadoop, Zookeeper or Accumulo work.  I do plan to use it in our
Accumulo Book, http://shop.oreilly.com/product/0636920032304.do

I considered using the MiniAccumuloCluster, but in the end felt it
would more useful for new users to have full installs of everything.

## Installation

To use this package, first clone the repo with

    git clone https://github.com/accumulobook/accumulo-quickstart.git

Then cd to to the quickinstall directory and run the following.

    ./bin/install

It will take a while to complete, as it must download the tar.gz files
for Accumulo, Hadoop and Zookeper.  Once complete, the install-home
directory will contain everything for this particular installation.
From the console, source the install-home/bin/cloud-env script to
setup some environment variables.  There are helper scripts in that
bin directory.

##  Prerequisites

This project only requires Java >= 1.6 be installed.  You will also
need password-less SSH setup.  There are many articles on how to do
this, including http://hortonworks.com/kb/generating-ssh-keys-for-passwordless-login/

SBT and Scala is used to download the files and setup the configs.
However, a copy of SBT version 0.13.0 is included, so you should not
need to do anything beside clone the repo and run `./bin/install`.

While not required, you may run into warning like this.

    WARN : Max files open on localhost is 64000, recommend 65536

There are many articles online about how to update this.  I am running on a Mac and
used the following, https://coderwall.com/p/lfjoaq

This project has not been tested on Windows.  I am unsure if Hadoop and
Accumulo will even run on Windows, so I didn't spend much time on it.  I did
work hard to not use Unix specific stuff except in the last step where
the namenode is formatted and Accumulo is init'd with a shell script.

## Usage

Hadoop, Zookeeper and Accumulo are full installations.  The typical
usage is to call executables in their respective bin directories.
That will still work with this installation.

This package also creates a set of helpers under the top level bin
directory.  To use these, source the cloud-env script in the install-home/bin
directory to setup some path.  Doing so will put the following on your path.

cloud-helpers

    cloud-env          - sets up the environment variables
    start-all          - starts hadoop, zookeeper and accumulo
    stop-all           - stops accumulo, hadoop and zookeeper

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

## Cleanup

Everything used by Hadoop, Zookeeper and Accumulo should be located in
the install-home directory.  If should be able to run `stop-all` and
simply remove that directory.

## Problems

If you run into issues, please create a ticket on github.  Feedback is appreciated.
