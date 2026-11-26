import os
import ssl
from dataclasses import dataclass


def flag(name, default=False):
    value = os.getenv(name, str(default)).strip().lower()
    if value not in {"true", "false"}:
        raise ValueError(f"{name} must be true or false")
    return value == "true"


def tls_context(inbound):
    if not flag("SSL_INBOUND_ACTIVATED" if inbound else "SSL_OUTBOUND_ACTIVATED"):
        return None
    prefix = "SSL_INBOUND_" if inbound else "SSL_OUTBOUND_"
    certificate = os.getenv(prefix + "KEY_STORE", os.getenv("SSL_KEY_STORE"))
    key = os.getenv(prefix + "KEY_FILE", os.getenv("SSL_KEY_FILE"))
    password = os.getenv(prefix + "KEY_STORE_PASSWORD", os.getenv("SSL_KEY_STORE_PASSWORD"))
    ca = os.getenv(prefix + "TRUST_STORE", os.getenv("SSL_TRUST_STORE"))
    if inbound:
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        modes = {"none": ssl.CERT_NONE, "want": ssl.CERT_OPTIONAL, "need": ssl.CERT_REQUIRED}
        mode = os.getenv("SSL_MTLS_SETTING", "none").lower()
        if mode not in modes:
            raise ValueError("SSL_MTLS_SETTING must be none, want, or need")
        context.verify_mode = modes[mode]
        if mode != "none" and not ca:
            raise ValueError("A trust store is required for inbound client authentication")
        if ca:
            context.load_verify_locations(cafile=ca)
        if not certificate:
            raise ValueError("A certificate is required for inbound TLS")
    else:
        context = ssl.create_default_context(cafile=ca)
    if certificate:
        context.load_cert_chain(certificate, keyfile=key, password=password)
    elif key:
        raise ValueError("A certificate is required when a TLS key file is specified")
    return context


@dataclass
class Settings:
    role: str
    bind: str
    port: int
    sender_domain: str
    sender_port: str
    receiver_domain: str
    receiver_port: str
    receiver_url: str
    receiver_username: str
    receiver_password: str
    kafka_url: str
    relevant_headers: tuple[str, ...]
    isa_path: str
    schema_path: str
    reload_enabled: bool
    set_isa_enabled: bool

    @classmethod
    def from_env(cls):
        env = os.getenv
        role = env("GATEWAY_ROLE", "SENDER").upper()
        if role not in {"SENDER", "RECEIVER"}:
            raise ValueError("GATEWAY_ROLE must be SENDER or RECEIVER")
        protocol = env("GATEWAY_PROTOCOL", "http").lower()
        if protocol not in {"http", "https"}:
            raise ValueError("GATEWAY_PROTOCOL must be http or https")
        sender_port = env("GATEWAY_SENDER_PORT", "10080")
        receiver_port = env("GATEWAY_RECEIVER_PORT", "10081")
        receiver_domain = env("GATEWAY_RECEIVER_DOMAIN", "localhost")
        kafka = f"{env('KAFKA_LOCAL_DOMAIN', 'localhost')}:{env('KAFKA_LOCAL_PORT', '8082')}"
        headers = env("RELEVANT_HEADERS",
                      "content-type,accept,user-agent,accept-encoding,"
                      "safe4soc-receiver-hostname,safe4soc-receiver-port")
        return cls(
            role=role,
            bind=env("GATEWAY_BIND", "127.0.0.1"),
            port=int(env("GATEWAY_PORT", sender_port if role == "SENDER" else receiver_port)),
            sender_domain=env("GATEWAY_SENDER_DOMAIN", "localhost"),
            sender_port=sender_port,
            receiver_domain=receiver_domain,
            receiver_port=receiver_port,
            receiver_url=(env("GATEWAY_RECEIVER_URL") or
                          f"{protocol}://{receiver_domain}:{receiver_port}").rstrip("/"),
            receiver_username=env("GATEWAY_RECEIVER_USERNAME", ""),
            receiver_password=env("GATEWAY_RECEIVER_PASSWORD", ""),
            kafka_url=f"{protocol}://{kafka}/topics/{env('KAFKA_TOPIC', 'alerts')}",
            relevant_headers=tuple(header.strip().lower() for header in headers.split(",")
                                   if header.strip()),
            isa_path=env("ISA_PATH", ""),
            schema_path=env("IDMEFv2SchemaPath", env("IDMEFV2_SCHEMA_PATH", "")),
            reload_enabled=flag("ENDPOINT_ACTIVE_RELOADCONFIG"),
            set_isa_enabled=flag("ENDPOINT_ACTIVE_SETISA"),
        )
