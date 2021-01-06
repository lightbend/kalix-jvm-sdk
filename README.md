# akkasls-maven-plugin

This plugin is designed for developers of Maven and Java projects that wish to target the
[Akka Serverless](https://www.lightbend.com/akka-serverless) environment.

The plugin provides two functions:

* generate templated source code from entity, command, event and state information declared by .`proto` files; and
* the ability to deploy directly to Akka Serverless without requiring the `akkasls` CLI.

By including this plugin, no tooling other than Maven and Java are required to support a full-featured development lifecycle.
