# Registering Car metrics and selling data to Suppliers

### Business logic
A car emits data as an IoT device that we retrieve and then expose partially to suppliers. We provide the services for the cars to register its new data. Right now only the battery level is handled (TODO add location and speed). 
When new metrics are pushed through the API `car.metrics.api.CarMetricService/RecordChargeLevel` this data is not only recorded as an event in an EventSourcedEntity it is also forwarded to a Value Entity when the battery is lower than 25%. We represented this Value Entity as the data we sell and provide, through the API, to a supplier of batteries such only when the car is in LowBattery the supplier gains access to its data and it can act upon.   

### I/O data

#### Writing Battery
We create/update the car state by storing its Battery Level through the service `car.metrics.api.CarMetricsService/RecordChargeLevel`:

- gRPC call

    grpcurl --plaintext -d '{"car_id": "car1", "metrics": { "remaining_watts": 51, "watts_capacity": 100}}' localhost:9000 car.metrics.api.CarMetricsService/RecordChargeLevel
    
    grpcurl --plaintext -d '{"car_id": "car2", "metrics": { "remaining_watts": 22, "watts_capacity": 222}}' localhost:9000 car.metrics.api.CarMetricsService/RecordChargeLevel

    grpcurl --plaintext -d '{"car_id": "car3", "metrics": { "remaining_watts": 33, "watts_capacity": 333}}' localhost:9000 car.metrics.api.CarMetricsService/RecordChargeLevel
   
- HTTP call

    curl -d '{"remaining_watts": 33, "watts_capacity": 333}' -H "Content-Type: application/json"  http://localhost:9000/car/charge/car3 

More info about the equivalence between gRPC and HTTP in [googleapis - http.proto](https://github.com/googleapis/googleapis/blob/master/google/api/http.proto) 

#### Reading Battery
##### By car
All the data above is accessible, by car, trough the service `car.metrics.api.CarMetricsService/GetCarMetrics` 

- gRPC call

        grpcurl --plaintext -d '{"car_id": "car1"}' localhost:9000 car.metrics.api.CarMetricsService/GetCarMetrics

- HTTP call

        curl -X GET http://localhost:9000/car/car1
    
##### All cars

All the data above is accessible, all cars at once, through the service `car.metrics.view.CarMetricsViewService.GetCarsMetrics`
 - gRPC call

        grpcurl --plaintext localhost:9000 car.metrics.view.CarMetricsViewService.GetCarsMetrics
        
- HTTP call

        curl -X GET http://localhost:9000/metrics
        
#### Checking if the car has Low Battery 
##### By car
The supplier can access to the data for only those cars that the battery is low (less or equal to 25%) through the service `car.supplier.api.ChargeSupplierService/GetBatteryState` (TODO add timestamp with location and a view to get them all and not one by one)

- gRPC call
        
        grpcurl --plaintext -d '{"car_id":"car2"}' localhost:9000 car.supplier.api.ChargeSupplierService/GetBatteryState

- HTTP call
        
        curl -X GET http://localhost:9000/alert/car2
        

#### Up and Running        
To start the server:

1. Start the local google Pub/Sub emulator

        gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085

2. Start the Serverless proxy core from akkaserverless-framework source code in the base folder

        sbt -Dakkaserverless.proxy.eventing.support=google-pubsub proxy-core/run
        
3. Start the app

        mvn compile exec:java



#### Integration testing 

Run the integration tests
```
mvn verify -Pit
