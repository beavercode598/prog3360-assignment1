# prog3360-assignment1
Microservices CI/CD pipeline – PROG3360
for self reference and easy pastes

dependencies for both services:
<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-h2console</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>



changed application.properties in both order_service and product_service

cd "C:\Users\Leon\OneDrive\Desktop\Conestoga Winter 2026\Prog3360\prog3360-assignment1\product-service"
.\mvnw.cmd clean spring-boot:run

ran the  commands bbelow in a new PowerShell terminal while the service is running

_____
Invoke-RestMethod http://localhost:8081/api/products | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/api/products" -Method POST -ContentType "application/json" -Body '{"name":"Milk","price":4.99,"quantity":10}'
Invoke-RestMethod http://localhost:8081/api/products/1
Invoke-RestMethod -Method DELETE http://localhost:8081/api/products/1


Invoke-RestMethod -Uri "http://localhost:8082/api/orders" -Method POST -ContentType "application/json" -Body '{"productId":1,"quantity":2}' | ConvertTo-Json
Invoke-RestMethod http://localhost:8082/api/orders | ConvertTo-Json
Invoke-RestMethod http://localhost:8082/api/orders/1 | ConvertTo-Json

should fail with insufficient quantity
Invoke-RestMethod -Uri "http://localhost:8082/api/orders" -Method POST -ContentType "application/json" -Body '{"productId":1,"quantity":999}'
______


git add .
git commit -m "Complete Product and Order Services with inter-service validation"
git push origin main

didnt have permission to open .vs so added .gitignore at  repo root folder 


git add .gitignore
git add README.md
git add product-service
git add order-service


added to properties 

management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true

which gives http://localhost:8081/actuator/health , http://localhost:8082/actuator/health

added Dockerfiles to order-service and product-service
added docker-compose.yml

ran Docker server
entered docker compose up --build

only one usage on port 8081 had to find it netstat -ano | findstr :8081
and task kill /PID /F

installed apache maven 3.9.12 locally to fix error with jar being run not having actuator causing order-service to be unhealthy.
