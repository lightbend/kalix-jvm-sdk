#!/usr/bin/env bash

  if [ $# -eq 0 ]; then 
    echo "No arguments provided!"
    echo "This scripted is used to update sample versions to the latest snapshot."
    echo "You should call it by passing one or more samples as arguments."
    echo "eg: ./updateSampleVersions.sh samples/java-valueentity-counter samples/scala-valueentity-counter"
    echo "or simply using bash expansion..."
    echo "eg: ./updateSampleVersions.sh samples/java-*"
  else 
    source publishLocally.sh
    export SDK_VERSION="$SDK_VERSION"
  
    echo "------------------------------------------------------------------------"
    for i in "$@"
    do
      echo
      if [ -f $i/pom.xml ]; then
        echo "Updating pom.xml file in: $i"
        sh ./updateSdkVersions.sh java $i
      elif [ -f $i/build.sbt ]; then
        echo "Updating plugins.sbt file in: $i"
        sh ./updateSdkVersions.sh scala $i
      fi
    done
    echo "------------------------------------------------------------------------"
  fi
