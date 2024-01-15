# fetch_heartbeat_checker
This project will periodically hit endpoints which are configured at src/main/resources/endpoints.yml and builds a aggregate of availability percentages by domain name. Currently, only 2 methods GET and POST were implemented as per the requirement.

### Run locally
1. mvn clean package
2. java -jar target/fetch_heartbeat_checker-1.0-SNAPSHOT-jar-with-dependencies.jar