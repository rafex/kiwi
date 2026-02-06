SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

FLYWAY := ./flywayw
ARGS   ?=
NONINTERACTIVE ?= 0

.PHONY: help
help:
	@echo ""
	@echo "Targets:"
	@echo "  migrate    Run flyway migrate"
	@echo "  info       Show flyway info"
	@echo "  validate   Run flyway validate"
	@echo "  repair     Run flyway repair"
	@echo "  clean      Run flyway clean (DANGER)"
	@echo ""
	@echo "Options:"
	@echo "  ARGS='-X'               (extra Flyway args)"
	@echo "  NONINTERACTIVE=1        (fail if env vars are missing)"
	@echo ""
	@echo "Credentials via env (recommended):"
	@echo "  FLYWAY_URL, FLYWAY_USER, FLYWAY_PASSWORD"
	@echo "If not set, it will prompt (unless NONINTERACTIVE=1)."
	@echo ""

.PHONY: _env
_env:
	@if [[ -z "$${FLYWAY_URL:-}" ]]; then \
		if [[ "$(NONINTERACTIVE)" == "1" ]]; then \
			echo "ERROR: FLYWAY_URL is required"; exit 2; \
		fi; \
		read -rp "Enter FLYWAY_URL: " FLYWAY_URL; \
	fi
	@if [[ -z "$${FLYWAY_USER:-}" ]]; then \
		if [[ "$(NONINTERACTIVE)" == "1" ]]; then \
			echo "ERROR: FLYWAY_USER is required"; exit 2; \
		fi; \
		read -rp "Enter FLYWAY_USER: " FLYWAY_USER; \
	fi
	@if [[ -z "$${FLYWAY_PASSWORD:-}" ]]; then \
		if [[ "$(NONINTERACTIVE)" == "1" ]]; then \
			echo "ERROR: FLYWAY_PASSWORD is required"; exit 2; \
		fi; \
		read -srp "Enter FLYWAY_PASSWORD: " FLYWAY_PASSWORD; echo; \
	fi
	@export FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD

.PHONY: migrate
migrate: _env
	$(FLYWAY) $(ARGS) migrate

.PHONY: info
info: _env
	$(FLYWAY) $(ARGS) info

.PHONY: validate
validate: _env
	$(FLYWAY) $(ARGS) validate

.PHONY: repair
repair: _env
	$(FLYWAY) $(ARGS) repair

.PHONY: clean
clean: _env
	@echo "WARNING: flyway clean will DROP all objects in the configured schemas."
	@if [[ "$(NONINTERACTIVE)" == "1" ]]; then \
		echo "ERROR: clean disabled in NONINTERACTIVE mode"; exit 3; \
	fi
	@read -rp "Type 'YES' to confirm: " ans; \
	if [[ "$$ans" != "YES" ]]; then echo "Aborted."; exit 4; fi
	$(FLYWAY) $(ARGS) clean
