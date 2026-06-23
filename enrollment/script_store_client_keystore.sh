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

echo "Storing client keystore from: $DOMAIN_PKI"
cd /app/tmp || abort "cant access folder /app/tmp"

curl -sk \
  -H "Content-Type: application/pkcs10" \
  --data @client.csr.b64 \
  https://$DOMAIN_PKI/.well-known/est/simpleenroll \
  -o client.cert.p7b.b64 || abort "cant curl"

echo -e "${GREEN}Received the following reply from CA${NC}"
cat client.cert.p7b.b64 || abort

grep -qF "Request Pending - Retry Later" client.cert.p7b.b64 \
  && { echo -e "${YELLOW}Still waiting for CA approval!${NC}" ; exit 1; }

echo "Decoding b64"
base64 -d client.cert.p7b.b64 > client.cert.p7b || abort "cant b64 decode"

echo "Transforming p7b to pem"
openssl pkcs7 -inform DER \
  -in client.cert.p7b \
  -print_certs \
  -out client.cert.pem || abort

cat client.cert.pem || abort

echo "Creating p12 archive"
openssl pkcs12 -export \
  -inkey ../key/client.key \
  -in client.cert.pem \
  -out client.p12 \
  -name 1 \
  -passout pass:password || abort

echo "Storing p12 archive at archives/keystore.p12"
mv client.p12 ../archives/keystore.p12 || abort

echo "Enrollment ready!"
echo "End of script reached!"
exit 0