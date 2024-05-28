#!/usr/bin/env bash

# this script is meant to be used after a new SDK version is out
# to facilitate the update of all the places where we usually depend on the latest version

# provide the new sdk version you want the project to be updated to
if [[ -z "$SDK_VERSION" ]]; then
    echo "Must provide SDK_VERSION in environment" 1>&2
    exit 1
fi

updateJavaSamples() {
  echo ">>> Updating pom versions to $SDK_VERSION"
  PROJS=$(find $1 -type f -name "pom.xml")
  for i in ${PROJS[@]}
  do
    echo "Updating pom for: $i"
    # we only want to update the first occurrence of <version>, the one belonging the parent-pom
    awk '/<version>[^<]*<\/version>/ && !subyet {sub("<version>[^<]*<\/version>", "<version>"ENVIRON["SDK_VERSION"]"</version>"); subyet=1} 1' $i > temp && mv temp $i
  done
}

updateScalaSamples() {
  echo ">>> Updating sbt plugins to $SDK_VERSION"
  PROJS=$(find $1 -type f -name "*plugins.sbt")
  for i in ${PROJS[@]}
  do
    echo "Updating plugins sbt for: $i"
    sed -i.bak "s/System.getProperty(\"kalix-sdk.version\", \".*\"))/System.getProperty(\"kalix-sdk.version\", \"$SDK_VERSION\"))/" $i
    rm $i.bak
  done
}

updateMavenPlugin() {
  echo ">>> Updating maven plugin to $SDK_VERSION"
  (
    cd maven-java && 
    ../.github/patch-maven-versions.sh
  )
}

DEFAULT_SAMPLES="./samples"
option="${1}"
sample="${2:-$DEFAULT_SAMPLES}"
case ${option} in
   java) updateJavaSamples $sample
      ;;
   scala) updateScalaSamples $sample
      ;;
   plugin) updateMavenPlugin
      ;;
   docs) updateDocs
      ;;
   all)
     updateDocs
     updateJavaSamples $sample
     updateScalaSamples $sample
     updateMavenPlugin
      ;;
   *)
      echo "`basename ${0}`:usage: java|scala|plugin|docs|all [project-folder]"
      echo "e.g.: `basename ${0}` java ./samples/java-protobuf-customer-registry-kafka-quickstart/"
      exit 1 # Command to come out of the program with status 1
      ;;
esac