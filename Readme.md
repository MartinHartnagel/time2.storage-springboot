# Spring Boot implementation of the Time2Emphasize storage

The Time2Emphasize app on https://time2.emphasize.de provides no initial connection to store user data. This implementation with Java Spring Boot allows running a storage and connect it with the Time2Emphasize app.

An example connection to add a running instance on http://localhost:8080 can be added by opening this link: 
https://time2.emphasize.de/?m=s&d=http%3A%2F%2Flocalhost%3A8080

Due to browser restrictions this connection (which is missing *https*) will only fully function in a Chrome browser. 

## Requirements

- Java 25

## Confige the database used

The following databases are supported:

- Sqlite (default, flat file only)
- Mysql
- Postgres

The datasource can be configured in src/main/resources/application.yaml or overridden with parameters when executing. 

## Run

```
mvn spring-boot:run
```


## Build an executable jar

```
mvn package spring-boot:repackage
```

then the jar can be executed with
```
java -jar target/time2.storage-0.0.1-SNAPSHOT.jar
```

