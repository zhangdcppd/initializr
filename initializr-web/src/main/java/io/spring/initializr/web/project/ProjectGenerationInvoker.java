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

package io.spring.initializr.web.project;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.spring.initializr.InitializrException;
import io.spring.initializr.generator.ProjectFailedEvent;
import io.spring.initializr.generator.ProjectGeneratedEvent;
import io.spring.initializr.generator.ProjectRequest;
import io.spring.initializr.generator.buildsystem.Build;
import io.spring.initializr.generator.buildsystem.BuildItemResolver;
import io.spring.initializr.generator.buildsystem.gradle.GradleBuild;
import io.spring.initializr.generator.buildsystem.gradle.GradleBuildSystem;
import io.spring.initializr.generator.buildsystem.maven.MavenBuild;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.project.ProjectGenerationContext;
import io.spring.initializr.generator.project.ProjectGenerator;
import io.spring.initializr.generator.project.ResolvedProjectDescription;
import io.spring.initializr.generator.spike.ConceptTranslator;
import io.spring.initializr.generator.spike.build.InitializrMetadataBuildItemResolver;
import io.spring.initializr.generator.spring.build.BuildCustomizer;
import io.spring.initializr.generator.spring.build.gradle.GradleBuildProjectContributor;
import io.spring.initializr.generator.spring.build.maven.MavenBuildProjectContributor;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataProvider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.FileSystemUtils;

/**
 * Invokes the project generation API. This is an intermediate layer that can consume a
 * {@link ProjectRequest} and trigger project generation based on the request.
 *
 * @author Madhura Bhave
 */
public class ProjectGenerationInvoker {

	private final ApplicationContext parentApplicationContext;

	private final ApplicationEventPublisher eventPublisher;

	private final ProjectRequestToDescriptionConverter converter;

	private transient Map<String, List<File>> temporaryFiles = new LinkedHashMap<>();

	public ProjectGenerationInvoker(ApplicationContext parentApplicationContext,
			ApplicationEventPublisher eventPublisher,
			ProjectRequestToDescriptionConverter converter) {
		this.parentApplicationContext = parentApplicationContext;
		this.eventPublisher = eventPublisher;
		this.converter = converter;
	}

	/**
	 * Invokes the project generation API that generates the entire project structure for
	 * the specified {@link ProjectRequest}. Returns a directory containing the project.
	 * @param request the project request
	 * @return the generated project structure
	 */
	public File invokeProjectStructureGeneration(ProjectRequest request) {
		InitializrMetadata metadata = this.parentApplicationContext
				.getBean(InitializrMetadataProvider.class).get();
		ProjectDescription projectDescription = this.converter.convert(request, metadata);
		ProjectGenerator projectGenerator = new ProjectGenerator(
				(projectGenerationContext) -> customizeProjectGenerationContext(
						projectGenerationContext, request, metadata));
		try {
			Path path = projectGenerator.generate(projectDescription);
			File file = path.toFile();
			String name = file.getName();
			addTempFile(name, file);
			publishProjectGeneratedEvent(request);
			return file;
		}
		catch (IOException ex) {
			publishProjectFailedEvent(request, ex);
			throw new IllegalStateException(ex); // TODO
		}
		catch (InitializrException ex) {
			publishProjectFailedEvent(request, ex);
			throw ex;
		}
	}

	/**
	 * Invokes the project generation API that knows how to just write the build file.
	 * Returns a directory containing the project for the specified
	 * {@link ProjectRequest}.
	 * @param request the project request
	 * @return the generated build content
	 */
	public byte[] invokeBuildGeneration(ProjectRequest request) {
		InitializrMetadata metadata = this.parentApplicationContext
				.getBean(InitializrMetadataProvider.class).get();
		try {
			ProjectDescription projectDescription = this.converter.convert(request,
					metadata);
			ProjectGenerator projectGenerator = new ProjectGenerator(
					(projectGenerationContext) -> customizeProjectGenerationContext(
							projectGenerationContext, request, metadata));
			byte[] bytes = projectGenerator.generate(projectDescription,
					this::generateBuild);
			publishProjectGeneratedEvent(request);
			return bytes;
		}
		catch (IOException ex) {
			publishProjectFailedEvent(request, ex);
			throw new IllegalStateException(ex); // TODO
		}
		catch (InitializrException ex) {
			publishProjectFailedEvent(request, ex);
			throw ex;
		}
	}

