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

import java.util.Collections;

import io.spring.initializr.generator.buildsystem.gradle.GradleBuild;
import io.spring.initializr.generator.spring.code.kotlin.SimpleKotlinProjectSettings;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.test.metadata.InitializrMetadataTestBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KotlinJpaGradleBuildCustomizer}.
 *
 * @author Madhura Bhave
 */
public class KotlinJpaGradleBuildCustomizerTests {

	@Test
	public void customizeWhenJpaFacetPresentShouldAddKotlinJpaPlugin() {
		Dependency dependency = Dependency.withId("foo");
		dependency.setFacets(Collections.singletonList("jpa"));
		GradleBuild build = getCustomizedBuild(dependency);
		assertThat(build.getAppliedPlugins()).contains("kotlin-jpa");
		assertThat(build.getPlugins()).hasSize(1);
		assertThat(build.getPlugins().get(0).getId())
				.isEqualTo("org.jetbrains.kotlin.plugin.jpa");
		assertThat(build.getPlugins().get(0).getVersion()).isEqualTo("1.2.70");
	}

	@Test
	public void customizeWhenJpaFacetAbsentShouldNotAddKotlinJpaPlugin() {
		Dependency dependency = Dependency.withId("foo");
		GradleBuild build = getCustomizedBuild(dependency);
		assertThat(build.getAppliedPlugins()).hasSize(0);
		assertThat(build.getPlugins()).hasSize(0);
	}

	private GradleBuild getCustomizedBuild(Dependency dependency) {
		InitializrMetadata metadata = InitializrMetadataTestBuilder.withDefaults()
				.addDependencyGroup("test", dependency).build();
		SimpleKotlinProjectSettings settings = new SimpleKotlinProjectSettings("1.2.70");
		KotlinJpaGradleBuildCustomizer customizer = new KotlinJpaGradleBuildCustomizer(
				metadata, settings);
		GradleBuild build = createBuild(metadata);
		build.dependencies().add("foo");
		customizer.customize(build);
		return build;
	}

	private GradleBuild createBuild(InitializrMetadata metadata) {
		return new GradleBuild(new InitializrMetadataBuildItemResolver(metadata));
	}

}
