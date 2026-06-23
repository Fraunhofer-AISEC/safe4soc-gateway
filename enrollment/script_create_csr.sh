#!/usr/bin/env bash

RED='\e[31m'
GREEN='\e[32m'
YELLOW='\e[33m'
BLUE='\e[34m'
NC='\e[0m'  # No Color

abort() {
    echo -e "${RED}ERROR${NC}: $1" >&2
    exit 1
}

# Check if a DOMAIN_GATEWAY was provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <DOMAIN_GATEWAY>"
    exit 1
fi

DOMAIN_GATEWAY="$1"

cd /app/tmp || abort "Cannot access /app/tmp"

echo "Creating key and csr for gateway domain: $DOMAIN_GATEWAY"

openssl req -keyout client.key -subj "/CN=${DOMAIN_GATEWAY}" -nodes \
  -new -newkey rsa:3072 -outform der -out - \
  | base64 > client.csr.b64 || abort "Cannot create key and csr"

mv client.key ../key/client.key || abort "Cannot move client.key to key folder"

echo "End of script reached!"
