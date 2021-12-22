#!/bin/sh

set +e 

# this script allow us to take the proto definitions from the ExampleSuite tests,
# drop them on a new mvn project and compile it. 
# if run without any argument, it will loop through all the projects under
# /codegen/java-gen/src/test/resources/tests/*/*/
#
# if a specific test is passed, it runs for that single test
# eg: ./bin/compile-example-suite-java.sh codegen/java-gen/src/test/resources/tests/action-service/with-action-in-name/proto


# use latest or take whatever is found in env var
# in CI the SDK_VERSION is set for the current build, that's the one we want to use
VERSION=LATEST
if [ $SDK_VERSION ]; then
  VERSION=$SDK_VERSION
fi 

echo "Using SDK Version '$VERSION'"

TARGET_DIR=target/example-suite-tests-java

mkdir -p $TARGET_DIR
(
  cd $TARGET_DIR
  if [ -d  example-suite-tpl ]; then 
    rm -rf example-suite-tpl
  fi 

  # create the base project. This project will be copied each time
  mvn -B archetype:generate -DgroupId=org.example \
    -DartifactId=example-suite-tpl \
    -DarchetypeGroupId=com.akkaserverless \
    -DarchetypeArtifactId=akkaserverless-maven-archetype \
    -DarchetypeVersion=$VERSION
)


BASE_DIR=`pwd`/codegen/java-gen/src/test/resources/tests
PROTO_DIR=$BASE_DIR/*/*/proto
# for local dev, we may want to pass one single test proto for debuging
# in CI we run for all tests
if [ $1 ] ; then
  PROTO_DIR=$1
fi

ALL_OK=true

for i in $PROTO_DIR
do
  TEST_DIR_TMP=${i##$BASE_DIR\/} # remove BASE_DIR from the front
  TEST_DIR=$TARGET_DIR/${TEST_DIR_TMP%%\/proto} # remove proto at the end
  
  if [ -d $TEST_DIR ]; then 
    rm -rf $TEST_DIR
  fi 
  # each time, we create a new test project with the template copied into it
  # on failures we can navigate to it and inspect the files
  mkdir -p $TEST_DIR/src/main/
  echo "Copying proto files from [$i] to test project [$TEST_DIR]"
  cp -LR $i $TEST_DIR/src/main/
  
  echo "Copying pom.xml from template to [$TEST_DIR]"
  cp $TARGET_DIR/example-suite-tpl/pom.xml $TEST_DIR

  # for debugging
  ls -l $TEST_DIR/
  ls -l $TEST_DIR/src/main/proto

  echo "Compiling test project in [$TEST_DIR]"
  cd $TEST_DIR  
  MSG=""
  mvn -Pit test-compile
  if [ $? -eq 0 ]; then    
    MSG="Compilation passed for [$TEST_DIR]"
  else
    MSG="Compilation failed for [$TEST_DIR]"
    ALL_OK=false
  fi

  cd ../../../../
  echo 
  echo "-----------------------------------------------------------------"
  echo $MSG
  echo "-----------------------------------------------------------------"
  echo 
done 

$ALL_OK