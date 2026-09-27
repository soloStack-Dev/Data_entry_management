# Spring Boot Configuration Specification

## 1. Dependencies

Spring Initializr dependencies:

``` text
Spring Web
Thymeleaf
Spring Data JPA
MySQL Driver
Validation
Spring Boot Actuator
Spring Boot DevTools
```

DevTools should be treated as a development dependency and not relied on
for production behavior.

## 2. Application Properties

Development example:

``` properties
spring.application.name=data-entry-platform

spring.datasource.url=jdbc:mysql://localhost:3306/data_entry_db
spring.datasource.username=${DB_USERNAME:datauser}
spring.datasource.password=${DB_PASSWORD:datapassword}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

spring.thymeleaf.cache=false

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when_authorized
```

## 3. Production Configuration

Use environment variables:

``` text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Example:

``` properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Do not hard-code production credentials in Git.

## 4. Actuator

Expose only the endpoints actually needed.

Recommended:

``` text
/actuator/health
/actuator/info
```

Health endpoint can be used to check whether:

-   Spring Boot is running
-   Database connectivity is healthy

## 5. HTTP Endpoints

Suggested routes:

``` text
GET  /
GET  /data-entry
POST /data-entry
GET  /data-entry/edit/{id}
POST /data-entry/edit/{id}

GET  /collection
GET  /collection/search
DELETE /collection/{id}
```

Use routes consistently and return Thymeleaf pages/fragments depending
on whether the request is a full-page or HTMX request.

## 6. Recommended Development Order

### Phase 1

Create Spring Boot project.

### Phase 2

Create:

``` text
DataEntry entity
DataEntryRepository
DataEntryService
DataEntryController
```

### Phase 3

Connect MySQL Docker.

### Phase 4

Build Data Entry page.

### Phase 5

Build Collection page.

### Phase 6

Add search with HTMX.

### Phase 7

Add edit/delete.

### Phase 8

Add validation.

### Phase 9

Add dashboard statistics.

### Phase 10

Dockerize Spring Boot.

### Phase 11

Deploy to Oracle Cloud.

## 7. Final Architecture

``` text
                 Browser
                    │
          HTML + Bootstrap + HTMX
                    │
                    ▼
             Spring Boot
                    │
       ┌────────────┼────────────┐
       │            │            │
   Controller     Service     Validation
       │            │
       └────────────┤
                    ▼
              Spring Data JPA
                    │
                    ▼
             MySQL Container
                    │
              Persistent Volume
```
