# Make Akka Serverless Java SDK documentation

module   := java-services
upstream := lightbend/akkaserverless-java-sdk
branch   := docs/current
sources  := src build/src/managed

src_managed := build/src/managed
managed_attachments := ${src_managed}/modules/${module}/attachments
managed_examples := ${src_managed}/modules/${module}/examples
managed_partials := ${src_managed}/modules/${module}/partials

.SILENT:

build: clean managed

clean:
	rm -rf build

managed: attributes apidocs examples
	mkdir -p "${src_managed}"
	cp src/antora.yml "${src_managed}/antora.yml"

attributes:
	mkdir -p "${managed_partials}"
	bin/version.sh | xargs -0  printf ":akkaserverless-java-sdk-version: %s" \
		> "${managed_partials}/attributes.adoc"
	echo ":minimum-java-version: 8" \
		>> "${managed_partials}/attributes.adoc"
	echo ":recommended-java-version: 11" \
		>> "${managed_partials}/attributes.adoc"

apidocs:
	cd .. && sbt sdk/doc
	mkdir -p "${managed_attachments}"
	rsync -a ../sdk/target/api/ "${managed_attachments}/api/"

examples:
	mkdir -p "${managed_examples}"
	rsync -a --exclude-from=.examplesignore ../samples/java-customer-registry/ "${managed_examples}/java-customer-registry/"
	rsync -a --exclude-from=.examplesignore ../samples/java-eventing-shopping-cart/ "${managed_examples}/java-eventing-shopping-cart/"
	rsync -a --exclude-from=.examplesignore ../samples/java-valueentity-shopping-cart/ "${managed_examples}/java-valueentity-shopping-cart/"
	rsync -a --exclude-from=.examplesignore ../samples/valueentity-counter/ "${managed_examples}/valueentity-counter/"

deploy: clean managed
	bin/deploy.sh --module ${module} --upstream ${upstream} --branch ${branch} ${sources}
