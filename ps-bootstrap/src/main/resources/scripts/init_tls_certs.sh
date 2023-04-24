#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

if [ ! -d ${CURR_DIR}/../certs ]; then
	mkdir -p ${CURR_DIR}/../certs
fi

openssl genrsa -out ${CURR_DIR}/../certs/server.key 2048 > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to generate server.key"
    exit 1
fi
openssl pkcs8 -topk8 -inform pem -in ${CURR_DIR}/../certs/server.key -nocrypt -out ${CURR_DIR}/../certs/server_pkcs8.key
if [ $? -ne 0 ]; then
    log_error "failed to generate pkcs8 server.key"
    exit 1
fi
mv ${CURR_DIR}/../certs/server_pkcs8.key ${CURR_DIR}/../certs/server.key
log_info "generate server.key successfully"

openssl req -new -x509 -days 36500 -key ${CURR_DIR}/../certs/server.key -out ${CURR_DIR}/../certs/server.crt -subj "/C=CN/ST=mykey/L=mykey/O=mykey/OU=mykey/CN=pluginserver"
if [ $? -ne 0 ]; then
    log_error "failed to generate server.crt"
    exit 1
fi
log_info "generate server.crt successfully"

if [ ! -f "trust.crt" ]; then
    cp ${CURR_DIR}/../certs/server.crt ${CURR_DIR}/../certs/trust.crt
    log_info "generate trust.crt successfully"
fi