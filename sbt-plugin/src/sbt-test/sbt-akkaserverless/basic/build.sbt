import com.akkaserverless.sbt.AkkaserverlessPlugin.autoImport.generateUnmanaged

scalaVersion := "2.13.6"

Compile / compile := {
  // Make sure 'generateUnmanaged' is executed on each compile, to generate scaffolding code for
  // newly-introduced concepts.
  // After initial generation they are to be maintained manually and will not be overwritten.
  (Compile / generateUnmanaged).value
  (Compile / compile).value
}