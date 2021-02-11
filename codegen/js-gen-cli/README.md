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

## Accommodating other code generation libraries

The recommended approach is to produce a CLI for each code generation required. For example,
this project provides a CLI for generating JavaScript and names it `akkasls-codegen-js`. 
An alternative project should be used to, say, generate a Rust-based one. This keeps the CLI 
options and commands to a minimum, while also reducing images to including only what is required 
for a given target.