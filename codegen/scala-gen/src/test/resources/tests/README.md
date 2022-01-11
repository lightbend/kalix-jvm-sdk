# Round-trip tests for code generation

These are the test files for the `ExampleSuite`.

Each directory contains one test case. Directories can be nested, a test directory is recognized by finding a `proto`
directory inside.

The `proto` below each test directory contains the input protobuf files and the `generated-` directories contain the
expected output files. You can create a file named `regenerate` into the test folder to have the test succeed and
create the test files for the given protobuf definitions. The `regenerate` file will be automatically deleted afterwards.

You can also change the flag `regenerateAll` in `ExampleSuite` to have the full set of example files regenerated after
a change. In that case, the test will always succeed and you are required to validate the diff to the previous version
of files.