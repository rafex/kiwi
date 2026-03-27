SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

REPO_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/../..)

.PHONY: help install run

help:
	@echo "=== KIWI NODE CLIENT ==="
	@echo ""
	@echo "  install    Install npm dependencies"
	@echo "  run        Run the API explorer web server"
	@echo ""

install:
	@echo "Installing dependencies..."
	@npm install

run:
	@echo "Starting Kiwi API Explorer..."
	@npm run web
