SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

V ?= 0
ifeq ($(V),0)
.SILENT:
endif

REPO_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
ENV_FILE  ?= $(REPO_ROOT)/.env
GITIGNORE ?= $(REPO_ROOT)/.gitignore
ENV_GITIGNORE_ENTRY ?= .env

# Variables requeridas (las que imprimimos)
REQUIRED_ENV_VARS ?= FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD DB_URL DB_USER DB_PASSWORD

# Variables para Makefiles anidados
BACKEND_MAKE := backend/java/Makefile
BACKEND_PARENT_MAKE := backend/java/kiwi-parent/Makefile
DB_MAKE := db/Makefile
CLIENT_MAKE := openapi/node-client/Makefile
TAG_MAJOR ?= 1
TAG_DATE ?= $(shell date +%Y%m%d)
TAG_PREFIX ?= v$(TAG_MAJOR).$(TAG_DATE)

define require_file
	if [[ ! -f "$(1)" ]]; then
		echo "ERROR: Required file '$(1)' not found."
		exit 2
	fi
endef

define require_gitignore_contains_exact_line
	if ! grep -qxF "$(1)" "$(GITIGNORE)"; then
		echo "ERROR: '$(1)' is not present in $(GITIGNORE)."
		echo "Fix: add a line with exactly: $(1)"
		exit 3
	fi
endef

define require_env_vars_in_file
	missing=0
	for v in $(REQUIRED_ENV_VARS); do
		if ! grep -Eq "^[[:space:]]*$$v[[:space:]]*=" "$(ENV_FILE)"; then
			echo "ERROR: '$$v' is missing in $(ENV_FILE)"
			missing=1
		fi
	done
	if [[ "$$missing" -ne 0 ]]; then exit 4; fi
endef

# Prints: export VAR='value' (shell-safe)
define print_exports_from_env
	for v in $(REQUIRED_ENV_VARS); do \
		line="$$(grep -E "^[[:space:]]*$${v}[[:space:]]*=" "$(ENV_FILE)" | tail -n 1 || true)"; \
		if [[ -z "$$line" ]]; then \
			echo "ERROR: '$$v' not found in $(ENV_FILE)"; exit 4; \
		fi; \
		val="$${line#*=}"; \
		val_escaped="$$(printf "%s" "$$val" | sed "s/'/'\\\\''/g")"; \
		echo "export $$v='$$val_escaped'"; \
	done
endef

# Variables para Makefiles anidados
BACKEND_MAKE := backend/java/Makefile
BACKEND_PARENT_MAKE := backend/java/kiwi-parent/Makefile
DB_MAKE := db/Makefile

.PHONY: help print_env load-env install-githooks print-next-tag release-tag
.PHONY: backend-help backend-build backend-quality backend-agent backend-native backend-run-native
.PHONY: backend-image backend-run-image backend-run-publish-image
.PHONY: db-help db-migrate db-info db-validate db-repair db-clean
.PHONY: client-help client-install client-run

help:
	@echo "=== KIWI PROJECT - PUNTO DE ENTRADA ÚNICO ==="
	@echo ""
	@echo "=== Gestión de entorno ==="
	@echo "  print_env          Print export commands from .env (use with eval)"
	@echo "  load-env           Load .env file into current shell session"
	@echo "  install-githooks   Configure git hooks path to .githooks"
	@echo ""
	@echo "=== Release ==="
	@echo "  print-next-tag     Calculate the next release tag"
	@echo "  release-tag        Create and push the next release tag"
	@echo ""
	@echo "=== Backend Java (backend/java/kiwi-parent/Makefile) ==="
	@echo "  backend-help       Show all backend targets"
	@echo "  backend-build      Build with Maven (skipTests)"
	@echo "  backend-quality    Run quality checks"
	@echo "  backend-agent      Run with native-image-agent"
	@echo "  backend-native     Build native image"
	@echo "  backend-run-native Run native binary"
	@echo ""
	@echo "=== Contenedores (backend/java/Makefile) ==="
	@echo "  backend-image           Build container image"
	@echo "  backend-run-image       Run container with env vars"
	@echo "  backend-run-publish-image Run published image from GHCR"
	@echo ""
	@echo "=== Base de datos (db/Makefile) ==="
	@echo "  db-help            Show all database targets"
	@echo "  db-migrate         Run Flyway migrations"
	@echo "  db-info            Show migration status"
	@echo "  db-validate        Validate migrations"
	@echo "  db-repair          Repair Flyway metadata"
	@echo "  db-clean           Clean database (DANGER)"
	@echo ""
	@echo "=== Cliente API (openapi/node-client/Makefile) ==="
	@echo "  client-help        Show all client targets"
	@echo "  client-install     Install npm dependencies"
	@echo "  client-run         Run the API explorer web server"
	@echo ""
	@echo "=== Uso ==="
	@echo "  eval \"\$$(make print_env)\""
	@echo "  eval \"\$$(make load-env)\""
	@echo "  make backend-build"
	@echo "  make backend-run-image"
	@echo "  make db-migrate"
	@echo ""

