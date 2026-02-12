SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

MVN ?= ./mvnw
JAR ?= target/kiwi-backend-1.0-SNAPSHOT-jar-with-dependencies.jar
BIN ?= kiwi-backend.bin
GRAAL_CONFIG_DIR ?= graal-config
RES_DIR ?= META-INF/native-image

.PHONY: help build agent sync-meta native run-native clean-native

help:
	@echo "Targets:"
	@echo "  make build        -> mvn package (skipTests)"
	@echo "  make agent        -> run app with native-image-agent (generates $(GRAAL_CONFIG_DIR)/)"
	@echo "  make sync-meta    -> copy metadata/config into $(RES_DIR) and rebuild jar"
	@echo "  make native       -> build native image $(BIN)"
	@echo "  make run-native   -> run ./$(BIN)"
	@echo "  make clean-native -> remove native binary and temp config"

build:
	$(MVN) -q -DskipTests package
	@test -f "$(JAR)" || (echo "Jar not found: $(JAR)"; ls -la target; exit 1)

agent: build
	rm -rf "$(GRAAL_CONFIG_DIR)"
	mkdir -p "$(GRAAL_CONFIG_DIR)"
	@echo ">> App will start with agent. Hit your endpoints now. Stop with Ctrl+C."
	java -agentlib:native-image-agent=config-output-dir=./"$(GRAAL_CONFIG_DIR)" -jar "$(JAR)"

sync-meta: build
	./.graal_sync_and_build.sh \
		--jar "$(JAR)" \
		--graal-config "$(GRAAL_CONFIG_DIR)" \
		--res-dir "$(RES_DIR)" \
		--mvn "$(MVN)"

native:
	./.native_build.sh --out "$(BIN)" \
		--jar "$(JAR)" \
		--graal-config "$(GRAAL_CONFIG_DIR)" \
		--res-dir "$(RES_DIR)"

run-native:
	./"$(BIN)"

clean-native:
	rm -f "$(BIN)"
	rm -rf "$(GRAAL_CONFIG_DIR)"
