/*
 * Copyright 2012-2018 the original author or authors.
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

package io.spring.initializr.generator.spike;

import java.util.Collections;

import io.spring.initializr.generator.buildsystem.Build;
import io.spring.initializr.generator.buildsystem.maven.MavenBuild;
import io.spring.initializr.generator.spike.build.InitializrMetadataBuildItemResolver;
import io.spring.initializr.generator.spike.build.WarPackagingWebStarterBuildCustomizer;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.test.metadata.InitializrMetadataTestBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WarPackagingWebStarterBuildCustomizer}.
 *
 * @author Stephane Nicoll
 */
public class WarPackagingWebStarterBuildCustomizerTests {

	@Test
	public void addWebStarterWhenNoWebFacetIsPresent() {
		Dependency dependency = Dependency.withId("test", "com.example", "acme", null,
				Dependency.SCOPE_COMPILE);
		InitializrMetadata metadata = InitializrMetadataTestBuilder.withDefaults()
				.addDependencyGroup("test", dependency).build();
		Build build = createBuild(metadata);
		build.dependencies().add("test");
		new WarPackagingWebStarterBuildCustomizer(metadata).customize(build);
		assertThat(build.dependencies().ids()).containsOnly("test", "web", "tomcat");
	}

	@Test
	public void addWebStarterWhenNoWebFacetIsPresentWithCustomWebStarter() {
		Dependency dependency = Dependency.withId("test", "com.example", "acme", null,
				Dependency.SCOPE_COMPILE);
		Dependency web = Dependency.withId("web", "com.example", "custom-web-starter",
				null, Dependency.SCOPE_COMPILE);
		InitializrMetadata metadata = InitializrMetadataTestBuilder.withDefaults()
				.addDependencyGroup("test", dependency, web).build();
		Build build = createBuild(metadata);
		build.dependencies().add("test");
		new WarPackagingWebStarterBuildCustomizer(metadata).customize(build);
		assertThat(build.dependencies().ids()).containsOnly("test", "web", "tomcat");
	}

	@Test
	public void addWebStarterDoesNotReplaceWebFacetDependency() {
		Dependency dependency = Dependency.withId("test", "com.example", "acme", null,
				Dependency.SCOPE_COMPILE);
		dependency.setFacets(Collections.singletonList("web"));
		InitializrMetadata metadata = InitializrMetadataTestBuilder.withDefaults()
				.addDependencyGroup("test", dependency).build();
		Build build = createBuild(metadata);
		build.dependencies().add("test");
		new WarPackagingWebStarterBuildCustomizer(metadata).customize(build);
		assertThat(build.dependencies().ids()).containsOnly("test", "tomcat");
	}

	private Build createBuild(InitializrMetadata metadata) {
		return new MavenBuild(new InitializrMetadataBuildItemResolver(metadata));
	}

}
