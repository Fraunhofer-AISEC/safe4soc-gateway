import logging
from pathlib import Path

import simplejson as json
from jsonschema import Draft4Validator
from jsonschema.validators import validator_for
from referencing import Registry

from .policy import IsaPolicy, expect, json_equal


RESOURCES = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
LOGGER = logging.getLogger(__name__)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate field '{key}'")
        result[key] = value
    return result


def read_object(body):
    return expect(json.loads(body, use_decimal=True, object_pairs_hook=unique_object),
                  dict, "object")


def encode(value):
    return json.dumps(value, ensure_ascii=True, separators=(",", ":")).encode("utf-8")


def resource(configured_path, fallback):
    if configured_path:
        path = Path(configured_path)
        if path.is_file():
            return path.read_bytes()
        bundled = (RESOURCES / configured_path.removeprefix("classpath:").lstrip("/")).resolve()
        if bundled.is_relative_to(RESOURCES) and bundled.is_file():
            return bundled.read_bytes()
        LOGGER.warning("Could not find %s. Using bundled %s.", configured_path, fallback)
    return (RESOURCES / fallback).read_bytes()


class IsaService:
    def __init__(self, path):
        self.path = path
        self.reload()

    def reload(self):
        self.update(resource(self.path, "isa/isa.json"))

    def update(self, body):
        self.current = IsaPolicy(read_object(body))

    def matches(self, body):
        return json_equal(read_object(body), self.current.agreement, compare_numeric_types=False)


class AlertRejected(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status


def pointer(parts):
    return "".join("/" + str(part).replace("~", "~0").replace("/", "~1") for part in parts)


class AlertProcessor:
    def __init__(self, schema_path, isa):
        self.isa = isa
        schema = read_object(resource(schema_path, "schemas/idmefv2-schema.json"))
        validator = validator_for(schema, default=Draft4Validator)
        validator.check_schema(schema)
        self.validator = validator(schema, format_checker=validator.FORMAT_CHECKER, registry=Registry())

    def violations(self, alert):
        return {
            (pointer(error.absolute_path), pointer(error.absolute_schema_path), error.message)
            for error in self.validator.iter_errors(alert)
        }

    def validate(self, alert, direction):
        errors = self.violations(alert)
        if errors:
            details = ", ".join(f"{path or '$'}: {message}" for path, _, message in sorted(errors))
            raise AlertRejected(422, f"{direction} alert is not valid against JSON Schema\n[{details}]")

    def process(self, body):
        try:
            alert = read_object(body)
        except ValueError as error:
            raise AlertRejected(400, f"Invalid JSON: {error}") from error
        self.validate(alert, "Incoming")
        rejection = self.isa.current.apply(alert, self.violations)
        if rejection:
            raise AlertRejected(422, rejection)
        self.validate(alert, "Outgoing")
        return alert
