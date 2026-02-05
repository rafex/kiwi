SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

V ?= 0
ifeq ($(V),0)
.SILENT:
endif

ENV_FILE   ?= .env
GITIGNORE  ?= .gitignore
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

define load_dotenv
	set -a
	# shellcheck disable=SC1090
	source "$(ENV_FILE)"
	set +a
endef

define require_env_vars
	missing=0
	for v in $(REQUIRED_ENV_VARS); do
		if [[ -z "$${!v:-}" ]]; then
			echo "ERROR: env var '$$v' is missing (expected in $(ENV_FILE) or exported)."
			missing=1
		fi
	done
	if [[ "$$missing" -ne 0 ]]; then exit 4; fi
endef

.PHONY: init validate_env show_env print-export

init: validate_env
	echo "init OK."

validate_env:
	$(call require_file,$(GITIGNORE))
	$(call require_gitignore_contains_exact_line,$(ENV_FILE))
	$(call require_file,$(ENV_FILE))
	$(call load_dotenv)
	$(call require_env_vars)
	echo "Environment loaded from $(ENV_FILE) and validated."

show_env: validate_env
	echo "FLYWAY_URL=$${FLYWAY_URL:-}"
	echo "FLYWAY_USER=$${FLYWAY_USER:-}"
	echo "FLYWAY_PASSWORD=*** (hidden)"

print-export: validate_env
	echo "export FLYWAY_URL='$${FLYWAY_URL:-}'"
	echo "export FLYWAY_USER='$${FLYWAY_USER:-}'"
	echo "export FLYWAY_PASSWORD='$${FLYWAY_PASSWORD:-}'"
