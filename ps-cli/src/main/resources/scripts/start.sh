#!/bin/bash

bin=`dirname "${BASH_SOURCE-$0}"`
CLI_HOME=`cd "$bin"; pwd`

which java > /dev/null
if [ $? -eq 1 ]; then
    echo "no java installed. "
    exit 1
fi

Help=$(cat <<-"HELP"

 start.sh — Start the CLI tool to manage your plugin server

 Usage:
   start.sh <params>

 Examples:
  1. print help info:
   start.sh -h
  2. identify the port number to start CLI
   start.sh -p 9091
  3. identify the server IP to start CLI
   start.sh -H 0.0.0.0

 Options:
   -h         print help info
   -p         identify the port number to start CLI, default 9091
   -H         identify the server IP to start CLI, default 0.0.0.0

HELP
)

while getopts "hH:p:" opt
do
  case "$opt" in
    "h")
      echo "$Help"
      exit 0
      ;;
    "H")
      HOST_IP="${OPTARG}"
      ;;
    "p")
      PORT=${OPTARG}
	    ;;
    "?")
      echo "invalid arguments. "
      exit 1
      ;;
    *)
      echo "Unknown error while processing options"
      exit 1
      ;;
  esac
done

JAR_FILE=`ls ${CLI_HOME}/../lib`

java -jar ${CLI_HOME}/../lib/${JAR_FILE} -p ${PORT:-9090} -H ${HOST_IP:-0.0.0.0}