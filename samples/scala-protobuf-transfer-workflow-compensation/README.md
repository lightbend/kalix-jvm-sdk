# Implementing a Workflow with compensation in Scala

## Building and running unit tests

To compile and test the code from the command line, use

```shell
sbt test
```

## Running Locally

When running a Kalix service locally, we need to have its companion Kalix Runtime running alongside it.

To start your service locally, run:

```shell
sbt runAll
```

This command will start your Kalix service and a companion Kalix Runtime as configured in [docker-compose.yml](./docker-compose.yml) file.

For further details see [Running a service locally](https://docs.kalix.io/developing/running-service-locally.html) in the documentation.

### Exercising the transfer

With both the Kalix Runtime and your service running, any defined endpoints should be available at `http://localhost:9000`.

Create wallet `a` with an initial balance

```shell
grpcurl -plaintext -d '{"wallet_id": "a", "balance": 100}' localhost:9000 com.example.wallet.api.TransferWorkflowService/Create
```

Create wallet `b` with an initial balance

```shell
grpcurl -plaintext -d '{"wallet_id": "b", "balance": 100}' localhost:9000 com.example.wallet.api.TransferWorkflowService/Create
```

Get wallet `a` current balance

```shell
grpcurl -plaintext -d '{"wallet_id": "a"}' localhost:9000 com.example.wallet.api.TransferWorkflowService/GetWalletState
```

Get wallet `b` current balance

```shell
grpcurl -plaintext -d '{"wallet_id": "b"}' localhost:9000 com.example.wallet.api.TransferWorkflowService/GetWalletState
```

Start transfer from wallet `a` to wallet `b`

```shell
grpcurl -plaintext -d '{"transfer_id": "1", "from": "a", "to": "b", "amount": 10}' localhost:9000 com.example.transfer.api.TransferWorkflowService/Start
```

Get transfer state

```shell
grpcurl -plaintext -d '{"transfer_id": "1"}' localhost:9000 com.example.transfer.api.TransferWorkflowService/GetTransferState
```

## Deploying

To deploy your service, install the `kalix` CLI as documented in
[Setting up a local development environment](https://docs.kalix.io/setting-up/)
and configure a Docker Registry to upload your docker image to.

You will need to update the `dockerImage` property in the `pom.xml` and refer to
[Configuring registries](https://docs.kalix.io/projects/container-registries.html)
for more information on how to make your docker image available to Kalix.

Finally, you can use the [Kalix Console](https://console.kalix.io)
to create a project and then deploy your service into the project either by using `mvn deploy kalix:deploy` which
will conveniently package, publish your docker image, and deploy your service to Kalix, or by first packaging and
publishing the docker image through `mvn deploy` and then deploying the image
through the `kalix` CLI.
