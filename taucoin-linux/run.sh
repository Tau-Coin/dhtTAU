#!/bin/bash

# the script arguments should be as follows:
# ./run.sh '-dataDir /home/sxs/.jtau_test -rpcPort 9022'

echo "$@"

if [ "0" -eq "$#" ];
then
    ../gradlew -p ../taucoin-linux run
else
    ../gradlew -p ../taucoin-linux run --args="$@"
fi
