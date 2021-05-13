FROM maven:3.8.1-adoptopenjdk-11

WORKDIR /home
ADD ./akkaserverless-maven-java akkaserverless-maven-java

# Resolve dependencies before loading the latest Jar for better snapshotting
RUN cd akkaserverless-maven-java && mvn dependency:resolve

ADD ./akkasls-codegen-java_2.13-1.0-SNAPSHOT.jar akkasls-codegen-java_2.13-1.0-SNAPSHOT.jar
ADD ./scripts scripts

# Configure and install tooling
RUN ./scripts/setup.sh

CMD ./scripts/entrypoint.sh
