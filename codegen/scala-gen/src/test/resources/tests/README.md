# Round-trip tests for code generation

These are the test files for the `ExampleSuite`.

Each directory contains one test case. Directories can be nested, a test directory is recognized by finding a `proto`
directory inside.


The `proto` below each test directory contains the input protobuf files and the `generated-` directories contain the
expected output files.

To generate the files for the first time, you must pass a -D property indicating the newly added test dir, eg:

```shell 
sbt -Dexample.suite.scala.regenerate=my-new-test-suite
```

When changing the codegen itself, you can use the ExampleSuite to force the regeneration of files.
After regenerating the files you can inspect their contents and decide if the output is as expected.

To regenerate the files for a specific group of tests, you can pass the name of a parent folder, eg:

```shell 
sbt -Dexample.suite.scala.regenerate=action-service
```

This will regenerate the files for all suites under `action-service`.

Finally, you can regenerate all files for all suites by passing.

```shell 
sbt -Dexample.suite.scala.regenerate=all
```

# Compiling

The ExampleSuite only generates files, but doesn't compile them.
To compile the files, you should start sbt with:

```shell
sbt -Dexample.suite.scala.enabled 
```

When the example suites are enabled at sbt level, you will get a few more sbt projects.

```shell
[info]   * kalix-jvm-sdk
...
# extra project 
[info]     codegenScalaCompilationExampleSuite
... 
# each directory under codegen/scala-gen/src/test/resources/tests containing proto files will show up here as an sbt project
[info]     test-scala-action-service-named-new-style
[info]     test-scala-action-service-simple-new-style
[info]     test-scala-action-service-simple-old-style
[info]     test-scala-action-service-with-action-in-name
[info]     test-scala-event-sourced-entity-absolute-packages
[info]     test-scala-event-sourced-entity-domain-in-service-package
[info]     test-scala-event-sourced-entity-named-new-style
[info]     test-scala-event-sourced-entity-state-events-in-different-package
[info]     test-scala-event-sourced-entity-unnamed-new-style
[info]     test-scala-replicated-entity-multimap-absolute-packages
[info]     test-scala-replicated-entity-multimap-domain-in-service-package
[info]     test-scala-replicated-entity-multimap-key-value-in-different-pacakge
[info]     test-scala-replicated-entity-multimap-named-new-style
[info]     test-scala-replicated-entity-multimap-scalar-named-new-style
[info]     test-scala-replicated-entity-multimap-unnamed-new-style
[info]     test-scala-value-entity-absolute-packages
[info]     test-scala-value-entity-domain-in-service-package
[info]     test-scala-value-entity-named-new-style
[info]     test-scala-value-entity-state-events-in-different-package
[info]     test-scala-value-entity-unnamed-new-style
[info]     test-scala-view-service-named-new-style
[info]     test-scala-view-service-unnamed-new-style
[info]     test-scala-view-service-view-in-name-new-style
```

You can compile then by calling: `codegenScalaCompilationExampleSuite/compile` or individually `test-scala-action-service-named-new-style/compile`

# Generate and compile cycle

When working on a specific part of the codegen, a possible workflow could be:


```shell
# when working on action codegen, for example
sbt -Dexample.suite.scala.regenerate=action-service -Dexample.suite.scala.enabled 

# code, regenerate and compile suites
codegenScala/testOnly *ExampleSuite;test-scala-action-service-named-new-style/compile
```