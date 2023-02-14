# Make Kalix Java SDK documentation

upstream := lightbend/kalix-jvm-sdk
branch   := docs/kalix-current
sources  := src build/src/managed

src_managed := build/src/managed
java_managed_attachments := ${src_managed}/modules/java/attachments
java_managed_examples := ${src_managed}/modules/java/examples
java_managed_partials := ${src_managed}/modules/java/partials

scala_managed_attachments := ${src_managed}/modules/scala/attachments

spring_managed_attachments := ${src_managed}/modules/spring/attachments
spring_managed_examples := ${src_managed}/modules/spring/examples
spring_managed_partials := ${src_managed}/modules/spring/partials

antora_docker_image := gcr.io/kalix-public/kalix-docbuilder
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
	mkdir -p "${java_managed_partials}"
	bin/version.sh | xargs -0  printf ":kalix-jvm-sdk-version: %s" \
		> "${java_managed_partials}/attributes.adoc"
	echo ":java-version: 11" \
		>> "${java_managed_partials}/attributes.adoc"
	echo ":spring-java-version: 17" \
		>> "${java_managed_partials}/attributes.adoc"
	echo ":minimum_maven_version: 3.6" \
		>> "${java_managed_partials}/attributes.adoc"
	echo ":minimum_sbt_version: 1.3.6" \
    	>> "${java_managed_partials}/attributes.adoc"
	echo ":minimum_docker_version: 20.10.14" \
		>> "${java_managed_partials}/attributes.adoc"
	echo ":java_minimum_sdk_version: 0.7.0" \
		>> "${java_managed_partials}/attributes.adoc"
	echo ":console: https://console.kalix.io/" \
		>> "${java_managed_partials}/attributes.adoc"

	mkdir -p "${spring_managed_partials}"
	cp "${java_managed_partials}/attributes.adoc" "${spring_managed_partials}/attributes.adoc"

apidocs:
	mkdir -p "${java_managed_attachments}"
	mkdir -p "${spring_managed_attachments}"
	cd .. && sbt sdkJava/doc sdkJavaTestKit/doc sdkScala/doc sdkScalaTestKit/doc sdkJavaSpring/doc sdkJavaSpringTestKit/doc
	rsync -a ../sdk/java-sdk/target/api/ "${java_managed_attachments}/api/"
	rsync -a ../sdk/java-sdk-testkit/target/api/ "${java_managed_attachments}/testkit/"
	rsync -a ../sdk/scala-sdk/target/scala-2.13/api/ "${java_managed_attachments}/scala-api/"
	rsync -a ../sdk/scala-sdk-testkit/target/scala-2.13/api/ "${java_managed_attachments}/scala-testkit-api/"
	rsync -a ../sdk/java-sdk/target/api/ "${spring_managed_attachments}/api/"
	rsync -a ../sdk/java-sdk-testkit/target/api/ "${spring_managed_attachments}/testkit/"
	rsync -a ../sdk/java-sdk-spring/target/api/ "${spring_managed_attachments}/api/"
	rsync -a ../sdk/java-sdk-spring-testkit/target/api/ "${spring_managed_attachments}/testkit/"
	mkdir -p "${scala_managed_attachments}"
	mkdir -p "${spring_managed_attachments}"
	bin/version.sh > "${java_managed_attachments}/latest-version.txt" \
		&& cp "${java_managed_attachments}/latest-version.txt" "${scala_managed_attachments}" \
		&& cp "${java_managed_attachments}/latest-version.txt" "${spring_managed_attachments}"

examples:
	mkdir -p "${java_managed_examples}"
	rsync -a --exclude-from=.examplesignore ../samples/java-* "${java_managed_examples}/"
	rsync -a --exclude-from=.examplesignore ../samples/scala-* "${java_managed_examples}/"
	rsync -a --exclude-from=.examplesignore ../samples/spring-* "${spring_managed_examples}/"

bundles:
	bin/bundle.sh --zip "${java_managed_attachments}/java-customer-registry-quickstart.zip" ../samples/java-customer-registry-quickstart
	bin/bundle.sh --zip "${java_managed_attachments}/java-customer-registry-views-quickstart.zip" ../samples/java-customer-registry-views-quickstart
	bin/bundle.sh --zip "${java_managed_attachments}/java-customer-registry-kafka-quickstart.zip" ../samples/java-customer-registry-kafka-quickstart
	bin/bundle.sh --zip "${java_managed_attachments}/java-shopping-cart-quickstart.zip" ../samples/java-shopping-cart-quickstart
	bin/bundle.sh --zip "${java_managed_attachments}/scala-customer-registry-quickstart.zip" ../samples/scala-customer-registry-quickstart
	bin/bundle.sh --zip "${spring_managed_attachments}/spring-customer-registry-quickstart.zip" ../samples/spring-customer-registry-quickstart

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

# TODO: we will need to include publication for spring module once we want it out
deploy: clean managed
	bin/deploy.sh --module java --upstream ${upstream} --branch ${branch} ${sources}
