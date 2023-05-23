#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

log_info "start plugin-server now..."

JAR_FILE=`ls ${CURR_DIR}/../lib/ | grep '.jar'`

java -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application.yml > /dev/null 2>&1 &
if [ $? -ne 0 ]; then
    log_error "failed to start plugin-server"
    exit 1
fi

log_info "plugin-server started successfully"