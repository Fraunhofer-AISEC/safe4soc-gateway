#!/usr/bin/env bash
set -euo pipefail

# Development-only mTLS certificate generator
PASSWORD="password"
DAYS_CA=3650
DAYS_CERT=825
KEY_SIZE=3072

OUT_DIR="${1:-./mtls-certs}"

mkdir -p "$OUT_DIR"
OUT_DIR="$(realpath "$OUT_DIR")"

command -v openssl >/dev/null || {
    echo "ERROR: openssl is not installed"
    exit 1
}

command -v keytool >/dev/null || {
    echo "ERROR: Java keytool is not installed"
    exit 1
}

echo "Generating local development CA..."

# --------------------------------------------------
# 1. Generate root CA
# --------------------------------------------------

openssl genrsa -out "$OUT_DIR/ca.key" 4096

openssl req -x509 -new -sha256 \
    -key "$OUT_DIR/ca.key" \
    -days "$DAYS_CA" \
    -out "$OUT_DIR/ca.crt" \
    -subj "/C=DE/O=Safe4Soc/OU=Development/CN=Safe4Soc Local Development CA" \
    -addext "basicConstraints=critical,CA:TRUE,pathlen:0" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" \
    -addext "subjectKeyIdentifier=hash"

# --------------------------------------------------
# 2. Create shared PKCS12 truststore
# --------------------------------------------------

keytool -importcert \
    -noprompt \
    -alias safe4soc-dev-ca \
    -file "$OUT_DIR/ca.crt" \
    -keystore "$OUT_DIR/truststore.p12" \
    -storetype PKCS12 \
    -storepass "$PASSWORD"

# --------------------------------------------------
# 3. Generate gateway certificates
# --------------------------------------------------

generate_gateway() {
    local DOMAIN="$1"
    local DIR="$OUT_DIR/$DOMAIN"

    mkdir -p "$DIR"

    echo "Generating certificate for $DOMAIN..."

    # Generate private key
    openssl genrsa \
        -out "$DIR/$DOMAIN.key" "$KEY_SIZE"

    # Generate certificate signing request
    openssl req -new \
        -key "$DIR/$DOMAIN.key" \
        -out "$DIR/$DOMAIN.csr" \
        -subj "/C=DE/O=Safe4Soc/OU=Development/CN=$DOMAIN"

    # Certificate extensions
    cat > "$DIR/extensions.cnf" <<EOF
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth,clientAuth
subjectAltName=DNS:$DOMAIN
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer
EOF

    # Sign certificate using local CA
    openssl x509 -req \
        -in "$DIR/$DOMAIN.csr" \
        -CA "$OUT_DIR/ca.crt" \
        -CAkey "$OUT_DIR/ca.key" \
        -CAcreateserial \
        -out "$DIR/$DOMAIN.crt" \
        -days "$DAYS_CERT" \
        -sha256 \
        -extfile "$DIR/extensions.cnf"

    # Create PKCS12 keystore
    openssl pkcs12 -export \
        -name "1" \
        -inkey "$DIR/$DOMAIN.key" \
        -in "$DIR/$DOMAIN.crt" \
        -certfile "$OUT_DIR/ca.crt" \
        -out "$DIR/keystore.p12" \
        -passout "pass:$PASSWORD"

    # Copy shared truststore for convenient deployment
    cp "$OUT_DIR/truststore.p12" "$DIR/truststore.p12"

    echo "Created $DOMAIN certificates and keystore."
}

generate_gateway "gateway-sender"
generate_gateway "gateway-receiver"

echo
echo "============================================"
echo "mTLS certificates generated successfully!"
echo "============================================"
echo
echo "Output directory: $OUT_DIR"
echo
echo "Truststore password: $PASSWORD"
echo "Keystore password:   $PASSWORD"
echo
echo "Files:"
find "$OUT_DIR" -maxdepth 2 -type f | sort
