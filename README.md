# Choreography Saga Quickstart

This quickstart sample demonstrates how to implement a Choreography Saga in Kalix.

This project explores the usage of [Event Sourced Entities](https://docs.kalix.io/java/event-sourced-entities.html), [Value Entities](https://docs.kalix.io/java/value-entity.html), [Actions](https://docs.kalix.io/java/actions.html) and [Timers](https://docs.kalix.io/java/timers.html).  

Actions are used in two different contexts in this sample:

* To implement an [application controller](https://docs.kalix.io/java/actions.html#_actions_as_controllers).
* To [subscribe and react](https://docs.kalix.io/java/actions-publishing-subscribing.html#_subscribing_and_acting_upon) to events and state changes from the `UserEntity` and `UniqueEmailEntity`, respectively.


To understand more about these components, see [Developing services](https://docs.kalix.io/services/) and check the Kalix Java SDK [documentation](https://docs.kalix.io/java/index.html).



## Prerequisites

To use this quickstart sample, you will need:

* **Java:** Java 17 or higher
* **Maven:** [Maven 3.6 or higher](https://maven.apache.org/download.cgi)
* **Docker:** [Docker 20.10.14 or higher](https://docs.docker.com/engine/install)

## What is a Choreography Saga?

A **Choreography Saga** is a distributed transaction pattern that helps you manage transactions across multiple services.

In the context of event-driven applications, a **Choreography Saga** is implemented as a sequence of transactions,
each of which publishes an event or state change notification that triggers the next operation in the saga.
If an operation fails because it violates a business rule, then the saga can execute a series of compensating transactions
that undo the changes made by the previous operations.

In Kalix, in addition to events from Event Sourced Entities, you can also subscribe to state changes from Value Entities.
To subscribe to events or state changes, we can use Kalix Actions with the appropriate subscription annotations.

You can create a Choreography Saga to manage transactions across multiple entities in a single service, or across multiple services.
This example implements a choreography that manages transactions across two entities in the same service.

## Cross Entity Field Uniqueness

A common challenge in event-sourced applications is called the _Set-Based Consistency Validation_ problem. It arises when we need to ensure that a particular field is unique across all entities in the system. For example, a user may have a unique identifier (e.g. social security number) that can be used as a unique entity ID, but may also have an email address that needs to be unique across all users in the system.

In an event-sourced application, the events emitted by an entity are stored in a journal optimised to store the payload of the event, without any prior knowledge of the structure of the data. As such, it is not possible to add a unique constraint.

In this quickstart example, a **Choreography Saga** is introduced to handle this challenge. Along with the `UserEntity`, an additional entity is established to serve as a barrier. This entity, named the `UniqueEmailEntity`, is responsible for ensuring that each email address is associated with only one user. The unique ID of the `UniqueEmailEntity` corresponds to the email address itself. Consequently, it is ensured that only one instance of this entity exists for each email address.

When a request to create a new `UserEntity` is received, the application initially attempts to reserve the email address using the `UniqueEmailEntity`. If the email address is not already in use, the application proceeds to create the `UserEntity`. After the `UserEntity` is successfully created, the status of the `UniqueEmailEntity` is set to CONFIRMED. However, if the email address is already in use, the attempt to create the `UserEntity` will not succeed.

To achieve this behavior, two Kalix Actions are implemented. These Actions subscribe to and react to events and state changes from the `UserEntity` and `UniqueEmailEntity`, respectively. The Actions are responsible for converging the system to a consistent state. The components react autonomously to what is happening in the application, similar to performers in a choreographed dance. Hence, the name Choreography Saga.

### Successful Scenario

The sunny day scenario is illustrated in the following diagram:

![Successful Flow](flow-successful.png?raw=true)

All incoming requests are handled by the `ApplicationController` which is implemented using a Kalix Action.

1. Upon receiving a request to create a new User, the `ApplicationController` will first reserve the email.
2. It will then create the User.
3. The `UserEventsSubscriber` Action is listening to the User's event.
4. The `UniqueEmailEntity` is confirmed as soon as the subscriber 'sees' that a User has been created.
      
### Failure Scenario

As these are two independent transactions, it's important to consider potential failure scenarios. For instance, while a request to reserve the email address might be successful, the request to create the user could fail. In such a situation, there is a possibility of having an email address that is reserved but not linked to a user.

The failure scenario is illustrated in the following diagram:

![Failure Flow](flow-failure.png?raw=true)

1. Upon receiving a request to create a new User, the `ApplicationController` will first reserve the email.
2. Then it tries to create the User, but it fails. As such, the email will never be confirmed.
3. In the background, the `UniqueEmailSubscriber` Action is listening to state changes from `UniqueEmailEntity`.
4. When it detects that an email has been reserved, it schedules a timer to un-reserve it after a certain amount of time.
5. When the timer fires, the reservation is cancelled if the `UniqueEmailEntity` is still in RESERVED status.

> [!NOTE]
> Everything on the side of the `UniqueEmailSubscriber` is happening in the background and independent of the success or failure of the User creation.

### Full Successful Scenario

Now that the failure scenario has been covered, let's examine the complete picture in the successful scenario:

![Full Successful Flow](flow-full.png?raw=true)

It's important to note that `UniqueEmailSubscriber` and `UserEventsSubscriber` are two independent components. Once deployed, they operate in the background, performing their tasks whenever they receive an event or state change notification.

In the scenario where a user is successfully created, `UniqueEmailSubscriber` continues to respond to the `UniqueEmailEntity` reservation, scheduling a timer for un-reservation. However, as the user has been created `UserEventsSubscriber` updates the `UniqueEmailEntity` status to CONFIRMED. This triggers another state change notification to `UniqueEmailSubscriber`, which cancels the timer.

This quickstart example demonstrates how to implement a **Choreography Saga** in Kalix. It involves two entities influencing each other, and two actions listening to events and state changes, ensuring the entire application converges to a consistent state.

## Running and exercising this sample

To start your service locally, run:

```shell
mvn kalix:runAll -Demail.confirmation.timeout=10s
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

The `email.confirmation.timeout` setting is used to configure the timer to fire after 10 seconds. In other words, if 
the email is not confirmed within this time, it will be released. The default value for this setting is 2 hours (see the `src/resources/application.conf` file). For demo purposes, it's convenient to set it to a few seconds so we don't have to wait.

### create user identified by 001

```shell
curl localhost:9000/api/users/001 \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{ "name":"John Doe","country":"Belgium", "email":"doe@acme.com" }'
```

Check the logs of `UniqueEmailSubscriber` and `UserEventsSubscriber` to see how the saga is progressing.

### check status for email doe@acme.com

```shell
curl localhost:9000/api/emails/doe@acme.com
```
The status of the email will be RESERVED or CONFIRMED, depending on whether the saga has been completed or not. 

### create user identified by 002

```shell
curl localhost:9000/api/users/002 \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{ "name":"Anne Doe","country":"Belgium", "email":"doe@acme.com" }'
```
A second user with the same email address will fail.

### try to create an invalid user

```shell
curl localhost:9000/api/users/003 \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{ "country":"Belgium", "email":"invalid@acme.com" }'
```

Note that the 'name' is not stored. This will result in the email address `invalid@acme.com` being reserved but not confirmed.

Check the logs of `UniqueEmailSubscriber` to see how the saga is progressing.

### check status for email invalid@acme.com

```shell
curl localhost:9000/api/emails/invalid@acme.com
```

The status of the email will be RESERVED or NOT_USED, depending on whether the timer to un-reserve it has fired or not.

### Bonus: change an email address

Change the email address of user 001 to `john.doe@acme.com`. Inspect the code to understand how it re-uses the existing saga.

```shell
curl localhost:9000/api/users/001/change-email \
  --header "Content-Type: application/json" \
  -XPUT \
  --data '{ "newEmail": "john.doe@acme.com" }'
```

Check the logs of `UniqueEmailSubscriber` and `UserEventsSubscriber` to see how the saga is progressing.

### check status for email doe@acme.com

```shell
curl localhost:9000/api/emails/doe@acme.com
```

The status of the email will be CONFIRMED or NOT_USED, depending on whether the saga has been completed or not.

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Install Kalix](https://docs.kalix.io/kalix/install-kalix.html)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

* Finally, use the `kalix` CLI to generate a project.
* Deploy your service into the project using `mvn deploy kalix:deploy`. This command conveniently packages, publishes your Docker image, and deploys your service to Kalix.
