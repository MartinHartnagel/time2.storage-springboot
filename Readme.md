# Spring Boot implementation of the Time2Emphasize storage

The Time2Emphasize app on https://time2.emphasize.de provides no initial connection to store user data. This implementation with Java Spring Boot allows running a storage and connecting it with the Time2Emphasize app. An alternative [PHP implementation](https://github.com/MartinHartnagel/time2.storage-php/) is also available.

An example connection to add a running instance on http://localhost:8080 can be added by opening this link: 
https://time2.emphasize.de/?m=s&d=http%3A%2F%2Flocalhost%3A8080

Due to browser restrictions this connection (which is missing *https*) will only fully function in a Chrome browser. With an installed https certificate any browser can be used.

## Requirements

- Java Version 25

## Confige the database used

The following databases are supported:

- Sqlite (default, flat file only)
- Mysql
- Postgres

The datasource can be configured in src/main/resources/application.yaml or by overriding with parameters when executing. 

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


## Using the Time2Emphasize REST API

After connecting your storage in the Time2Emphasize Webapp, the endpoints will be made accessable under https://time2.emphasize.de/api in the Time2Emphasize REST API for testing and integration of git-hooks or other implementations.

## Features only available with a connected storage

- sharing of [notes](https://time2.emphasize.de/note)
