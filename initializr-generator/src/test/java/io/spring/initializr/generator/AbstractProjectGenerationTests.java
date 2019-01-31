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

package io.spring.initializr.generator;

import java.nio.file.Path;
import java.util.function.Consumer;

import io.spring.initializr.generator.buildsystem.BuildItemResolver;
import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.buildsystem.gradle.GradleBuildSystem;
import io.spring.initializr.generator.buildsystem.maven.MavenBuildSystem;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.io.SimpleIndentStrategy;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.ProjectGenerationContext;
import io.spring.initializr.generator.spike.build.InitializrMetadataBuildItemResolver;
import io.spring.initializr.generator.test.project.ProjectGeneratorTester;
import io.spring.initializr.generator.test.project.ProjectStructure;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.test.generator.ProjectAssert;
import io.spring.initializr.test.metadata.InitializrMetadataTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

/**
 * Abstract base class for testing project generation.
 *
 * @author Madhura Bhave
 */
@ExtendWith(TempDirectory.class)
public abstract class AbstractProjectGenerationTests {

	private Path tempDir;

	protected static final Dependency WEB = Dependency.withId("web",
			"org.springframework.boot", "spring-boot-starter-web");

	@BeforeEach
	void setup(@TempDirectory.TempDir Path dir) {
		this.tempDir = dir;
	}

	protected ProjectAssert generateProject(String language, String build,
			String version) {
		return generateProject(language, build, version, (description) -> {
		});
	}

	protected ProjectAssert generateProject(String language, String build, String version,
			Consumer<ProjectDescription> descriptionCustomizer) {
		InitializrMetadata metadata = InitializrMetadataTestBuilder.withDefaults()
				.addDependencyGroup("web", WEB).build();
		return generateProject(language, build, version, descriptionCustomizer, metadata);
	}

	protected ProjectAssert generateProject(String language, String build, String version,
			Consumer<ProjectDescription> descriptionCustomizer,
			InitializrMetadata metadata) {
		ProjectGeneratorTester projectTester = new ProjectGeneratorTester()
				.withDirectory(this.tempDir).withDefaultContextInitializer()
				.withDescriptionCustomizer((description) -> setupProjectDescription(
						language, version, build, description))
				.withDescriptionCustomizer(descriptionCustomizer).withContextInitializer(
						(context) -> setupProjectGenerationContext(metadata, context));
		ProjectStructure projectStructure = projectTester
				.generate(new ProjectDescription());
		Path resolve = projectStructure.resolve("");
		return new ProjectAssert(resolve.toFile());
	}

	private void setupProjectGenerationContext(InitializrMetadata metadata,
			ProjectGenerationContext context) {
		context.registerBean(InitializrMetadata.class, () -> metadata);
		context.registerBean(BuildItemResolver.class,
				() -> new InitializrMetadataBuildItemResolver(metadata));
		context.registerBean(IndentingWriterFactory.class,
				() -> IndentingWriterFactory.create(new SimpleIndentStrategy("\t")));
	}

	private void setupProjectDescription(String language, String version, String build,
			ProjectDescription description) {
		BuildSystem buildSystem = (build.startsWith("gradle")) ? new GradleBuildSystem()
				: new MavenBuildSystem();
		description.setLanguage(Language.forId(language, Language.DEFAULT_JVM_VERSION));
		description.setBuildSystem(buildSystem);
		description.setPlatformVersion(Version.parse(version));
		description.setName("demo");
		description.setDescription("Demo project for Spring Boot");
	}

}
