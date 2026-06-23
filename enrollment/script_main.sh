#!/usr/bin/env bash
set -e
RED='\e[31m'
GREEN='\e[32m'
YELLOW='\e[33m'
BLUE='\e[34m'
GRAY='\e[90m'
NC='\e[0m'  # No Color

abort() {
    echo -e "${RED}ERROR${NC}: $1" >&2
    exit 1
}

echo "Checking if p12 truststore with CA info can be found."
if [ -f "/app/archives/truststore.p12" ]; then
    echo -e "${GREEN}Truststore found!${NC} Not creating new ones."
    echo -e "${GRAY}Delete existing archives/truststore.p12 and relaunch for new material!${NC}"
else
    echo -e "${YELLOW}Truststore p12-archive missing.${NC}"
    echo "Attempting retrieval from $DOMAIN_PKI"
    bash ./script_store_ca_truststore.sh $DOMAIN_PKI || abort "script_store_ca_truststore.sh"
fi

echo "Checking if p12 keystore with certificate can be found."
if [ -f "/app/archives/keystore.p12" ]; then
    echo -e "${GREEN}Keystore found!${NC} Not creating new ones."
    echo -e "${GRAY}Delete existing archives/keystore.p12 and relaunch for new material!${NC}"
else
    echo -e "${YELLOW}Keystore p12-archive missing!${NC}"
    echo "Checking if client.key was created"
    if [ -f "/app/key/client.key" ]; then
        echo -e "${GREEN}Key material found!${NC} Not creating new ones."
        echo -e "${GRAY}Delete existing key/client.key and relaunch for new material!${NC}"
    else
        echo -e "${YELLOW}Key material not found!${NC}"
        echo "Creating new key and CSR."
        bash ./script_create_csr.sh $DOMAIN_GATEWAY || abort "script_create_csr.sh"
        bash ./script_send_csr.sh $DOMAIN_PKI || abort "script_send_csr.sh"
        echo -e "${GREEN}CSR was registered at CA, waiting for approval from CA!${NC}"
    fi
    echo "Attempting to store p12 keystore with certificate"
    # default value if not set
    REPEATS=${REPEATS:-1}
    echo "Running $REPEATS times with each $SECONDS seconds"
    for ((i=1; i<=REPEATS; i++)); do
      echo "Iteration $i"
      bash ./script_store_client_keystore.sh $DOMAIN_PKI \
        && { echo -e "${GREEN}Enrollment finished!${NC}" ; exit 0; }
      sleep ${SECONDS}
    done
    echo -e "${YELLOW}Enrollment not finished! - Giving up for now!${NC}"
    exit 1
fi

