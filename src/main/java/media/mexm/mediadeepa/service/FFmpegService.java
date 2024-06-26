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
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import media.mexm.mediadeepa.cli.FilterCmd;
import media.mexm.mediadeepa.cli.ProcessFileCmd;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserExtractResult;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserExtractResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.wavmeasure.MeasuredWav;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public interface FFmpegService {

	Map<String, String> getMtdFiltersAvaliable();

	Map<String, String> getVersions();

	MediaAnalyserExtractResult extractMedia(File inputFile,
											ProcessFileCmd processFileCmd,
											File lavfiSecondaryVideoFile,
											FFprobeJAXB ffprobeJAXB,
											FilterCmd options);

	MediaAnalyserProcessResult processMedia(File inputFile,
											ProcessFileCmd processFileCmd,
											File lavfiSecondaryVideoFile,
											FFprobeJAXB ffprobeJAXB,
											FilterCmd options);

	ContainerAnalyserExtractResult extractContainer(File inputFile,
													ProcessFileCmd processFileCmd,
													Duration programDuration);

	ContainerAnalyserProcessResult processContainer(File inputFile,
													ProcessFileCmd processFileCmd,
													Duration programDuration);

	FFprobeJAXB getFFprobeJAXBFromFileToProcess(File inputFile, ProcessFileCmd processFileCmd);

	Optional<MeasuredWav> measureWav(File inputFile, FFprobeJAXB ffprobeJAXB, ProcessFileCmd processFileCmd);

}
