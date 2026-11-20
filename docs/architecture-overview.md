# Architecture Overview

![Architecture Image should be here](./documentation-figures/Architecture.drawio.png)

The gateway consists of a sender gateway on the sender side and the receiving gateway on the receiver side.
As database for storing the received alert messages Kafka is used.
Kafka does not natively support JSON objects send via HTTP REST, therefore a translation was provided by the Kafka 
developers to translate HTTP Rest API calls into native Kafka protocol communication.

The sender gateway receives IDMEFv2 as JSON formatted alert message via HTTP message. 
The gateway sender afterward performs configured filtering or enrichment and forwards the 
alert to the receiver side gateway. 
Here the alert is also filtered or enriched as configured and afterward sent to the Kafka HTTP Rest API container.
The Kafka REST API container does the translation to the final destination, the Kafka database which is locally 
reachable on the receiver side.

## TOC

Architecture Overview(here)

[Compilation Instructions](compilation.md)

[Configuration Instructions](configuration.md)

[Testing Instructions](testing.md)

[Demonstration Instructions](demonstration.md)

[Additional Links](additional-links.md)