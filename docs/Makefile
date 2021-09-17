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

prepare: 
	mkdir -p "${src_managed}"
	cp src/antora.yml "${src_managed}/antora.yml"

managed: prepare attributes apidocs examples bundles

attributes:
	mkdir -p "${managed_partials}"
	bin/version.sh | xargs -0  printf ":akkaserverless-java-sdk-version: %s" \
		> "${managed_partials}/attributes.adoc"
	echo ":minimum-java-version: 8" \
		>> "${managed_partials}/attributes.adoc"
	echo ":recommended-java-version: 11" \
		>> "${managed_partials}/attributes.adoc"
	echo ":java_minimum_sdk_version: 0.7.0" \
		>> "${managed_partials}/attributes.adoc"

apidocs:
	cd .. && sbt sdkJava/doc
	cd .. && sbt testkitJava/doc
	mkdir -p "${managed_attachments}"
	rsync -a ../sdk/java-sdk/target/api/ "${managed_attachments}/api/"
	rsync -a ../testkit-java/target/api/ "${managed_attachments}/testkit/"

examples:
	mkdir -p "${managed_examples}"
	rsync -a --exclude-from=.examplesignore ../samples/ "${managed_examples}/"
  
bundles:
	bin/bundle.sh --zip "${managed_attachments}/java-customer-registry-quickstart.zip" ../samples/java-customer-registry-quickstart

dev: clean managed validate-xrefs dev-html

# like dev but without apidocs, bundles and testkits. Useful for fast dev cycles
quick-dev: clean prepare attributes examples dev-html

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
		-c "cd /antora/${base_path} && find src -name '*.adoc' -print0 | xargs -0 -n1 asciidoc-link-check --progress --config config/validate-links.json"

deploy: clean managed
	bin/deploy.sh --module ${module} --upstream ${upstream} --branch ${branch} ${sources}
