FLYWAY := ./flywayw

migrate: validate_env
	$(FLYWAY) migrate

info: validate_env
	$(FLYWAY) info

validate: validate_env
	$(FLYWAY) validate

repair: validate_env
	$(FLYWAY) repair
