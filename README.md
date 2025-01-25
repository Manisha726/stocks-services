# stocks-services
 

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Git
- Gradle

To run the service locally follow the below steps

1. Clone the git repo using :
```
https://github.com/Manisha726/stocks-services.git
```
2. Copy application-example.properties to application.properties and update the configurations
- spring.datasource.url  (postgres url link)
- spring.datasource.username
- spring.datasource.password
- alpha.vantage.api.key   (you can generate the key using below link)
     ```
     https://www.alphavantage.co/support/#api-key
     ```
- jwt.secret
- front.end.url  (give front-end url of stocks-web)
3. Build the project using :
  for windows
  ```
  ./gradlew.bat build
  ```
  for mac/linux
  ```
  ./gradlew build
  ```
4. Run the project using :
   for windows
  ```
  ./gradlew.bat bootRun
  ```
  for mac/linux
  ```
  ./gradlew bootRun
  ```
5. The application will run on: http://localhost:8080


### Assumption or Limitations
- Using stocks-web and stocks-services you can buy or sell shares(not real shares).
- As the free api key of alpha vantage provides to hit limited number of requests whenever the api key expires, static data will be shown from stocks-web and instead of buying the share with real price only for ($1 - constant value) you can buy.


### Desclaimer

- The stocks that you buy or sell using stocks-web and stocks-serives are not real.

