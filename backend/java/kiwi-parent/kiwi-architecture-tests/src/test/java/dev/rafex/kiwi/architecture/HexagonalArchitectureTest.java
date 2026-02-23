/*
 * Copyright 2026 Raúl Eduardo González Argote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.rafex.kiwi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.rafex.kiwi")
public class HexagonalArchitectureTest {

	private static final String[] ADAPTER_PACKAGES = {
			"dev.rafex.kiwi.repository.impl..",
			"dev.rafex.kiwi.db..",
			"dev.rafex.kiwi.bootstrap..",
			"dev.rafex.kiwi.handlers..",
			"dev.rafex.kiwi.server..",
			"dev.rafex.kiwi.http..",
			"dev.rafex.kiwi.json..",
			"dev.rafex.kiwi.dtos.." };

	@ArchTest
	static final ArchRule ports_must_not_depend_on_core_or_adapters = noClasses()
			.that()
			.resideInAPackage("dev.rafex.kiwi.repository")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("dev.rafex.kiwi.services..", "dev.rafex.kiwi.models..",
					"dev.rafex.kiwi.repository.impl..", "dev.rafex.kiwi.db..", "dev.rafex.kiwi.bootstrap..",
					"dev.rafex.kiwi.handlers..", "dev.rafex.kiwi.server..", "dev.rafex.kiwi.http..",
					"dev.rafex.kiwi.json..", "dev.rafex.kiwi.dtos..");

	@ArchTest
	static final ArchRule common_must_not_depend_on_core_or_adapters = noClasses()
			.that()
			.resideInAnyPackage("dev.rafex.kiwi.logging..", "dev.rafex.kiwi.security..", "dev.rafex.kiwi.errors..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("dev.rafex.kiwi.services..", "dev.rafex.kiwi.models..",
					"dev.rafex.kiwi.repository..", "dev.rafex.kiwi.repository.impl..", "dev.rafex.kiwi.db..",
					"dev.rafex.kiwi.bootstrap..", "dev.rafex.kiwi.handlers..", "dev.rafex.kiwi.server..",
					"dev.rafex.kiwi.http..", "dev.rafex.kiwi.json..", "dev.rafex.kiwi.dtos..");

	@ArchTest
	static final ArchRule core_must_not_depend_on_adapters = noClasses()
			.that()
			.resideInAPackage("dev.rafex.kiwi.services..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(ADAPTER_PACKAGES);

	@ArchTest
	static final ArchRule infra_must_not_depend_on_transport_or_core_services = noClasses()
			.that()
			.resideInAnyPackage("dev.rafex.kiwi.repository.impl..", "dev.rafex.kiwi.db..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("dev.rafex.kiwi.services..", "dev.rafex.kiwi.models..", "dev.rafex.kiwi.bootstrap..",
					"dev.rafex.kiwi.handlers..", "dev.rafex.kiwi.server..", "dev.rafex.kiwi.http..",
					"dev.rafex.kiwi.json..", "dev.rafex.kiwi.dtos..");

	@ArchTest
	static final ArchRule transport_must_not_depend_on_infra_details = noClasses()
			.that()
			.resideInAnyPackage("dev.rafex.kiwi.handlers..", "dev.rafex.kiwi.server..", "dev.rafex.kiwi.http..",
					"dev.rafex.kiwi.json..", "dev.rafex.kiwi.dtos..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("dev.rafex.kiwi.repository.impl..", "dev.rafex.kiwi.db..");

	@ArchTest
	static final ArchRule bootstrap_must_not_depend_on_transport = noClasses()
			.that()
			.resideInAPackage("dev.rafex.kiwi.bootstrap..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("dev.rafex.kiwi.handlers..", "dev.rafex.kiwi.server..", "dev.rafex.kiwi.http..",
					"dev.rafex.kiwi.json..", "dev.rafex.kiwi.dtos..");

}
