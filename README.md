# Stock Market Data Application

This project was created for fun. Essentially populating an H2 database with Stock Market Data and then able to be retrieved via REST protocol. (Potentially more to come.)

### Prerequisites

Requirements for the software and other tools to build, test and push
- [Java 21](https://www.oracle.com/ca-en/java/technologies/downloads/#java21) Installed and working
- [Maven](https://maven.apache.org/download.cgi) For package management and building
  
Optional
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) Installed and working (if wanting to use Docker deployments locally, not required)

## Getting Started
### Installing

Begin by retrieving the codebase from the [GitHub Repository](https://github.com/JackBainbridge/stock-market-data).

    git clone https://github.com/JackBainbridge/stock-market-data.git

Once cloned into local working directory, we can build the application.

    mvn clean install

After the project is built, it is now able to be executed. 
Simply start the application via IntelliJ, or optionally deploy the container to Docker Desktop.

    ./docker-deploy-local.sh

Once running data is able to be retrieved via the in memory H2 database by using the H2 console. (Potentially add other user with lower access for connection)

    http://localhost:8080/h2-console

## Running the tests

Explain how to run the automated tests for this system

### Sample Tests

- TBD

    Give an example

## Deployment

- TBD

## Built With

- [Creative Commons](https://creativecommons.org/) - Used to choose
  the license

## Versioning
- TBD

## Authors
- **Jack Bainbridge** - *Created Application* -
