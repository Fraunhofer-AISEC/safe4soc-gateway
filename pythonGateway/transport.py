import base64
import http.client
import logging
import socket
import ssl
from urllib.parse import urlsplit

from .processing import encode


LOGGER = logging.getLogger(__name__)


class AlertTransport:
    def __init__(self, settings, context):
        self.settings = settings
        self.context = context

    def forward(self, method, path, incoming, alert):
        settings = self.settings
        headers = {name: incoming[name] for name in settings.relevant_headers if name in incoming}
        if settings.role == "SENDER":
            url = settings.receiver_url + path
            headers["safe4soc-sender-hostname"] = settings.sender_domain
            headers["safe4soc-sender-port"] = settings.sender_port
            if settings.receiver_username.strip() and settings.receiver_password.strip():
                credentials = f"{settings.receiver_username}:{settings.receiver_password}".encode()
                headers["authorization"] = "Basic " + base64.b64encode(credentials).decode("ascii")
        else:
            url = settings.kafka_url
            headers["safe4soc-receiver-hostname"] = settings.receiver_domain
            headers["safe4soc-receiver-port"] = settings.receiver_port
            headers["content-type"] = "application/vnd.kafka.json.v2+json"
            alert = {"records": [{"key": "test", "value": alert}]}
        body = encode(alert) if method == "POST" else None
        return self.send(url, method, headers, body)

    def send(self, url, method, headers, body):
        connection = None
        try:
            address = urlsplit(url)
            if address.scheme == "https":
                connection = http.client.HTTPSConnection(address.hostname, address.port, timeout=10,
                                                         context=self.context)
            elif address.scheme == "http":
                connection = http.client.HTTPConnection(address.hostname, address.port, timeout=10)
            else:
                raise ValueError("Destination URL must use HTTP or HTTPS")
            try:
                connection.connect()
            except TimeoutError:
                return 503, b"Receiver is unreachable", None
            connection.request(method, address.path or "/", body, headers)
            with connection.getresponse() as response:
                status = response.status
                if status == 401:
                    return 401, f"Receiver authentication failed: url={url}".encode(), None
                if status == 403:
                    return 403, f"Receiver authorization denied: url={url}".encode(), None
                if status >= 500:
                    return 502, f"Receiver returned an internal server error: url={url}".encode(), None
                return status, response.read(), response.getheader("Content-Encoding")
        except ssl.SSLCertVerificationError:
            return 502, b"TLS or mTLS handshake failed", None
        except ssl.SSLError as error:
            handshake = connection is None or connection.sock is None or "ALERT" in (error.reason or "")
            message = b"TLS or mTLS handshake failed" if handshake else b"TLS communication failed"
            return 502, message, None
        except socket.gaierror:
            return 502, b"Receiver hostname cannot be resolved", None
        except ConnectionRefusedError:
            return 503, b"Receiver is unreachable", None
        except TimeoutError:
            return 504, b"Receiver request timed out", None
        except (OSError, http.client.HTTPException):
            return 502, b"Network error while contacting receiver", None
        except ValueError:
            LOGGER.exception("Invalid forwarding configuration")
            return 500, b"Internal gateway error", None
        finally:
            if connection is not None:
                connection.close()
