from copy import deepcopy
from datetime import datetime, timezone
from decimal import Decimal


SCALAR_FIELDS = {
    "Version", "ID", "Status", "OrganisationName", "OrganisationId",
    "CreateTime", "StartTime", "Priority", "Confidence", "Description", "Cause",
}
ARRAY_FIELDS = {"Sensor", "Source", "Target"}


def expect(value, kind, name):
    if not isinstance(value, kind):
        raise ValueError(f"Expected a JSON {name}")
    return value


def json_equal(left, right, *, compare_numeric_types):
    if type(left) is not type(right):
        if compare_numeric_types or isinstance(left, bool) or isinstance(right, bool):
            return False
        return isinstance(left, (int, Decimal)) and isinstance(right, (int, Decimal)) and left == right
    if isinstance(left, dict):
        return left.keys() == right.keys() and all(
            json_equal(left[key], right[key], compare_numeric_types=compare_numeric_types)
            for key in left
        )
    if isinstance(left, list):
        return len(left) == len(right) and all(
            json_equal(a, b, compare_numeric_types=compare_numeric_types)
            for a, b in zip(left, right)
        )
    return left == right


def missing_field(alert, config):
    for field, instruction in config.items():
        value = alert.get(field)
        if value is None:
            return field
        if isinstance(instruction, dict):
            if not isinstance(value, dict) or missing_field(value, instruction) is not None:
                return field
    return None


def filter_object(target, rules, path, blanks):
    if not rules:
        target.clear()
    for field, replacement in rules.items():
        if field not in target or replacement is None:
            continue
        if replacement == "":
            if isinstance(target[field], list):
                target[field].clear()
            else:
                blanks.append((*path, field))
        else:
            target[field] = deepcopy(replacement)


def filter_array(entries, replacement, path, blanks):
    if isinstance(replacement, str):
        if not replacement:
            entries.clear()
        for entry in entries:
            target = expect(entry, dict, "object")
            if "IP" in target:
                target["IP"] = replacement
        return
    rules = expect(replacement, dict, "object")
    if not rules:
        entries.clear()
    for index, entry in enumerate(entries):
        filter_object(expect(entry, dict, "object"), rules, (*path, index), blanks)


def apply_filter(alert, config, violations):
    blanks = []
    for field, replacement in config.items():
        try:
            if field in SCALAR_FIELDS:
                if replacement == "":
                    blanks.append((field,))
                elif replacement is not None:
                    alert[field] = deepcopy(replacement)
            elif field == "Category":
                expect(alert.get(field), list, "array").clear()
            elif field == "Analyzer" and field in alert:
                filter_object(expect(alert[field], dict, "object"),
                              expect(replacement, dict, "object"), (field,), blanks)
            elif field in ARRAY_FIELDS and field in alert:
                filter_array(expect(alert[field], list, "array"),
                             replacement, (field,), blanks)
        except ValueError:
            return field

    if blanks:
        # Keep a blank only if it introduces no schema violations.
        current_errors = violations(alert)
        for path in blanks:
            target = alert
            for part in path[:-1]:
                target = target[part]
            field = path[-1]
            target[field] = ""
            errors = violations(alert)
            if errors <= current_errors:
                current_errors = errors
            else:
                del target[field]
                current_errors = violations(alert)
    return None


def enrich(alert, config):
    for field, instruction in config.items():
        if field in {"CreateTime", "StartTime"}:
            if field not in alert or not expect(alert[field], str, "string"):
                now = datetime.now(timezone.utc)
                alert[field] = now.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-4] + "Z"
        elif field in SCALAR_FIELDS | {"Category", "Analyzer"}:
            rules = expect(instruction, dict, "object")
            target = expect(alert.get(field), dict, "object")
            if not rules:
                target.clear()
        elif field == "Sensor":
            expect(alert.get(field), list, "array").clear()
        elif field == "Source":
            rules = expect(instruction, dict, "object")
            targets = expect(alert.get(field), list, "array")
            if not rules:
                targets.clear()


class IsaPolicy:
    def __init__(self, agreement):
        self.agreement = agreement
        for section in ("dropConfig", "requiredConfig", "filterConfig", "enrichmentConfig"):
            if not isinstance(agreement.get(section), dict):
                raise ValueError(f"Expected a JSON object for ISA section {section}")

    def apply(self, alert, violations):
        for field, forbidden in self.agreement["dropConfig"].items():
            if field not in SCALAR_FIELDS | {"Category"}:
                continue
            value = alert.get(field)
            if value is not None and not isinstance(value, (list, dict)):
                if json_equal(value, forbidden, compare_numeric_types=True):
                    return f"Alert rejected: forbidden value found in {field}"

        missing = missing_field(alert, self.agreement["requiredConfig"])
        if missing is not None:
            return f"Alert rejected: missing mandatory field {missing}"
        unfilterable = apply_filter(alert, self.agreement["filterConfig"], violations)
        if unfilterable:
            return f"Alert cant be filtered: missing field {unfilterable}"
        try:
            enrich(alert, self.agreement["enrichmentConfig"])
        except ValueError as error:
            return f"Alert cannot be enriched: {error}"
        return None