print_env:
	@if [[ ! -f "$(GITIGNORE)" ]]; then \
		echo "ERROR: Required file '$(GITIGNORE)' not found."; \
		exit 2; \
	fi
	@if ! grep -qxF "$(ENV_GITIGNORE_ENTRY)" "$(GITIGNORE)"; then \
		echo "ERROR: '$(ENV_GITIGNORE_ENTRY)' is not present in $(GITIGNORE)."; \
		echo "Fix: add a line with exactly: $(ENV_GITIGNORE_ENTRY)"; \
		exit 3; \
	fi
	@if [[ ! -f "$(ENV_FILE)" ]]; then \
		echo "ERROR: Required file '$(ENV_FILE)' not found."; \
		exit 2; \
	fi
	@missing=0; \
	for v in $(REQUIRED_ENV_VARS); do \
		if ! grep -Eq "^[[:space:]]*$$v[[:space:]]*=" "$(ENV_FILE)"; then \
			echo "ERROR: '$$v' is missing in $(ENV_FILE)"; \
			missing=1; \
		fi; \
	done; \
	if [[ "$$missing" -ne 0 ]]; then exit 4; fi
	@for v in $(REQUIRED_ENV_VARS); do \
		line="$$(grep -E "^[[:space:]]*$${v}[[:space:]]*=" "$(ENV_FILE)" | tail -n 1 || true)"; \
		if [[ -z "$$line" ]]; then \
			echo "ERROR: '$$v' not found in $(ENV_FILE)"; exit 4; \
		fi; \
		val="$${line#*=}"; \
		val_escaped="$$(printf "%s" "$$val" | sed "s/'/'\\\\''/g")"; \
		echo "export $$v='$$val_escaped'"; \
	done

load-env:
	@if [[ ! -f "$(ENV_FILE)" ]]; then \
		echo "ERROR: Required file '$(ENV_FILE)' not found."; \
		exit 2; \
	fi
	@echo "set -a && source $(ENV_FILE) && set +a"

install-githooks:
	git config core.hooksPath .githooks
	echo "Git hooks path configured: $$(git config --get core.hooksPath)"

print-next-tag:
	@prefix="$(TAG_PREFIX)"; \
	last_tag="$$(git tag --list "$$prefix*" --sort=-v:refname | head -n 1)"; \
	if [ -z "$$last_tag" ]; then \
		echo "$$prefix"; \
	elif [[ "$$last_tag" =~ ^$${prefix}-([0-9]+)$$ ]]; then \
		next="$$(($${BASH_REMATCH[1]} + 1))"; \
		echo "$$prefix-$$next"; \
	elif [ "$$last_tag" = "$$prefix" ]; then \
		echo "$$prefix-1"; \
	else \
		echo "$$prefix"; \
	fi

release-tag:
	@next_tag="$$( $(MAKE) --no-print-directory print-next-tag TAG_MAJOR=$(TAG_MAJOR) TAG_DATE=$(TAG_DATE) )"; \
	echo "Creating tag $$next_tag"; \
	git tag -a "$$next_tag" -m "Release $$next_tag"; \
	git push origin "$$next_tag"

# === Backend Java (backend/java/kiwi-parent/Makefile) ===
backend-help:
	@$(MAKE) -C backend/java/kiwi-parent help

backend-build:
	@$(MAKE) -C backend/java/kiwi-parent build

backend-quality:
	@$(MAKE) -C backend/java/kiwi-parent quality

backend-agent:
	@$(MAKE) -C backend/java/kiwi-parent agent

backend-native:
	@$(MAKE) -C backend/java/kiwi-parent native

backend-run-native:
	@$(MAKE) -C backend/java/kiwi-parent run-native

# === Contenedores (backend/java/Makefile) ===
backend-image:
	@$(MAKE) -C backend/java image

backend-run-image:
	@$(MAKE) -C backend/java run-image

backend-run-publish-image:
	@$(MAKE) -C backend/java run-publish-image

# === Base de datos (db/Makefile) ===
db-help:
	@$(MAKE) -C db help

db-migrate:
	@$(MAKE) -C db migrate

db-info:
	@$(MAKE) -C db info

db-validate:
	@$(MAKE) -C db validate

db-repair:
	@$(MAKE) -C db repair

db-clean:
	@$(MAKE) -C db clean

# === Cliente API (openapi/node-client/Makefile) ===
client-help:
	@$(MAKE) -C openapi/node-client help

client-install:
	@$(MAKE) -C openapi/node-client install

client-run:
	@$(MAKE) -C openapi/node-client run
