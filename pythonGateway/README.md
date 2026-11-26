# Safe4Soc Python gateway

## WSL or Linux

Safe4Soc Python Gateway

Prerequisites

Run all commands from the repository root with Python 3.10 or newer.

The application reads the shared schemas and ISAs under src/main/resources.

On Ubuntu or WSL, install the required Python components if they are not already available:
```
sudo apt update
sudo apt install python3 python3-venv python3-pip
```
Installation

Create a Python virtual environment and install the required dependencies:
```
python3 -m venv pythonGateway/.venv
./pythonGateway/.venv/bin/python -m pip install -r pythonGateway/requirements.txt
```
The dependencies are installed inside the virtual environment without modifying the system-wide Python environment.

Running the Gateway

Start a receiver in one terminal and a sender in another. Both terminals must be opened in the repository root.

Common Configuration

In both terminals, set the following environment variables to run the Bruno HTTP scenarios:
```
export IDMEFv2SchemaPath='src/main/resources/schemas/idmefv2-schema0.9.json'
export ENDPOINT_ACTIVE_SETISA=true
export ENDPOINT_ACTIVE_RELOADCONFIG=true
```
Receiver Terminal
```
export GATEWAY_ROLE=RECEIVER
./pythonGateway/.venv/bin/python -m pythonGateway
```
The receiver listens on port 10081 by default.

Sender Terminal
```
export GATEWAY_ROLE=SENDER
./pythonGateway/.venv/bin/python -m pythonGateway
```
The sender listens on port 10080 by default.

## Windows

Run from the repository root with Python 3.10 or newer. The application reads the shared schemas and ISAs under `src/main/resources`.

```powershell
python -m venv pythonGateway/.venv
./pythonGateway/.venv/Scripts/python -m pip install -r pythonGateway/requirements.txt
```

Start a receiver in one terminal and a sender in another. In both terminals, set the following variables to run the Bruno HTTP scenarios.

```powershell
$env:IDMEFv2SchemaPath = 'src/main/resources/schemas/idmefv2-schema0.9.json'
$env:ENDPOINT_ACTIVE_SETISA = 'true'
$env:ENDPOINT_ACTIVE_RELOADCONFIG = 'true'
```

Receiver terminal:

```powershell
$env:GATEWAY_ROLE = 'RECEIVER'
./pythonGateway/.venv/Scripts/python -m pythonGateway
```

Sender terminal:

```powershell
$env:GATEWAY_ROLE = 'SENDER'
./pythonGateway/.venv/Scripts/python -m pythonGateway
```

On Linux or macOS, use `export NAME=value` and `pythonGateway/.venv/bin/python`. In Bruno, use the local HTTP environment and confirm it points to ports 10080 and 10081. Start the Kafka REST Proxy and create the configured topic for scenarios that expect successful delivery. Run one scenario folder at a time, starting with its first request so it can install the expected ISA on both gateways.

## Configuration

The Python gateway reads environment variables. Most names match the Java Docker configuration.

| Variable | Default or purpose |
| --- | --- |
| `GATEWAY_ROLE` | `SENDER` or `RECEIVER`. Defaults to `SENDER`. |
| `GATEWAY_BIND`, `GATEWAY_PORT` | Bind address defaults to `127.0.0.1`. The port defaults to the role's configured port. |
| `GATEWAY_SENDER_DOMAIN`, `GATEWAY_SENDER_PORT` | `localhost`, `10080` |
| `GATEWAY_RECEIVER_DOMAIN`, `GATEWAY_RECEIVER_PORT` | `localhost`, `10081` |
| `GATEWAY_PROTOCOL` | Outbound scheme for the receiver and Kafka URLs. Defaults to `http`. |
| `GATEWAY_RECEIVER_URL` | Optional base URL override for the sender. |
| `GATEWAY_RECEIVER_USERNAME`, `GATEWAY_RECEIVER_PASSWORD` | Optional sender-to-receiver Basic authentication credentials. Set both to enable. |
| `KAFKA_LOCAL_DOMAIN`, `KAFKA_LOCAL_PORT`, `KAFKA_TOPIC` | `localhost`, `8082`, `alerts` |
| `ISA_PATH`, `IDMEFv2SchemaPath` | Overrides for the default `isa/isa.json` and `schemas/idmefv2-schema.json` resources. |
| `RELEVANT_HEADERS` | Optional comma-separated list of forwarded headers. Defaults to the Java list. |
| `ENDPOINT_ACTIVE_SETISA`, `ENDPOINT_ACTIVE_RELOADCONFIG` | Both default to `false`. Enable them for Bruno as shown above. |

`GET /status` returns the role and active ISA. `POST /isacheck` compares the full agreement. `POST /setisa` replaces the active agreement in memory, and `GET /reloadconfig` restores it from disk. The original ISA keys and metadata are preserved. Alert bodies require a Content-Length and may be up to 16 MiB. The gateway uses Python’s built-in HTTP server for local and integration use.

### TLS and mutual TLS

Use `SSL_INBOUND_ACTIVATED=true` for an HTTPS listener. Set `SSL_OUTBOUND_ACTIVATED=true` to load custom trust roots and an optional client identity for outbound HTTPS. The scheme still comes from the outbound URL. Standard certificate and hostname verification apply to HTTPS.

- `SSL_KEY_STORE` points to a PEM certificate chain. Use `SSL_KEY_FILE` for the private key if it is in a separate PEM file. `SSL_KEY_STORE_PASSWORD` supplies its password when encrypted.
- `SSL_TRUST_STORE` points to a PEM CA bundle. The development certificate script in `pki-test` generates PEM certificates, keys, and a CA certificate alongside its Java PKCS#12 stores. Use the PEM files for Python.
- Set `SSL_MTLS_SETTING` to `none`, `want`, or `need` for no, optional, or required inbound client authentication. `want` and `need` require a CA bundle.
- Prefix the key, certificate, password, or trust-store variables with `SSL_INBOUND_` or `SSL_OUTBOUND_` in place of `SSL_` to configure separate identities or CAs for each direction.
