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

# Check if a DOMAIN_PKI was provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <DOMAIN_PKI>"
    exit 1
fi

DOMAIN_PKI="$1"

echo "Sending CSR to $DOMAIN_PKI"

curl -k -v -H "Content-Type: application/pkcs10" \
  --data @/app/tmp/client.csr.b64 https://$DOMAIN_PKI/.well-known/est/simpleenroll \
 || abort "Cannot send csr"

echo "End of script reached!"
