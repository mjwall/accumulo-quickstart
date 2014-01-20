#!/bin/bash

source "$(dirname $0)/../install-home/bin/cloud-env"

read -d '' CMDS << EOC
createtable test1
insert "a" "b" "c" "d"
scan -r "a"
deletetable -f test1
exit
EOC

$ACCUMULO_HOME/bin/accumulo shell -u root -p secret -e "$CMDS"
