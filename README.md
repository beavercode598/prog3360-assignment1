# prog3360-assignment1
Microservices CI/CD pipeline – PROG3360


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