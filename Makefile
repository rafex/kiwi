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

.PHONY: help print_env install-githooks print-next-tag release-tag
help:
	echo "Targets:"
	echo "  print_env   Print export commands from .env (use with eval)"
	echo "  install-githooks   Configure git hooks path to .githooks"
	echo "  print-next-tag   Calculate the next release tag"
	echo "  release-tag   Create and push the next release tag"
	echo ""
	echo "Usage:"
	echo "  eval \"\$$(make print_env)\""
	echo ""

print_env:
	$(call require_file,$(GITIGNORE))
	$(call require_gitignore_contains_exact_line,$(ENV_GITIGNORE_ENTRY))
	$(call require_file,$(ENV_FILE))
	$(call require_env_vars_in_file)
	$(call print_exports_from_env)

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
