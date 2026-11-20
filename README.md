# SAFE4SOC Information Sharing Gateway

SAFE4SOC Information Sharing Gateway (S4S-IS-GW)
The SAFE4SOC Information Sharing Gateway (S4S-IS-GW) is a cybersecurity software component developed within the European research and innovation project [SAFE4SOC](https://safe4soc.eu/), funded under the EU Digital Europe Programme.

Its primary purpose is to enable secure, interoperable, and automated exchange of cybersecurity information between different organizations and security infrastructures, particularly Security Operations Centers (SOCs).

The gateway acts as an intermediary between cybersecurity systems, allowing them to exchange security alerts and incident-related information while enforcing configurable security and information-sharing policies.

## Key objectives
1. Interoperability: Facilitate standardized cybersecurity information exchange using the Incident Detection Message Exchange Format version 2 (IDMEFv2).
2. Secure communication: Protect exchanged information through encrypted communication and mutual TLS (mTLS) authentication.
3. Information filtering: Apply configurable policies to determine which cybersecurity information may be shared with other organizations.
4. Data validation: Validate exchanged security alerts against predefined JSON schemas to ensure structural correctness and interoperability.
5. Integration: Connect existing SOC infrastructures and security monitoring systems without requiring major changes to their internal architectures.

## Documentation

Installation, usage, and configuration instructions can be found in the [Project Documentation](./docs/main.md).

## How to Build and Run

Please see the [Compilation Documentation](docs/compilation.md).

## Maintenance

This project is a sample show-case that is used in the corresponding SAFE4SOC demonstrator, to demonstrate how cross-border Security Operation Centers can share their alert information, despite using different software stacks in the processing.

The project will be actively maintained during the project timeline of [SAFE4SOC](https://safe4soc.eu/).