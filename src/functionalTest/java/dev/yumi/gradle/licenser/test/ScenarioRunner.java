/*
 * Copyright 2024 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class ScenarioRunner {
	private static final PathResolver TEST_JAR_PATH;
	private final String name;
	private final Path projectDir;

	public ScenarioRunner(String name, Path projectDir) {
		this.name = name;
		this.projectDir = projectDir;
	}

	private Path getScenarioPath(String path) {
		return TEST_JAR_PATH.resolve("scenarios/" + this.name + path);
	}

	public Path path(String path) {
		return this.projectDir.resolve(path).normalize();
	}

	private Path copy(String pathStr) throws IOException {
		Path destinationPath = this.path(pathStr);
		Files.createDirectories(destinationPath.getParent());
		Files.copy(
				this.getScenarioPath('/' + pathStr),
				destinationPath
		);
		return destinationPath.toAbsolutePath();
	}

	private Path recurseCopy(String pathStr) throws IOException {
		Path destinationPath = this.projectDir.resolve(pathStr).normalize();
		Files.createDirectories(destinationPath);

		var startPath = this.getScenarioPath('/' + pathStr);
		Files.walkFileTree(startPath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				var targetDir = destinationPath.resolve(startPath.relativize(dir));

				try {
					Files.copy(dir, targetDir);
				} catch (FileAlreadyExistsException e) {
					if (!Files.isDirectory(targetDir))
						throw e;
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, destinationPath.resolve(startPath.relativize(file)));
				return FileVisitResult.CONTINUE;
			}
		});

		return destinationPath.toAbsolutePath();
	}

	private void writeString(String path, String string) throws IOException {
		Files.writeString(this.projectDir.resolve(path), string);
	}

	public void setup() throws IOException {
		if (!Files.exists(this.getScenarioPath("/settings.gradle"))) {
			this.writeString("settings.gradle", "");
		}

		Files.copy(ScenarioRunner.class.getResourceAsStream("/HEADER"), this.projectDir.resolve("HEADER"));
		this.copy("build.gradle");
		this.recurseCopy("src");
	}

	public BuildResult run() {
		// Run the build
		var runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("applyLicenses", "--stacktrace");
		runner.withProjectDir(this.projectDir.toFile());
		return runner.build();
	}

	public void runCheck() {
		// Run checkLicenses tasks
		var runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("checkLicenses", "--stacktrace");
		runner.withProjectDir(this.projectDir.toFile());
		runner.build();
	}

	interface PathResolver {
		Path resolve(String p);
	}

	static {
		try {
			URL headerUrl = Objects.requireNonNull(ScenarioRunner.class.getResource("/HEADER"));

			if (headerUrl.getProtocol().equals("file")) {
				var path = Path.of(headerUrl.toURI()).getParent();
				TEST_JAR_PATH = path::resolve;
			} else if (headerUrl.getProtocol().equals("jar")) {
				final JarURLConnection connection =
						(JarURLConnection) headerUrl.openConnection();
				final URL url = connection.getJarFileURL();
				var fs = FileSystems.newFileSystem(Path.of(url.toURI()));
				TEST_JAR_PATH = fs::getPath;
			} else {
				throw new IllegalStateException("Invalid resource.");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
