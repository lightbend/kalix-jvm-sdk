FROM node:16.1-buster

WORKDIR /home
ADD ./akkaserverless-npm-js akkaserverless-npm-js
ADD ./akkasls-codegen-js akkasls-codegen-js
ADD ./scripts scripts

# Configure and install tooling
RUN ./scripts/setup.sh

CMD ./scripts/entrypoint.sh
