# Publishing

The samples are published as Docker images to `grc.io/kalix-public`.

To publish the images, ensure that you have the right permissions and configure `gcloud` to connect

```shell
# needs to be done if not logged in
gcloud auth login 

# needs to be done once
gcloud auth configure-docker 
```

```shell
mvn package docker:push
```
