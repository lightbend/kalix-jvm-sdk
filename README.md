# akkasls-codegen #

![Test](https://github.com/lightbend/akkaserverless-codegen/workflows/Test/badge.svg)

akkasls-codegen is a JVM-based library that accepts protobuf-generated Java sources and
incrementally compiles them by invoking the Java compiler. The compiled artifacts are then
introspected to build a graph of Cloudstate event-sourced entities along with their commands,
events and serializable state types. This graph can then be fed into code generation for which
multiple libraries are also provided e.g. `akkasls-codegen-java` takes the graph along with a target source
directory and generates Java Akka serverless source code. Existing Akka serverless source code
can also be presented to code generation whereupon it will be diligently modified instead of
being overwritten.

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with
any pull requests, please state that the contribution is your original work and that you license
the work to the project under the project's open source license. Whether or not you state this
explicitly, by submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project's open source license and warrant that you have the
legal authority to do so.


