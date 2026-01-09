# Camunda 7 Demo - Real estate financing

## Voraussetzungen

- Java 17+
- Maven 3.9+

## Start

```bash
mvn -U clean spring-boot:run
```

## Usage

1. Open the UI at http://localhost:8080/credit.

## Additional tools

- Camunda Webapps: http://localhost:8080/camunda/app/welcome/default/#!/login
  Login: `camunda` / `admin`
- H2 console: http://localhost:8080/h2-console  
  JDBC URL: `jdbc:h2:mem:camunda`
