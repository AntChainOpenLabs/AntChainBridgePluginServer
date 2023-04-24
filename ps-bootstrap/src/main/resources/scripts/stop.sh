#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

log_info "stop plugin-server now..."

ps -ewf | grep -e "ps-bootstrap.*\.jar" | grep -v grep | awk '{print $2}' | xargs kill
if [ $? -ne 0 ]; then
    log_error "failed to stop plugin-server"
    exit 1
fi

log_info "plugin-server stopped successfully"