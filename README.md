# Stock Market Data Application

This project was created for fun. Essentially populating an H2 database with Stock Market Data and then able to be retrieved via REST protocol. (Potentially more to come.)

### Prerequisites

Requirements for the software and other tools to build, test and push
- [Java 21](https://www.oracle.com/ca-en/java/technologies/downloads/#java21) Installed and working
- [Maven](https://maven.apache.org/download.cgi) For package management and building
  
Optional
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) Installed and working (if wanting to use Docker deployments locally, not required)

## Getting Started
### Installation

Begin by retrieving the codebase from the [GitHub Repository](https://github.com/JackBainbridge/stock-market-data).

    git clone https://github.com/JackBainbridge/stock-market-data.git

Once cloned into local working directory, we can build the application.

    mvn clean install

### Running Locally (without Docker)
Open IntelliJ IDE and first, build the project. (File -> Build -> Build Project) or (Ctrl + F9).

Once the project has built successfully, it is able to be executed. (File -> Run -> DataApplication) or (Alt + Shift + F10)

Once running data is able to be retrieved via the in memory H2 database by using the H2 console. (Potentially add other user with lower access for connection)

    http://localhost:8080/h2-console

![IntelliJ-H2-Database-Connection-Test](images/IntelliJ-H2-Database-Connection-Test.png)


### Running Locally (with Docker)
After the project is built, it is now able to be executed. 
Simply deploy the container to Docker Desktop via docker-deploy-local.sh shell script.

    ./docker-deploy-local.sh

![Docker-Desktop-Local-Deployment](images/Docker-Desktop-Local-Deployment.png)

This will spin up a container named 'stock-market-container' in Docker Desktop. From there once again, you are able to view the H2 database by using the H2 Console in the browser 

Ensure JDBC URL = jdbc:h2:mem:testdb

    http://localhost:8082/h2-console

![Docker-H2-Database-Connection-Test](images/Docker-H2-Database-Connection-Test.png)

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
