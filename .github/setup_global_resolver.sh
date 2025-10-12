#!/bin/bash

# .github/script.sh: Adds the Akka repository globally for sbt and Maven.

# Fail on any error, reference undefined variables, and prevent pipeline failures
set -euo pipefail

# --- Configuration ---
AKKA_RESOLVER_URL='https://repo.akka.io/maven/github_actions'
SBT_RESOLVER_LINE="resolvers += \"Akka library repository\" at \"$AKKA_RESOLVER_URL\""

SBT_PLUGIN_PROJECT_NAME="${1:-}"
# Uses GITHUB_WORKSPACE (set by the runner) or defaults to the current directory if run locally
SBT_SCRIPTED_TESTS_BASE_DIR="${GITHUB_WORKSPACE:-.}/${SBT_PLUGIN_PROJECT_NAME}/src/sbt-test"

# ----------------------------------------

## Setup for sbt
setup_sbt() {
    echo "--- Setting up Akka resolver for sbt global configuration (~/.sbt/1.0/resolvers.sbt)"
    mkdir -p ~/.sbt/1.0
    echo "$SBT_RESOLVER_LINE" >> ~/.sbt/1.0/resolvers.sbt
    echo "✅ Added resolver to ~/.sbt/1.0/resolvers.sbt"
}

## Setup for Scripted Tests
setup_scripted_tests() {
    echo -e "\n--- Setting up Akka resolver for sbt scripted tests (globally per test case)"

    if [ ! -d "$SBT_SCRIPTED_TESTS_BASE_DIR" ]; then
        echo "⚠️ Warning: Tests directory not found: $SBT_SCRIPTED_TESTS_BASE_DIR. Skipping setup for scripted tests."
        return 0
    fi

    echo "Scanning for sbt projects (directories with 'build.sbt') in sbt-tests: $SBT_SCRIPTED_TESTS_BASE_DIR"

    # Use find to recursively locate all 'build.sbt' files.
    # We then loop through the parent directories of these files.
    find "$SBT_SCRIPTED_TESTS_BASE_DIR" -type f -name 'build.sbt' | while IFS= read -r BUILD_FILE_PATH; do
        # Extract the directory containing build.sbt
        PROJECT_ROOT_DIR=$(dirname "$BUILD_FILE_PATH")
        TARGET_DIR="${PROJECT_ROOT_DIR}/global"
        RESOLVERS_FILE="${TARGET_DIR}/resolvers.sbt"

        mkdir -p "$TARGET_DIR"
        echo "$SBT_RESOLVER_LINE" > "$RESOLVERS_FILE"
        echo "-> Configured resolver for project: $PROJECT_ROOT_DIR"
    done
    echo "✅ Finished setting up resolvers for sbt scripted tests."
}

## Setup for Maven
setup_maven() {
    echo -e "\n--- Setting up Akka resolver for Maven global configuration (~/.m2/settings.xml)"
    mkdir -p ~/.m2

    # Create the settings.xml file with the Akka repository configuration
    cat > ~/.m2/settings.xml <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>akka-repo-redirect</id>
      <mirrorOf>akka-repository</mirrorOf>
      <url>$AKKA_RESOLVER_URL</url>
    </mirror>
    <mirror>
      <mirrorOf>external:http:*</mirrorOf>
      <name>Pseudo repository to mirror external repositories initially using HTTP.</name>
      <url>http://0.0.0.0/</url>
      <blocked>true</blocked>
      <id>maven-default-http-blocker</id>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <id>akka-repo</id>
      <repositories>
        <repository>
          <id>akka-repository</id>
          <url>$AKKA_RESOLVER_URL</url>
        </repository>
      </repositories>

      <pluginRepositories>
        <pluginRepository>
        <id>akka-plugin-repository</id>
        <url>$AKKA_RESOLVER_URL</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>akka-repo</activeProfile>
  </activeProfiles>
</settings>
EOF
    echo "✅ Created/Overwrote ~/.m2/settings.xml with Akka repository configuration."
}

# --- Main Execution ---
main() {
    setup_sbt
    # Only run scripted test setup if the sbt plugin project name (passed as $1) is non-empty.
    if [ -n "$SBT_PLUGIN_PROJECT_NAME" ]; then
        echo "Using SBT plugin project name: $SBT_PLUGIN_PROJECT_NAME to locate scripted tests."
        setup_scripted_tests
    else
        echo "⚠️ SBT plugin project name (argument \$1) is empty. Skipping scripted test setup."
    fi
    setup_maven
    echo -e "\n🎉 Akka resolvers setup complete."
}

main