# Gateway test runs

Run one scenario folder at a time and start from its first request. The four `TEST-ALERT-ADMISSION`, `TEST-DROP-PRIORITY`, `TEST-REQUIRED-ALERT-FIELDS`, and `TEST-FILTER-RECEIVER-CHECKS` folders install their own sender and receiver policies.

Use a dedicated test deployment with IDMEFv2 0.9, both gateways, ISA updates enabled, and a reachable Kafka REST Proxy and topic. Select a Bruno environment that defines `PROTOCOL`, `HOSTNAME`, `SENDER_PORT`, `RECEIVER_PORT`, and `KAFKE_TOPIC`. For HTTPS, configure Bruno to trust the server certificates and supply a client certificate if your listener requires one.

These scenarios replace both live ISAs, and successful alerts are written to Kafka. To restore the configured ISA files after a run, use the existing **Reload Config** requests for both gateways when reload is enabled. If the deployment had an in-memory ISA override you want to keep, save it with the **Status** requests before running and reapply it afterward.
