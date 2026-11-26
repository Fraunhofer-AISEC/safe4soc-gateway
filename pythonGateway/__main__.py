import logging
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from .config import Settings, tls_context
from .processing import AlertProcessor, AlertRejected, IsaService, encode
from .transport import AlertTransport


LOGGER = logging.getLogger(__name__)
MAX_BODY = 16 * 1024 * 1024


class GatewayServer(ThreadingHTTPServer):
    def __init__(self, settings):
        self.settings = settings
        self.inbound_tls = tls_context(inbound=True)
        self.isa = IsaService(settings.isa_path)
        self.processor = AlertProcessor(settings.schema_path, self.isa)
        self.transport = AlertTransport(settings, tls_context(inbound=False))
        super().__init__((settings.bind, settings.port), GatewayHandler)

    def process_request_thread(self, request, client_address):
        request.settimeout(30)
        if self.inbound_tls is not None:
            try:
                request = self.inbound_tls.wrap_socket(request, server_side=True)
            except OSError:
                LOGGER.debug("Inbound TLS handshake failed", exc_info=True)
                self.shutdown_request(request)
                return
        super().process_request_thread(request, client_address)


class GatewayHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.dispatch()

    def do_POST(self):
        self.dispatch()

    def log_message(self, fmt, *args):
        LOGGER.info("%s %s", self.address_string(), fmt % args)

    def respond(self, status, body, encoding=None):
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        if encoding:
            self.send_header("Content-Encoding", encoding)
        self.end_headers()
        self.wfile.write(body)

    def read_body(self):
        if self.headers.get("Transfer-Encoding"):
            raise AlertRejected(400, "Invalid JSON: expected a Content-Length request body")
        lengths = self.headers.get_all("Content-Length", [])
        if len(lengths) != 1 or not lengths[0].isascii() or not lengths[0].isdecimal():
            raise AlertRejected(400, "Invalid JSON: missing or invalid Content-Length")
        digits = lengths[0].lstrip("0") or "0"
        if len(digits) > len(str(MAX_BODY)) or int(digits) > MAX_BODY:
            raise AlertRejected(413, "Request body is too large")
        length = int(digits)
        body = self.rfile.read(length)
        if len(body) != length:
            raise AlertRejected(400, "Invalid JSON: incomplete request body")
        return body

    def dispatch(self):
        path = self.path.split("?", 1)[0]
        isa = self.server.isa
        settings = self.server.settings
        try:
            if self.command == "GET" and path == "/status":
                self.respond(200, f"Gateway {settings.role} online.\nISA: ".encode() +
                             encode(isa.current.agreement))
            elif self.command == "GET" and path == "/reloadconfig":
                if not settings.reload_enabled:
                    self.respond(406, "Configuration reload endpoint deactivated.")
                    return
                try:
                    isa.reload()
                except (ValueError, OSError):
                    LOGGER.exception("ISA reload failed")
                    self.respond(500, "Could not reload ISA configuration.")
                    return
                self.respond(200, "Configuration reload attempt was processed, please see internal logs for results.")
            elif self.command == "POST" and path in {"/setisa", "/isacheck"}:
                self.handle_isa(path)
            else:
                alert = self.server.processor.process(self.read_body())
                headers = {key.lower(): value for key, value in self.headers.items()}
                self.respond(*self.server.transport.forward(self.command, path, headers, alert))
        except AlertRejected as error:
            self.respond(error.status, str(error))
        except TimeoutError:
            self.respond(408, "Request timed out")

    def handle_isa(self, path):
        isa = self.server.isa
        if path == "/setisa" and not self.server.settings.set_isa_enabled:
            self.respond(406, "SetISA configuration endpoint deactivated.")
            return
        body = self.read_body()
        try:
            if path == "/setisa":
                isa.update(body)
                self.respond(200, "Processed ISA update.")
            elif isa.matches(body):
                self.respond(200, "ISA compatible.")
            else:
                self.respond(409, "ISA incompatible.")
        except ValueError as error:
            self.respond(400, f"Invalid ISA JSON: {error}")


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    with GatewayServer(Settings.from_env()) as server:
        settings = server.settings
        protocol = "https" if server.inbound_tls else "http"
        LOGGER.info("Gateway %s listening at %s://%s:%s", settings.role, protocol,
                    *server.server_address)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == "__main__":
    main()
