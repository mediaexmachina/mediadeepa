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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import java.io.File;
import java.util.List;

import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public interface ExportFormat {

	void exportMediaAnalyserResult(String source,
								   MediaAnalyserResult maResult,
								   File exportDirectory);

	void exportEbur128StrErrFilterEvent(String source,
										List<Ebur128StrErrFilterEvent> ebur128events,
										File exportDirectory);

	void exportRawStdErrFilterEvent(String source,
									List<RawStdErrFilterEvent> rawStdErrEvents,
									File exportDirectory);

	void exportFFprobeJAXB(String source,
						   FFprobeJAXB ffprobeResult,
						   File exportDirectory);
	// TODO add container

}
