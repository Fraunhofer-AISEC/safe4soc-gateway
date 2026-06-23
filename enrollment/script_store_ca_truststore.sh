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

echo "Storing CA Certs from: $DOMAIN_PKI"
cd /app/tmp || abort "cant access folder /app/tmp"
curl -k -s "https://$DOMAIN_PKI/.well-known/est/cacerts" > ca-chain.pem.pkcs7.b64 || abort "cant curl"
cat ca-chain.pem.pkcs7.b64  | openssl base64 -d -A > ca-chain.pem.pkcs7 || abort "cant transform"
cat ca-chain.pem.pkcs7 | openssl pkcs7 -inform DER -print_certs > ca-chain.pem || abort "cant transform"

echo "Printing received certs:"
cat ca-chain.pem || abort
echo "Splitting received certs:"
csplit -s -z -f ca- ca-chain.pem '/-----BEGIN CERTIFICATE-----/' '{*}' || abort "cant split certificates"

# Inspect truststore
echo "Inspecting Truststore"
openssl x509 -in ca-01 -noout -subject -issuer || abort
openssl x509 -in ca-02 -noout -subject -issuer || abort

echo "Making p12 archive"
keytool -importcert \
  -alias issuing-ca \
  -file ca-01 \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass password \
  -noprompt || abort

keytool -importcert \
  -alias root-ca \
  -file ca-02 \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass password \
  -noprompt || abort

#Verify Truststore
keytool -list \
  -v \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass password || abort

mv truststore.p12 ../archives/truststore.p12 || abort

echo "End of script reached!"
