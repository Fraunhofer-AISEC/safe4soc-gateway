# Testing

This section details how alerts can be sent through the gateways.

The gateways can be tested using a postman collection, one for either a secured gateway or an insecure
configured gateway using http for development purposes.
Get postman here: https://www.postman.com/downloads/
Once installed the testing collection (SAFE4SOC.postman_collection.json) which contains predefined REST API requests
can be imported via File -> Import.

The following sections show sample usages of the API using curl and command-line only.

## HTTPS

### Message Post gateway-sender

```bash
curl --location 'https://localhost:10080/topics/alerts' \
--header 'Content-Type: application/vnd.kafka.json.v2+json' \
--data '{
    "Version": "2.D.V08",
    "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a2f",
    "Status": [
        "Incident"
    ],
    "OrganisationName": "acme.eu",
    "CreateTime": "2026-11-10T10:00:30Z",
    "StartTime": "2026-11-10T10:00:30Z",
    "Cause": "Malicious",
    "Category": [
        "Theft.Machinery"
    ],
    "Confidence": 0.85,
    "Description": "Unauthorized removal of a secure laptop from a restricted server room detected via badge access logs and motion sensors.",
    "Analyzer": {
        "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a21",
        "IP": "192.168.100.10",
        "Name": "Security Control Panel",
        "Hostname": "security-hub.acme.eu",
        "Model": "Honeywell Pro-Watch",
        "Category": [
            "ID.IAM"
        ],
        "Data": [
            "Contact",
            "Vibration"
        ],
        "Method": [
            "Threshold"
        ],
        "GeoLocation": "+41.9028,+12.4964,+50",
        "UnLocation": "IT ROM",
        "Location": "ACME-ROME-SERVER1"
    },
    "Sensor": [
        {
            "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a22",
            "IP": "192.168.100.10",
            "Name": "Security Control Panel",
            "CaptureZone": "[{\"type\":\"physical-area\",\"spec_version\":\"2.1\",\"id\": \"physical-area--abcd1234-5678-90ef-abcd-1234567890ef\",\"value\": \"Server Room A - Floor 2\"}]"
        }
    ],
    "Source": [
        {
            "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a23",
            "IP": "192.168.200.50",
            "Protocol": [
                "BadgeAccess"
            ]
        }
    ],
    "Target": [
        {
            "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a24",
            "IP": "192.168.100.22",
            "Port": [],
            "Location": "Secure Server Room - Rack 5"
        }
    ]
}'
```

### Message Post gateway-receiver

```bash
curl --location 'https://localhost:10081/topics/alerts' \
--header 'Content-Type: application/vnd.kafka.json.v2+json' \
--data '{
    "Version": "2.D.V08",
    "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a2f",
    "Status": [
        "Incident"
    ],
    "OrganisationName": "acme.eu",
    "CreateTime": "2026-11-10T10:00:30Z",
    "StartTime": "2026-05-15T13:49:55Z",
    "Cause": "Malicious",
    "Category": [
        "Theft.Machinery"
    ],
    "Priority": "High",
    "Confidence": 0.85,
    "Description": "Unauthorized removal of a secure laptop from a restricted server room detected via badge access logs and motion sensors.",
    "Analyzer": {
        "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a21",
        "IP": "192.168.100.10",
        "Name": "Security Control Panel",
        "Hostname": "security-hub.acme.eu",
        "Model": "Honeywell Pro-Watch",
        "Category": [
            "ID.IAM"
        ],
        "Data": [
            "Contact",
            "Vibration"
        ],
        "Method": [
            "Threshold"
        ],
        "GeoLocation": "+41.9028,+12.4964,+50",
        "UnLocation": "IT ROM",
        "Location": "ACME-ROME-SERVER1"
    },
    "Sensor": [
        {
            "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a22",
            "IP": "192.168.100.10",
            "Name": "Security Control Panel",
            "CaptureZone": "[{\"type\":\"physical-area\",\"spec_version\":\"2.1\",\"id\": \"physical-area--abcd1234-5678-90ef-abcd-1234567890ef\",\"value\": \"Server Room A - Floor 2\"}]"
        }
    ],
    "Source": [
        {
            "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a23",
            "IP": "192.168.200.50",
            "Protocol": [
                "BadgeAccess"
            ]
        }
    ],
    "Target": [
        {
            "ID": "9a3e8dbe-b90f-4d55-b45e-823f0e509a24",
            "IP": "192.168.100.22",
            "Port": [],
            "Location": "Secure Server Room - Rack 5"
        }
    ]
}'
```

