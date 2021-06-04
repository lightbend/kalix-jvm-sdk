
# Registering Car metrics and selling data to Suppliers

### Business logic
A car emits data as an IoT device that we retrieve and then expose partially to suppliers. We provide the services for the cars to register their new data. Right now only the battery level is handled (TODO add location and speed). 
When new metrics are pushed through the API `car.metrics.api.CarMetricService/RecordChargeLevel`, this data is not only recorded as an event in an EventSourcedEntity, it is also forwarded to a Value Entity when the battery charge level is lower than 25%. We represented this Value Entity as the data we sell and provide, through an API, to suppliers of batteries. The supplier only gains access to data when the car battery is low.   

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
The supplier can access the data for only those cars that the battery is low (less or equal to 25%) through the service `car.supplier.api.ChargeSupplierService/GetBatteryState` (TODO add timestamp with location and a view to get them all and not one by one)

- gRPC call
        
        grpcurl --plaintext -d '{"car_id":"car3"}' localhost:9000 car.supplier.api.ChargeSupplierService/GetBatteryState

- HTTP call
        
        curl -X GET http://localhost:9000/alert/car3
        

#### Up and Running        

1. Start the Akka Serverless proxy.

To run your application locally, you must run the Akka Serverless proxy. All HTTP or gRPC requests you issue are first directed to this proxy then to your application and back to you through the proxy, again. As any HTTP proxy. 

The included `docker-compose` file contains the configuration required to run the proxy for a locally running application.


```shell
docker-compose up
```

> On Linux this requires Docker 20.10 or later (https://github.com/moby/moby/pull/40007),
> or for a `USER_FUNCTION_HOST` environment variable to be set manually.
```shell
docker-compose -f docker-compose.yml -f docker-compose.linux.yml up
```

2. Start the local google Pub/Sub emulator that the Akka Serverless proxy will connect to.

        gcloud beta emulators pubsub start --project=test --host-port=0.0.0.0:8085

3. Start the app
To start the application locally, the `exec-maven-plugin` is used. Use the following command:

```shell
mvn compile exec:java
```

With both the proxy and your application running, any defined endpoints should be available at `http://localhost:9000`. In addition to the defined gRPC interface, each method has a corresponding HTTP endpoint. Unless configured otherwise (see [Transcoding HTTP](https://docs.lbcs.dev/js-services/proto.html#_transcoding_http)), this endpoint accepts POST requests at the path `/[package].[entity name]/[method]`. For example, using `curl`:
        

#### Integration testing 

Run the integration tests
```
mvn verify -Pit
```
