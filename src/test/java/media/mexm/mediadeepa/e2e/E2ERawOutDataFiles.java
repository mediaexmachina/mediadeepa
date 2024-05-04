/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.e2e;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

public record E2ERawOutDataFiles(File mediaFile,
								 boolean hasVideo,
								 File archive) {

	String getExtension() {
		return FilenameUtils.getExtension(mediaFile().getName());
	}

	E2ESpecificMediaFile getSpecificMediaFile() {
		return E2ESpecificMediaFile.getFromMediaFile(mediaFile());
	}

	boolean allOutExists() {
		return archive.exists();
	}

	static E2ERawOutDataFiles create(final File mediaFile) {
		final var baseDirName = "target/e2e/" + mediaFile.getName().replace(".", "-");
		return new E2ERawOutDataFiles(
				mediaFile,
				FilenameUtils.getExtension(mediaFile.getName()).equals("wav") == false,
				new File(baseDirName + ".zip"));
	}
}
