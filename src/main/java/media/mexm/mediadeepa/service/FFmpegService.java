/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.service;

import java.io.File;
import java.util.Map;

import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserSession;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSession;

public interface FFmpegService {

	Map<String, String> getMtdFiltersAvaliable();

	Map<String, String> getVersions();

	MediaAnalyserSession createMediaAnalyserSession(ProcessFile processFile, File lavfiSecondaryVideoFile);

	ContainerAnalyserSession createContainerAnalyserSession(ProcessFile processFile);

	@Deprecated
	MediaAnalyserResult doExtractMtd(File source, ProgressCLI progressCLI, boolean audioNo, boolean videoNo);

}
