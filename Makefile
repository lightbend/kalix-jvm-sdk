
projectname = trinity


# removes project if exists
# creates archetype
# runs the archetype to create trinity
# copy the files from integration test, unit test, the CounterImpl 
# fixes the Main (from missing SERVICE variable)
# runs integration and unit tests
run-all: clean create-archetype create-project cp-all-and-fix-and-test

clean:
	@echo "### removing project $(projectname)"
	rm -rf $(projectname)

create-archetype:
	@echo "### generating archetype for value entity"
	cd maven-java && mvn install

create-project:
	@echo "### gerating project with archetype"
	
	@echo com.example > responses.txt
	@echo $(projectname) >> responses.txt
	@echo " " >> responses.txt
	@echo " " >> responses.txt
	@echo Y >> responses.txt

	cat responses.txt | mvn archetype:generate \
	-DarchetypeGroupId=com.akkaserverless \
	-DarchetypeArtifactId=akkaserverless-maven-archetype \
	-DarchetypeVersion=1.0-SNAPSHOT -Darchetype.catalog=local


# copy the files from integration test, unit test, the CounterImpl 
# runs integration and unit tests
cp-all-and-test: cp-counter-impl cp-it-test cp-unit-test test-it test-unit

# copy the files from integration test, unit test, the CounterImpl 
# fixes the Main (from missing SERVICE variable)
# runs integration and unit tests
cp-all-and-fix-and-test: cp-counter-impl cp-it-test cp-unit-test fix-main test-it test-unit

fix-main:
	@echo "### copying Main from samples/valueentity-counter"
	@echo "### bear in mind the Main has the package com.example"
	cp samples/valueentity-counter/src/main/java/com/example/Main.java \
	$(projectname)/src/main/java/com/example/Main.java 

test-it:
	@echo "### running integration tests for $(projectname)"
	cd $(projectname) && mvn verify -Pit

test-unit:
	@echo "### running unit tests for $(projectname)"
	cd $(projectname) && mvn verify

cp-it-test:
	@echo "### copying integration test solution from samples/valueentity-counter"
	mkdir -p $(projectname)/src/it/java/com/example/domain
	cp samples/valueentity-counter/src/it/java/com/example/domain/CounterIntegrationTest.java \
	$(projectname)/src/it/java/com/example/domain/CounterIntegrationTest.java

cp-unit-test:
	@echo "### copying unit test solution from samples/valueentity-counter"
	mkdir -p $(projectname)/src/test/java/com/example/domain
	cp samples/valueentity-counter/src/test/java/com/example/domain/CounterTest.java \
	$(projectname)/src/test/java/com/example/domain/CounterTest.java

cp-counter-impl:
	@echo "### copying CounterImpl from samples/valueentity-counter"
	mkdir -p $(projectname)/src/main/java/com/example/domain
	cp samples/valueentity-counter/src/main/java/com/example/domain/CounterImpl.java \
	$(projectname)/src/main/java/com/example/domain/CounterImpl.java