	/**
	 * Create a file in the same directory as the given directory using the directory name
	 * and extension.
	 * @param dir the directory used to determine the path and name of the new file
	 * @param extension the extension to use for the new file
	 * @return the newly created file
	 */
	public File createDistributionFile(File dir, String extension) {
		File download = new File(dir.getParent(), dir.getName() + extension);
		addTempFile(dir.getName(), download);
		return download;
	}

	private void addTempFile(String group, File file) {
		this.temporaryFiles.computeIfAbsent(group, (key) -> new ArrayList<>()).add(file);
	}

	/**
	 * Clean all the temporary files that are related to this root directory.
	 * @param dir the directory to clean
	 * @see #createDistributionFile
	 */
	public void cleanTempFiles(File dir) {
		List<File> tempFiles = this.temporaryFiles.remove(dir.getName());
		if (!tempFiles.isEmpty()) {
			tempFiles.forEach((File file) -> {
				if (file.isDirectory()) {
					FileSystemUtils.deleteRecursively(file);
				}
				else if (file.exists()) {
					file.delete();
				}
			});
		}
	}

	private byte[] generateBuild(ProjectGenerationContext context) throws IOException {
		ResolvedProjectDescription projectDescription = context
				.getBean(ResolvedProjectDescription.class);
		StringWriter out = new StringWriter();
		if (projectDescription.getBuildSystem() instanceof GradleBuildSystem) {
			GradleBuildProjectContributor buildContributor = context
					.getBean(GradleBuildProjectContributor.class);
			buildContributor.writeBuild(out);
		}
		else {
			// Assuming Maven
			MavenBuildProjectContributor buildContributor = context
					.getBean(MavenBuildProjectContributor.class);
			buildContributor.writeBuild(out);
		}
		return out.toString().getBytes();
	}

	private void customizeProjectGenerationContext(
			AnnotationConfigApplicationContext context, ProjectRequest request,
			InitializrMetadata metadata) {
		context.setParent(this.parentApplicationContext);
		context.registerBean(InitializrMetadata.class, () -> metadata);
		context.registerBean(BuildItemResolver.class,
				() -> new InitializrMetadataBuildItemResolver(metadata));
		context.registerBean("temporaryBuildCustomizer", BuildCustomizer.class,
				() -> buildCustomizer(request));
	}

	private void publishProjectGeneratedEvent(ProjectRequest request) {
		ProjectGeneratedEvent event = new ProjectGeneratedEvent(request);
		this.eventPublisher.publishEvent(event);
	}

	private void publishProjectFailedEvent(ProjectRequest request, Exception cause) {
		ProjectFailedEvent event = new ProjectFailedEvent(request, cause);
		this.eventPublisher.publishEvent(event);
	}

	private BuildCustomizer<Build> buildCustomizer(ProjectRequest request) {
		return (build) -> {
			request.getBuildProperties().getVersions()
					.forEach((versionProperty, valueSupplier) -> {
						build.addVersionProperty(
								ConceptTranslator.toVersionProperty(versionProperty),
								valueSupplier.get());
					});
			if (build instanceof MavenBuild) {
				request.getBuildProperties().getMaven()
						.forEach((key, valueSupplier) -> ((MavenBuild) build)
								.setProperty(key, valueSupplier.get()));
			}
			if (build instanceof GradleBuild) {
				request.getBuildProperties().getGradle()
						.forEach((key, valueSupplier) -> ((GradleBuild) build).ext(key,
								valueSupplier.get()));
			}
		};
	}

}
