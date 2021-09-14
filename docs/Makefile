# Make Akka Serverless Java SDK documentation

module   := java
upstream := lightbend/akkaserverless-java-sdk
branch   := docs/current
sources  := src build/src/managed

src_managed := build/src/managed
managed_attachments := ${src_managed}/modules/${module}/attachments
managed_examples := ${src_managed}/modules/${module}/examples
managed_partials := ${src_managed}/modules/${module}/partials

antora_docker_image := gcr.io/akkaserverless-public/akkaserverless-docbuilder
antora_docker_image_tag := 0.0.5
root_dir := $(shell git rev-parse --show-toplevel)
base_path := $(shell git rev-parse --show-prefix)

.SILENT:

build: dev

clean:
	rm -rf build

managed: attributes apidocs examples bundles
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
	echo ":java_minimum_sdk_version: 0.7.0-beta.19" \
		>> "${managed_partials}/attributes.adoc"

apidocs:
	cd .. && sbt sdk/doc
	cd .. && sbt testkit/doc
	mkdir -p "${managed_attachments}"
	rsync -a ../sdk/target/api/ "${managed_attachments}/api/"
	rsync -a ../testkit/target/api/ "${managed_attachments}/testkit/"

examples:
	mkdir -p "${managed_examples}"
	rsync -a --exclude-from=.examplesignore ../samples/customer-registry-quickstart/ "${managed_examples}/customer-registry-quickstart/"
	rsync -a --exclude-from=.examplesignore ../samples/java-valueentity-customer-registry/ "${managed_examples}/java-valueentity-customer-registry/"
	rsync -a --exclude-from=.examplesignore ../samples/java-eventsourced-customer-registry/ "${managed_examples}/java-eventsourced-customer-registry/"
	rsync -a --exclude-from=.examplesignore ../samples/java-eventing-shopping-cart/ "${managed_examples}/java-eventing-shopping-cart/"
	rsync -a --exclude-from=.examplesignore ../samples/java-eventsourced-shopping-cart/ "${managed_examples}/java-eventsourced-shopping-cart/"
	rsync -a --exclude-from=.examplesignore ../samples/java-replicatedentity-shopping-cart/ "${managed_examples}/java-replicatedentity-shopping-cart/"
	rsync -a --exclude-from=.examplesignore ../samples/java-valueentity-shopping-cart/ "${managed_examples}/java-valueentity-shopping-cart/"
	rsync -a --exclude-from=.examplesignore ../samples/replicatedentity-examples/ "${managed_examples}/replicatedentity-examples/"
	rsync -a --exclude-from=.examplesignore ../samples/valueentity-counter/ "${managed_examples}/valueentity-counter/"
	rsync -a --exclude-from=.examplesignore ../samples/fibonacci-action/ "${managed_examples}/fibonacci-action/"
	rsync -a --exclude-from=.examplesignore ../samples/doc-snippets/ "${managed_examples}/doc-snippets/"
	rsync -a --exclude-from=.examplesignore ../samples/first-service/ "${managed_examples}/first-service/"

bundles:
	bin/bundle.sh --zip "${managed_attachments}/customer-registry-quickstart.zip" ../samples/customer-registry-quickstart

dev: clean managed validate-xrefs dev-html

dev-html:
	docker run \
		-v ${root_dir}:/antora \
		--rm \
		--entrypoint /bin/sh \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		-c "cd /antora/${base_path} && antora --cache-dir=.cache/antora --stacktrace --log-failure-level=warn dev/antora.yml"
	@echo "Generated docs at dev/build/site/java/index.html"

validate-xrefs:
	docker run \
		-v ${root_dir}:/antora \
		--rm \
		--entrypoint /bin/sh \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		-c 'cd /antora/${base_path} && NODE_PATH="$$(npm -g root)" antora --generator @antora/xref-validator dev/antora.yml'

validate-links:
	docker run \
		-v ${root_dir}:/antora \
		--rm \
		--entrypoint /bin/sh \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		-c "cd /antora/${base_path} && find src -name '*.adoc' -print0 | xargs -0 -n1 asciidoc-link-check --progress"

deploy: clean managed
	bin/deploy.sh --module ${module} --upstream ${upstream} --branch ${branch} ${sources}