## HTTP

### Message Post gateway-sender

```bash
curl --location 'http://localhost:10080/topics/alerts' \
--header 'Content-Type: application/vnd.kafka.json.v2+json' \
--data '{
  "Version": "2.D.V04",
  "ID": "e5f9bbae-163e-42f9-a2f2-0daaf78fefb1",
  "Status": "Incident",
  "OrganisationName": "acme.eu",
  "CreateTime": "",
  "StartTime": "2021-01-18T23:33:04.52Z",
  "Cause": "Malicious",
  "Category": [
    "Recon.Scanning"
  ],
  "Priority": "Medium",
  "Confidence": 0.9,
  "Description": "An IDS event of type \"Unknown IP Protocol\" was observed",
  "Analyzer": {
    "IP": "172.10.5.31",
    "Name": "Core router",
    "Hostname": "core-router.example.com",
    "Type": "Cyber",
    "Model": "Cisco 4532",
    "Category": [
      "NIDS"
    ],
    "Data": [
      "Datagram"
    ],
    "Method": [
      "signature"
    ],
    "GeoLocation": "+48.75726,+2.299528,+65.1",
    "UnLocation": "GR ATH",
    "Location": "SBDX1"
  },
  "Sensor": [
    {
      "IP": "172.10.5.31",
      "Name": "Core router",
      "CaptureZone": "[{\"type\":\"ipv4-addr\",\"spec_version\":\"2.1\",\"id\": \"ipv4-addr--ff26c055-6336-5bc5-b98d-13d6226742dd\",\"value\": \"172.17.0.0\"},{\"type\":\"ipv4-net-mask\",\"spec_version\":\"2.1\",\"id\":\"ipv4-net-mask--ff26c055-6336-5bc5-b98d-13d6226742dd\",\"value\":\"255.255.0.0\"}]"
    }
  ],
  "Source": [
    {
      "IP": "77.222.222.115",
      "Protocol": [
        "Unknown"
      ]
    }
  ],
  "Target": [
    {
      "IP": "172.20.7.25",
      "Port": [
        645
      ],
      "Location": "Datacenter SBX1"
    }
  ]
}'
```

### Message Post gateway-receiver

```bash
curl --location 'http://localhost:10081/topics/alerts' \
--header 'Content-Type: application/vnd.kafka.json.v2+json' \
--data '{
  "Version": "2.D.V04",
  "ID": "e5f9bbae-163e-42f9-a2f2-0daaf78fefb1",
  "Status": "Incident",
  "OrganisationName": "acme.eu",
  "CreateTime": "",
  "StartTime": "2021-01-18T23:33:04.52Z",
  "Cause": "Malicious",
  "Category": [
    "Recon.Scanning"
  ],
  "Priority": "Medium",
  "Confidence": 0.9,
  "Description": "An IDS event of type \"Unknown IP Protocol\" was observed",
  "Analyzer": {
    "IP": "172.10.5.31",
    "Name": "Core router",
    "Hostname": "core-router.example.com",
    "Type": "Cyber",
    "Model": "Cisco 4532",
    "Category": [
      "NIDS"
    ],
    "Data": [
      "Datagram"
    ],
    "Method": [
      "signature"
    ],
    "GeoLocation": "+48.75726,+2.299528,+65.1",
    "UnLocation": "GR ATH",
    "Location": "SBDX1"
  },
  "Sensor": [
    {
      "IP": "172.10.5.31",
      "Name": "Core router",
      "CaptureZone": "[{\"type\":\"ipv4-addr\",\"spec_version\":\"2.1\",\"id\": \"ipv4-addr--ff26c055-6336-5bc5-b98d-13d6226742dd\",\"value\": \"172.17.0.0\"},{\"type\":\"ipv4-net-mask\",\"spec_version\":\"2.1\",\"id\":\"ipv4-net-mask--ff26c055-6336-5bc5-b98d-13d6226742dd\",\"value\":\"255.255.0.0\"}]"
    }
  ],
  "Source": [
    {
      "IP": "77.222.222.115",
      "Protocol": [
        "Unknown"
      ]
    }
  ],
  "Target": [
    {
      "IP": "172.20.7.25",
      "Port": [
        645
      ],
      "Location": "Datacenter SBX1"
    }
  ]
}'
```
