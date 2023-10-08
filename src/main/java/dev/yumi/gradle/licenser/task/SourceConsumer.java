/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task;

import dev.yumi.gradle.licenser.api.comment.HeaderComment;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a source consumer to execute an action on project files.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
interface SourceConsumer {
	/**
	 * Called with a project file to process.
	 *
	 * @param project the project
	 * @param logger the logger
	 * @param sourceSetPath the path to the processed source set
	 * @param headerComment the header comment type used for this file type
	 * @param path the path to the file to process
	 * @throws IOException if read or writing the file fails
	 */
	void consume(Project project, Logger logger, Path sourceSetPath, HeaderComment headerComment, Path path) throws IOException;

	/**
	 * Called when the task has finished processing all matching files.
	 *
	 * @param logger the logger
	 */
	void end(Logger logger);
}
