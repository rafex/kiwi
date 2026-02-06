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
REQUIRED_ENV_VARS ?= FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD

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

.PHONY: help
help:
	echo "Targets:"
	echo "  print_env   Print export commands from .env (use with eval)"
	echo ""
	echo "Usage:"
	echo "  eval \"\$$(make print_env)\""
	echo ""

.PHONY: print_env
print_env:
	$(call require_file,$(GITIGNORE))
	$(call require_gitignore_contains_exact_line,$(ENV_GITIGNORE_ENTRY))
	$(call require_file,$(ENV_FILE))
	$(call require_env_vars_in_file)
	$(call print_exports_from_env)
