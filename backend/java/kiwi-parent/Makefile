SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

MVN ?= ./mvnw
JAR_JETTY ?= kiwi-transport-jetty/target/kiwi-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar
GLOWROOT_AGENT_JAR ?= ../observability/glowroot/glowroot.jar
BIN ?= kiwi-jetty.bin
GRAAL_CONFIG_DIR ?= graal-config
RES_DIR ?= kiwi-transport-jetty/META-INF/native-image
IMAGE ?= kiwi-jetty:latest
PORT ?= 8080
CONTAINER_NAME ?= kiwi-jetty-run
CONTAINER_TOOL ?= podman
DB_USER ?=
DB_PASSWORD ?=
DB_URL ?=

.PHONY: help exec-jetty build build-without-tests agent sync-meta native run-native clean-native image run-image glowroot-jetty glowroot-jetty-build

help:
	@echo "Targets:"
	@echo "  make build        -> mvn package (skipTests)"
	@echo "  make agent        -> run app with native-image-agent (generates $(GRAAL_CONFIG_DIR)/)"
	@echo "  make sync-meta    -> copy metadata/config into $(RES_DIR) and rebuild jar"
	@echo "  make native       -> build native image $(BIN)"
	@echo "  make image        -> build a container image with $(CONTAINER_TOOL) (uses Dockerfile)"
	@echo "  make run-image    -> run the container image with $(CONTAINER_TOOL) (PORT configurable, default 8080)"
	@echo "  make run-native   -> run ./$(BIN)"
	@echo "  make clean-native -> remove native binary and temp config"

exec-jetty:
	@echo "Running: exec:java with remote debugging on port 5005 (suspend=n means it won't wait for a debugger to attach)"
	$(MVN) -f kiwi-transport-jetty/pom.xml -q exec:java \
		-Dexec.args="--log=INFO" \
		-Dexec.jvmArgs='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'

glowroot-jetty:
	@echo "Running with Glowroot agent:"
	java \
	  -javaagent:$(GLOWROOT_AGENT_JAR) \
	  -jar $(JAR_JETTY) \
	  --log=DEBUG

glowroot-jetty-build: build
	@echo "Compiling with Glowroot agent for profiling:"
	java \
	  -javaagent:$(GLOWROOT_AGENT_JAR) \
	  -jar $(JAR_JETTY) \
	  --log=DEBUG

build:
	$(MVN) clean package

build-without-tests:
	$(MVN) -DskipTests clean package

agent: build-without-tests
	rm -rf "$(GRAAL_CONFIG_DIR)"
	mkdir -p "$(GRAAL_CONFIG_DIR)"
	@echo ">> App will start with agent. Hit your endpoints now. Stop with Ctrl+C."
	java -agentlib:native-image-agent=config-output-dir=./"$(GRAAL_CONFIG_DIR)" -jar "$(JAR_JETTY)"

sync-meta: build-without-tests
	./.graal_sync_and_build.sh \
		--jar "$(JAR_JETTY)" \
		--graal-config "$(GRAAL_CONFIG_DIR)" \
		--res-dir "$(RES_DIR)" \
		--mvn "$(MVN)"

# Usage: make native [MARCH="x86-64-v2 x86-64-v3"]
native:
	./.native_build.sh --out "$(BIN)" \
		--jar "$(JAR_JETTY)" \
		--graal-config "$(GRAAL_CONFIG_DIR)" \
		--res-dir "$(RES_DIR)" \
		$(foreach m,$(MARCH),--march $(m))

run-native:
	./"$(BIN)"

clean-native:
	@echo "Removing binaries for $(BIN)..."
	# remove exact bin and any march-specific variants (e.g. kiwi-backend-x86-64-v2.bin)
	rm -f $(BIN) $(basename $(BIN))* || true
	rm -rf "$(GRAAL_CONFIG_DIR)"

image:
	@echo "Checking for fat JAR: $(JAR_JETTY)"
	@if [ -f "$(JAR_JETTY)" ]; then \
		echo "Found JAR: $(JAR_JETTY)"; \
		echo "Building container image: $(IMAGE)"; \
		$(CONTAINER_TOOL) build -t "$(IMAGE)" -f Dockerfile .; \
	else \
		echo "No JAR found at $(JAR_JETTY). Please run 'make build' first." >&2; exit 1; \
	fi
	
run-image: image
	@echo "Running container $(CONTAINER_NAME), mapping host port $(PORT) -> container 8080"
	-@$(CONTAINER_TOOL) rm -f "$(CONTAINER_NAME)" 2>/dev/null || true
	@env_args=""; \
	# prefer Make variables, otherwise fall back to shell environment variables
	if [ -n "$(DB_USER)" ]; then \
		env_args="$$env_args -e DB_USER=$(DB_USER)"; \
	elif [ -n "$$DB_USER" ]; then \
		env_args="$$env_args -e DB_USER=$$DB_USER"; \
	fi; \
	if [ -n "$(DB_PASSWORD)" ]; then \
		env_args="$$env_args -e DB_PASSWORD=$(DB_PASSWORD)"; \
	elif [ -n "$$DB_PASSWORD" ]; then \
		env_args="$$env_args -e DB_PASSWORD=$$DB_PASSWORD"; \
	fi; \
	if [ -n "$(DB_URL)" ]; then \
		env_args="$$env_args -e DB_URL=$(DB_URL)"; \
	elif [ -n "$$DB_URL" ]; then \
		env_args="$$env_args -e DB_URL=$$DB_URL"; \
	fi; \
	echo "Using env: $$env_args"; \
	$(CONTAINER_TOOL) run -d --name "$(CONTAINER_NAME)" $$env_args -p "$(PORT):8080" "$(IMAGE)"

