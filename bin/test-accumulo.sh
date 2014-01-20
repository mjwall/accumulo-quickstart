#!/bin/bash

source "$(dirname $0)/../install-home/bin/cloud-env"

read -d '' CMDS << EOC
createtable test1
insert "a" "b" "c" "d"
scan -r "a"
deletetable -f test1
exit
EOC

OUTPUT=$($ACCUMULO_HOME/bin/accumulo shell -u root -p secret -e "$CMDS")

echo "OUTPUT"
echo "${OUTPUT}"
echo ""

read -d '' EXPECTED <<EOE
a b:c []    d
Table: [test1] has been deleted.
EOE

# extra space after actual output
if [ "${OUTPUT}" == "${EXPECTED} " ]; then
  echo "Test passed"
  exit 0
else
  echo "Test failed, expected:"
  echo "${EXPECTED}"
  exit 1
fi
