/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.initializr.generator.spike.build;

import io.spring.initializr.generator.buildsystem.gradle.GradleBuild;
import io.spring.initializr.generator.spring.build.BuildCustomizer;
import io.spring.initializr.generator.spring.code.kotlin.KotlinProjectSettings;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.InitializrMetadata;

/**
 *
 * {@link BuildCustomizer} for Gradle that configures the JPA Kotlin plugin if a JPA
 * related dependency is present.
 *
 * @author Madhura Bhave
 */
public class KotlinJpaGradleBuildCustomizer implements BuildCustomizer<GradleBuild> {

	private final InitializrMetadata metadata;

	private final KotlinProjectSettings settings;

	public KotlinJpaGradleBuildCustomizer(InitializrMetadata metadata,
			KotlinProjectSettings settings) {
		this.metadata = metadata;
		this.settings = settings;
	}

	@Override
	public void customize(GradleBuild build) {
		if (hasJpaFacet(build)) {
			build.addPlugin("org.jetbrains.kotlin.plugin.jpa",
					this.settings.getVersion());
			build.applyPlugin("kotlin-jpa");
		}
	}

	private boolean hasJpaFacet(GradleBuild build) {
		return build.dependencies().ids().anyMatch((id) -> {
			Dependency dependency = this.metadata.getDependencies().get(id);
			if (dependency != null) {
				return dependency.getFacets().contains("jpa");
			}
			return false;
		});
	}

}
