# Prerequisite
Java 21 or later<br>
Apache Maven 3.6 or higher<br>
Docker 20.10.14 or higher (client and daemon)<br>
cURL<br>
IDE / editor<br>

# Kalix SDK

This use case will be implemented using Kalix Java SDK, code first SDK. 
In a code first SDK, we will be using Java code (`Record`, `Class`) to represent domain and api data structures.

# Use case

![Cinema Booking Use case](images/cinema-show-usecase.png?raw=true)

Cinema booking system allows customers to book a cinema show seats and pay using the e-wallet.<br>
1. Cinema show manager creates a show with available number of seats
2. Customer creates an e-wallet with initial resources 
3. Customer searches for shows with available seats
4. Customer reserves the seats for the show
5. Payment for the reserved seats is done via e-wallet

# Design Cinema booking system

For designing the cinema booking system we are going to use Tactical [EventStorming](https://en.wikipedia.org/wiki/Event_storming) methodology.<br>

Legend:<br>
![Event Storming agenda](images/EventStorming-legend.png)

## Entities / Aggregate Roots
![Entities](images/EventStorming-AggregateRoots.png)

### Show

Show entity business data model:
```
public record Show (
   String id
   String title
   Map<Integer, Seat> seats
   Map<String, Integer> pendingReservations
   Map<String, FinishedReservation> finishedReservations
){}
public record Seat(
   int number, 
   SeatStatus status, 
   BigDecimal price
){}
public enum SeatStatus {
AVAILABLE, RESERVED, PAID
}
```
In addition to show `id` and `title` show has a list of `sets` where each seat has an index `number`, `status` (AVAILABLE, RESERVED, PAID) and `price`.<br>
`pendingReservations`, bookings that are not paid yet and `finishedReservations` that are paid.
### Wallet

Wallet entity business data model:
```
public record Wallet(String id, BigDecimal balance, Map<String, Expense> expenses) {}
public record Expense(String expenseId, BigDecimal amount) {}
```
In addition to wallet `id`,  `balance` represents the available amount, `expenses` represents all expenses done using this instance of the wallet (indexed by unique `expenseId`).
Each `expense` has a unique `expenseId` and amount that was expensed! 

## Commands & Domain Events
### Create show & search show

![Create Show Event](images/EventStorming-CreateShow.png)

`CreateShow` command initiates creation of the Show by emitting `ShowCreated` domain event.<br>

ShowCommand data model:
```
 record CreateShow(String showId, String title, int maxSeats){}
```

ShowCreated data model:
```
 record ShowCreated(String showId, Show.InitialShow initialShow){}
public record InitialShow(String id, String title, List<Seat> seats){}
```
Based on `ShowCreated` domain event, `Show by availability` read view is updated. <br>
`Show by availability` view is used to search for shows based on the availability.

View Data model:
```
record ShowsByAvailableSeatsViewRecord(String showId, String title, int availableSeats){}
```

Initial value of `availableSeats` is total number of seats for the show (`seats.size()`).<br> 
Command `Get shows by availability` is querying the `Show by availability` read view based on requested number of seats to book.

Get shows by availability command data model:
```
record GetShowsByAvailableSeatsCommand(Integer requestedSeatCount){}
```
Get shows by availability command response data model:
```
record ShowsByAvailableSeatsRecordList(List<ShowsByAvailableSeatsViewRecord> list){}
```
### Create Wallet

![Create Wallet Event](images/EventStorming-CreateWallet.png)

`CreateWallet` command initiates creation of the wallet with initial balance by emitting `WalletCreated` domain event.<br>

WalletCommand data model:
```
 record CreateWallet(String walletId, BigDecimal initialAmount){}
```

ShowCreated domain event data model:
```
record WalletCreated(String walletId, BigDecimal initialAmount) {}
public record InitialShow(String id, String title, List<Seat> seats)  {}
```

Based on the `maxSeats`, the list of `Seats` with sequential index `number` of individual seat and with initial `SeatStatus`: `AVAILABLE`

### Book & Pay the show flow
![Book show](images/EventStorming-BookFlow.png)

#### Show Reserve seat
After customer has chosen the show he wants to book, `Reserve Seat` Command initiates seat reservation. 
If requested seat number is available, `SeatReserved` domain event is emitted, reserving the specified seats.

`ReserveSeat` Command data model:
```
record ReserveSeat(String walletId, String reservationId, int seatNumber){}
```
Reserve seat requires `walletId`, id of the wallet instance that is going to be used to pay the seat reservation, 
`reservationId` that unique reservation id and a `seatNumber`, the seat index.
ReserveSeat

`SeatReserved` domain event data model:
```
 record SeatReserved(String showId, String walletId, String reservationId, int seatNumber, BigDecimal price, int availableSeatsCount){}
```
`price` is a price of a chosen seat that needs to be charged on wallet. <br>
`availableSeatsCount` represents the count of available seats after this reservation.

Like with `ShowCreated` domain event, `Show by availability` read view is also updated based on `SeatReserved` domain event ( using `availableSeatsCount`).<br>

### Charge wallet based on the seat reservation

Based on `SeatReserved` domain event, `ChargeWallet` command initiates wallet charge. `walletId` from `SeatReserved` is used to identify instance of the wallet that needs to be expensed.<br> 
`ChargeWallet` command data model:
```
 record ChargeWallet(BigDecimal amount, String expenseId){}
```
In case of successful charge, Wallet emits `WalletCharged` domain event. In case of any error, Wallet emits `WalletChargeRejected` domain event.

`WalletCharged` data model:
```
record WalletCharged(String walletId, BigDecimal amount, String expenseId) {}
```
`WalletChargeRejected` data model:
```
record WalletChargeRejected(String walletId, String expenseId){}
```

# Maven Kalix Project setup

## Create kickstart maven project

```
mvn archetype:generate \
  -DarchetypeGroupId=io.kalix \
  -DarchetypeArtifactId=kalix-spring-boot-archetype \
  -DarchetypeVersion=1.3.5
```
Define value for property 'groupId': `com.example`<br>
Define value for property 'artifactId': `java-spring-cinema-booking-choreography`<br>
Define value for property 'version' 1.0-SNAPSHOT: :<br>
Define value for property 'package' com.example: : `com.example`<br>

## Import generated project in your IDE/editor

## pom.xml setup
Reload the `pom.xml` after finished.
### Add dependencies
Add following to pom.xml:
```
<dependency>
  <groupId>io.vavr</groupId>
  <artifactId>vavr</artifactId>
  <version>0.10.4</version>
</dependency>
<dependency>
  <groupId>org.assertj</groupId>
  <artifactId>assertj-core</artifactId>
  <version>3.23.1</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.awaitility</groupId>
  <artifactId>awaitility</artifactId>
  <version>4.2.0</version>
</dependency>
```

`vavr` library is used to provide support for:
- immutable collections
- Either support 

`assertj` library is used as an alternative to standard `JUnit` assertion.<br>

`awaitility` library that offers convenient way to implement asynchronous tests. <br>

*Note:* These added libraries are not mandatory to develop Kalix services but are convenient to use in different situations that are explained later. 

## Develop Kalix Service

# Show Entity

## Setup

Create package `com.example.cinema`

## Define persistence (domain)`
1. Create package `com.example.cinema.model`
2. Implement Java Record `Show`
3. Implement sealed interface `ShowEvent`
   <i><b>Tip</b></i>: Check content in `step-1-show-entity` git branch

## Define API data structure and endpoints
1. Implement interface `CinemaApiModel` with `ShowCommand`,`Response` and `ShowResponse`
2. Implement class `ShowEntity`
   <i><b>Tip</b></i>: Check content in `step-1-show-entity` git branch


## Implement unit test
1. Create  `src/test/java` <br>
2. Create package `com.example.cinema`
3. Implement helper classes `DomainGenerators`, `ShowBuilder` and `ShowCommandGenerators`
4. Implement business logic state test: `ShowTest`
6. Implement Entity test: `ShowEntityTest`<br>
   <i><b>Tip</b></i>: Check content in `step-1-show-entity` git branch

## Run unit test
```
mvn test
```
## Run locally
Start the service and kalix runtime:

```
mvn kalix:runAll
```

## Test service locally
Create show:
```
curl -XPOST -d '{
  "title": "title",
  "maxSeats": 100
}' http://localhost:9000/cinema-show/1 -H "Content-Type: application/json"
```
Reserve a seat:
```
curl -XPATCH -d '{
  "walletId": "title",
  "reservationId": "res1",
  "seatNumber": 1
}' http://localhost:9000/cinema-show/1/reserve -H "Content-Type: application/json"
```
Confirm seat payment:
```
curl -XPATCH http://localhost:9000/cinema-show/1/confirm-payment/res1 -H "Content-Type: application/json"
```
Get:
```
curl -XGET http://localhost:9000/cinema-show/1 -H "Content-Type: application/json"
```

### Deploy
1. Install Kalix CLI
   https://docs.kalix.io/setting-up/index.html#_1_install_the_kalix_cli
2. Kalix CLI
    1. Register (FREE)
    ```
    kalix auth signup
    ```
   **Note**: Following command will open a browser where registration information can be filled in<br>
    2. Login
    ```
    kalix auth login
    ```
   **Note**: Following command will open a browser where authentication approval needs to be provided<br>

    3. Create a project
    ```
    kalix projects new cinema-booking --region=gcp-us-east1
    ```
   **Note**: `gcp-is-east1` is currently the only available region for deploying trial projects. For non-trial projects you can select Cloud Provider and regions of your choice<br>

    4. Authenticate local docker for pushing docker image to `Kalix Container Registry (KCR)`
    ```
    kalix auth container-registry configure
    ```
   **Note**: The command will output `Kalix Container Registry (KCR)` path that will be used to configure `dockerImage` in `pom.xml`<br>
    5. Extract Kalix user `username`
   ```
   kalix auth current-login
   ```
   **Note**: The command will output Kalix user details and column `USERNAME` will be used to configure `dockerImage` in `pom.xml`<br>
3. Configure `dockerImage` path in `pom.xml`
   Replace `my-docker-repo` in `dockerImage` in `pom.xml` with: <br>
   `Kalix Container Registry (KCR)` path + `/` + `USERNAME` + `/cinema-booking`<br>
   **Example** where `Kalix Container Registry (KCR)` path is `kcr.us-east-1.kalix.io` and `USERNAME` is `myuser`:<br>
```
<dockerImage>kcr.us-east-1.kalix.io/myuser/cinema-booking/${project.artifactId}</dockerImage>
```
4. Deploy service in Kalix project:
 ```
mvn deploy kalix:deploy
 ```
This command will:
- compile the code
- execute tests
- package into a docker image
- push the docker image to Kalix docker registry
- trigger service deployment by invoking Kalix CLI
5. Check deployment:
```
kalix service list
```
Result:
```
kalix service list                                                                         
NAME                                         AGE    REPLICAS   STATUS        IMAGE TAG                     
cinema-booking-java                          50s    0          Ready         1.0-SNAPSHOT                  
```
**Note**: When deploying service for the first time it can take up to 1 minute for internal provisioning

## Test service in production
Proxy connection to Kalix service via Kalix CLI
```
kalix service proxy cinema-booking-java --port 9000
```
Proxy Kalix CLI command will expose service proxy connection on `localhost:8080` <br>

Create show:
```
curl -XPOST -d '{
  "title": "title",
  "maxSeats": 100
}' http://localhost:8080/cinema-show/1 -H "Content-Type: application/json"
```
Reserve a seat:
```
curl -XPATCH -d '{
  "walletId": "title",
  "reservationId": "res1",
  "seatNumber": 1
}' http://localhost:8080/cinema-show/1/reserve -H "Content-Type: application/json"
```
Confirm seat payment:
```
curl -XPATCH http://localhost:8080/cinema-show/1/confirm-payment/res1 -H "Content-Type: application/json"
```
Get:
```
curl -XGET http://localhost:8080/cinema-show/1 -H "Content-Type: application/json"
```

# Shows by available seats View
## Define View data structures
1. In interface `CinemaApiModel` add `ShowsByAvailableSeatsViewRecord` and `ShowsByAvailableSeatsRecordList`
2. Implement class `ShowsByAvailableSeatsView`
   <i><b>Tip</b></i>: Check content in `step-2-shows-view` git branch

## Implement integration test
1. Delete `IntegrationTest` in `src/itjava.com.example`
2. Create package `cinema`
3. Add helper classes: `TestUtils`, `Calls` (with all Show related endpoints only)
4. Implement integration test `ShowsByAvailableSeatsViewIntegrationTest`

## Run integration test
```
mvn -Pit verify
```
## Test service locally
Create show:
```
curl -XPOST -d '{
  "title": "title",
  "maxSeats": 100
}' http://localhost:9000/cinema-show/1 -H "Content-Type: application/json"
```
Search view:
```
curl -XGET http://localhost:9000/cinema-shows/by-available-seats/1 -H "Content-Type: application/json"
```
# Wallet Entity
## Setup

Create package `com.example.wallet`

## Define persistence (domain)`
1. Create package `com.example.wallet.model`
2. Implement Java Record `Wallet`
3. Implement sealed interface `WalletEvent`
   <i><b>Tip</b></i>: Check content in `step-3-wallet-entity` git branch

## Define API data structure and endpoints
1. Implement interface `WalletApiModel` with `WalletCommand` and `WalletResponse`
2. Implement class `WalletEntity`
   <i><b>Tip</b></i>: Check content in `step-3-wallet-entity` git branch

## Implement unit test
2. Create package `com.example.wallet`
3. Implement helper classes `DomainGenerators`
4. Implement business logic state test: `WalletTest`
6. Implement Entity test: `WalletEntityTest`<br>
   <i><b>Tip</b></i>: Check content in `step-3-wallet-entity` git branch

## Run unit test
```
mvn test
```
## Run locally
Start the service and kalix runtime:

```
mvn kalix:runAll
```

## Test service locally
Create wallet with initial balance:
```
curl -XPOST http://localhost:9000/wallet/1/create/100 -H "Content-Type: application/json"
```
Charge:
```
curl -XPATCH -d '{
  "amount": 50,
  "expenseId": "exp1",
  "commandId": "exp1"
}' http://localhost:9000/wallet/1/charge -H "Content-Type: application/json"
```
Get:
```
curl -XGET http://localhost:9000/wallet/1 -H "Content-Type: application/json"
```

# Choreography Saga





