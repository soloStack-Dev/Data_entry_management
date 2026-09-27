# MySQL Database and Docker Specification

## 1. Database

Database engine:

``` text
MySQL 8.x
```

Database name:

``` text
data_entry_db
```

Main table:

``` text
data_entries
```

## 2. Docker Compose

Create:

``` text
docker-compose.yml
```

Example:

``` yaml
services:
  mysql:
    image: mysql:8.4
    container_name: data-entry-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: data_entry_db
      MYSQL_USER: datauser
      MYSQL_PASSWORD: datapassword
      MYSQL_ROOT_PASSWORD: rootpassword
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

For production, do not commit real passwords to Git.

Use environment variables or a secrets mechanism.

## 3. Database Schema

Database:

``` sql
CREATE DATABASE IF NOT EXISTS data_entry_db;
```

## 4. Main Table

Recommended schema:

``` sql
CREATE TABLE data_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    timing TIME NOT NULL,
    entry_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    from_user VARCHAR(100) NOT NULL,
    to_user VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_product_name (product_name),
    INDEX idx_entry_date (entry_date),
    INDEX idx_type (type)
);
```

## 5. Column Meaning

  Column         Purpose
  -------------- -----------------------
  id             Unique record ID
  product_name   Product name
  description    Entry description
  timing         Entry time
  entry_date     Entry date
  type           Entry category/type
  from_user      Source user
  to_user        Destination user
  created_at     Creation timestamp
  updated_at     Last update timestamp

## 6. Spring Entity

Entity name:

``` text
DataEntry
```

Suggested mapping:

``` text
Long id
String productName
String description
LocalTime timing
LocalDate entryDate
String type
String fromUser
String toUser
LocalDateTime createdAt
LocalDateTime updatedAt
```

Use JPA annotations.

## 7. JPA Configuration

For development:

``` properties
spring.datasource.url=jdbc:mysql://localhost:3306/data_entry_db
spring.datasource.username=datauser
spring.datasource.password=datapassword

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

For production, prefer controlled database migrations such as Flyway
rather than relying on automatic schema updates.

## 8. Docker Networking

If Spring Boot runs directly on the host and MySQL runs in Docker:

``` text
jdbc:mysql://localhost:3306/data_entry_db
```

If both Spring Boot and MySQL run as Docker containers in the same
Compose network:

``` text
jdbc:mysql://mysql:3306/data_entry_db
```

Do not use `localhost` from the Spring Boot container to reach the MySQL
container.

## 9. Persistent Storage

Always use a Docker volume:

``` yaml
volumes:
  - mysql_data:/var/lib/mysql
```

This prevents database data from disappearing when the MySQL container
is recreated.

## 10. Production Considerations

For Oracle Cloud deployment:

``` text
Oracle Cloud VM
    ↓
Docker
    ├── Spring Boot container
    └── MySQL container
```

Do not expose MySQL port `3306` publicly unless there is a specific
requirement.

Prefer:

``` text
Internet
   ↓
Nginx / Reverse Proxy
   ↓
Spring Boot
   ↓
Private Docker network
   ↓
MySQL
```

Only expose the application HTTP/HTTPS ports publicly.

## 11. Backup

Create regular database backups.

Example concept:

``` bash
docker exec data-entry-mysql \
  mysqldump -u datauser -p data_entry_db > backup.sql
```

Do not store production database backups only on the same VM.
