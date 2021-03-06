# Mulperi

Mulperi is a service in the dependency engine of OpenReq infrastructure that primarily focuses on the contexts, which already contain a large number of existing and dependent requirements, such as large distributed open source projects or large systems engineering projects. For example, the Qt Company has about one hundred-thousand (100,000) issues in its Jira. The dependency engine focuses on the entire body of requirements as an interdepedent "requirements model".

This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463.

# Technical Description

Mulperi is a service that is used in the OpenReq infrastructure to generate appropriate knowledge representation for inference in [KeljuCaaS](https://github.com/OpenReqEU/keljucaas/) and manage the communication with KeljuCaaS.  For further details, see the swagger documentation https://api.openreq.eu/#/services/mulperi.



## The following technologies are used:
- Java
- Spring Boot
- Maven
- GSON


## Public APIs

The API is documented by using Swagger2: https://api.openreq.eu/#/services/mulperi


## How to Install

Run the compiled jar file, e.g., nohup java -jar Mulperi-1.10.jar.

Mulperi uses the port 9202 that needs to be open to in order that the endpoints and Swagger page can be accessed. Mulperi also connects to KeljuCaaS services of OpenReq in the port 9205.

For token-based authorization using headers, the token can be added as a run property --mulperi.authToken=“tokenhere”, or 
in the appropriate application.properties file. 
If no token is provided, no such header will be included in the requests.

## How to Use This Microservice

The swagger page describes all endpoints. There are currently two key functionalities supported by Mulperi, which both take as an argument a project and its requirements in OpenReq JSON format.

1) Construction of a graph (a requirements model) from individual requirements (endpoint
`sendModelToKeljuCaas`) for easy query of interdendencies (endpoint `findTransitiveClosureOfRequirement`).

2) Checking for a model consistency and, if necessary, providing diagnosis for an inconsisteny model (e.g., endpoint `uploadDataCheckForConsistencyAndDoDiagnosis`).

## Notes for Developers

None at the moment.

## Sources

None

# How to Contribute
See the OpenReq Contribution Guidelines [here](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md).

# License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/).
