# akkasls-codegen-js-cli

This is a CLI suitable for invoking JavaScript code generation from JavaScript tooling.

## Building

We use GraalVM's native-image command to build a native image via the `sbt-native-image` plugin. To build:

```
sbt 'akkasls-codegen-js-cli/nativeImage'
```

The above command will automatically download the native-image tool by detecting the operating system
and JDK. Once done, the command will yield the location of the image built, which will be in the 
sub-project's target directory.

## Configuration

On occasion, particularly if you run into errors when running the CLI, you may need to have GraalVM
help you with configuring the image build. This can be done using the following syntax from the sbt
prompt:

> The first time you run this, it will warn you have not being able to merge configuration. Ignore
> this warning as it cannot merge for the first time. Run the command a few times with different
> options to provide greater coverage.

```
akkasls-codegen-js-cli/nativeImageRunAgent " --descriptor-set-output-dir=/Users/myuser/Projects/lightbend/akkasls-codegen/core/src/test/resources/test-files/descriptor-sets --descriptor-set-file=hello-1.0-SNAPSHOT.protobin"
```

This will run the CLI via the GraalVM JVM and record various configuration. The output of this configuration can be found
in the target folder and then copied directly into the src/main/resources folder of the CLI project.

## Running on macOS from a deployed artifact

On macOS, binaries generally downloaded from the internet are untrusted. To trust and run the codegen CLI:

```
xattr -d com.apple.quarantine <PATH-TO-IMAGE>/akkasls-codegen-js-x86_64-apple-darwin
```

## Accommodating other code generation libraries

The recommended approach is to produce a CLI for each code generation required. For example,
this project provides a CLI for generating JavaScript and names it `akkasls-codegen-js`. 
An alternative project should be used to, say, generate a Rust-based one. This keeps the CLI 
options and commands to a minimum, while also reducing images to including only what is required 
for a given target.