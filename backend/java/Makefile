SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

JAR_JETTY ?= kiwi-parent/kiwi-transport-jetty/target/kiwi-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar
IMAGE ?= kiwi-jetty:latest
PUBLISHED_IMAGE ?= ghcr.io/rafex/kiwi-jetty-backend:latest
PORT ?= 8080
GLOWROOT_PORT ?= 4000
CONTAINER_NAME ?= kiwi-jetty-run
CONTAINER_TOOL ?= podman
DB_USER ?=
DB_PASSWORD ?=
DB_URL ?=
JWT_ISS ?= dev.rafex.kiwi
JWT_AUD ?= kiwi-backend
JWT_SECRET ?=
JWT_TTL_SECONDS ?= 3600
ENVIRONMENT ?= work02

.PHONY: help image run-image run-publish-image run-image-from-current

help:
	@echo "Targets:"
	@echo "  make image      -> build container image ($(IMAGE))"
	@echo "  make run-image  -> run container and pass app env vars"
	@echo "  make run-publish-image -> pull and run published image ($(PUBLISHED_IMAGE))"
	@echo ""
	@echo "Common overrides:"
	@echo "  IMAGE, PUBLISHED_IMAGE, PORT, GLOWROOT_PORT, CONTAINER_NAME, CONTAINER_TOOL"
	@echo "  DB_USER, DB_PASSWORD, DB_URL"
	@echo "  JWT_ISS, JWT_AUD, JWT_SECRET, JWT_TTL_SECONDS"
	@echo "  ENVIRONMENT"

image:
	@echo "Checking for fat JAR: $(JAR_JETTY)"
	@if [ -f "$(JAR_JETTY)" ]; then \
		echo "Found JAR: $(JAR_JETTY)"; \
		echo "Building container image: $(IMAGE)"; \
		$(CONTAINER_TOOL) build -t "$(IMAGE)" -f Dockerfile .; \
	else \
		echo "No JAR found at $(JAR_JETTY). Please run 'make build' first." >&2; exit 1; \
	fi
	
run-image: image run-image-from-current

run-publish-image:
	@echo "Pulling published image $(PUBLISHED_IMAGE)"
	@$(CONTAINER_TOOL) pull "$(PUBLISHED_IMAGE)"
	@$(MAKE) --no-print-directory run-image-from-current IMAGE="$(PUBLISHED_IMAGE)"

run-image-from-current:
	@echo "Running container $(CONTAINER_NAME), mapping host ports $(PORT)->8080 and $(GLOWROOT_PORT)->4000"
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
	if [ -n "$(JWT_ISS)" ]; then \
		env_args="$$env_args -e JWT_ISS=$(JWT_ISS)"; \
	elif [ -n "$$JWT_ISS" ]; then \
		env_args="$$env_args -e JWT_ISS=$$JWT_ISS"; \
	fi; \
	if [ -n "$(JWT_AUD)" ]; then \
		env_args="$$env_args -e JWT_AUD=$(JWT_AUD)"; \
	elif [ -n "$$JWT_AUD" ]; then \
		env_args="$$env_args -e JWT_AUD=$$JWT_AUD"; \
	fi; \
	if [ -n "$(JWT_SECRET)" ]; then \
		env_args="$$env_args -e JWT_SECRET=$(JWT_SECRET)"; \
	elif [ -n "$$JWT_SECRET" ]; then \
		env_args="$$env_args -e JWT_SECRET=$$JWT_SECRET"; \
	else \
		gen_jwt_secret="$$(uuidgen)"; \
		env_args="$$env_args -e JWT_SECRET=$$gen_jwt_secret"; \
	fi; \
	if [ -n "$(JWT_TTL_SECONDS)" ]; then \
		env_args="$$env_args -e JWT_TTL_SECONDS=$(JWT_TTL_SECONDS)"; \
	elif [ -n "$$JWT_TTL_SECONDS" ]; then \
		env_args="$$env_args -e JWT_TTL_SECONDS=$$JWT_TTL_SECONDS"; \
	fi; \
	if [ -n "$(ENVIRONMENT)" ]; then \
		env_args="$$env_args -e ENVIRONMENT=$(ENVIRONMENT)"; \
	elif [ -n "$$ENVIRONMENT" ]; then \
		env_args="$$env_args -e ENVIRONMENT=$$ENVIRONMENT"; \
	fi; \
	echo "Using env: $$env_args"; \
	$(CONTAINER_TOOL) run -d --name "$(CONTAINER_NAME)" $$env_args -p "$(PORT):8080" -p "$(GLOWROOT_PORT):4000" "$(IMAGE)"
