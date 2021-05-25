# Make Akka Serverless Java SDK documentation

# FIXME: set up new documentation building

module   := java
upstream := lightbend/akkaserverless-java-sdk
branch   := java-sdk/docs/current
sources  := src build/src/managed

cloudstate_antora_download := https://github.com/cloudstateio/cloudstate-antora/raw/master/cloudstate-antora
cloudstate_antora := .cache/bin/cloudstate-antora
descriptor := build/site.yml
root_dir := $(shell git rev-parse --show-toplevel)
src_managed := build/src/managed
managed_attachments := ${src_managed}/modules/java/attachments
managed_partials := ${src_managed}/modules/java/partials

.SILENT:

build: clean managed validate html

${cloudstate_antora}:
	mkdir -p $$(dirname ${cloudstate_antora})
	curl -Lo ${cloudstate_antora} ${cloudstate_antora_download}
	chmod +x ${cloudstate_antora}

clean-cache:
	rm -rf .cache

update: clean-cache ${cloudstate_antora}

clean: ${cloudstate_antora}
	${cloudstate_antora} clean

managed: attributes apidocs
	mkdir -p "${src_managed}"
	cp src/antora.yml "${src_managed}/antora.yml"

attributes: ${cloudstate_antora}
	mkdir -p "${managed_partials}"
	${cloudstate_antora} version --prefix "java-support-" | xargs -0  printf ":akkaserverless-java-sdk-version: %s" \
		> "${managed_partials}/attributes.adoc"

apidocs:
	cd ${root_dir}/java-sdk && sbt sdk/doc
	mkdir -p "${managed_attachments}"
	rsync -a "${root_dir}/java-sdk/sdk/target/api/" "${managed_attachments}/api/"

${descriptor}: ${cloudstate_antora}
	mkdir -p $$(dirname ${descriptor})
	${cloudstate_antora} source --preview --upstream ${upstream} ${sources} > build/source.yml
	${cloudstate_antora} site --preview --exclude ${module} --exclude springboot build/source.yml > ${descriptor}

validate: ${descriptor}
	${cloudstate_antora} validate ${descriptor}

html: ${descriptor}
	${cloudstate_antora} build ${descriptor}

build-author-mode: clean managed validate html-author-mode

html-author-mode: ${descriptor}
	${cloudstate_antora} build --author-mode ${descriptor}

validate-links: ${cloudstate_antora}
	${cloudstate_antora} validate --no-xrefs --links --links-config config/validate-links.json

deploy: clean managed
	${cloudstate_antora} deploy --module ${module} --upstream ${upstream} --branch ${branch} ${sources}
