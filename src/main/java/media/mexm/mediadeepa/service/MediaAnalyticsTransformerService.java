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
package media.mexm.mediadeepa.service;

import java.util.List;
import java.util.Optional;

import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExportTo;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public interface MediaAnalyticsTransformerService {

	void exportMediaAnalytics(String source,
							  MediaAnalyserResult maResult,
							  List<Ebur128StrErrFilterEvent> ebur128events,
							  List<RawStdErrFilterEvent> rawStdErrEvents,
							  Optional<FFprobeJAXB> oFFprobeResult,
							  ExportTo exportTo);

	void exportContainerAnalytics(String source,
								  ContainerAnalyserResult caResult,
								  ExportTo exportTo);

}
