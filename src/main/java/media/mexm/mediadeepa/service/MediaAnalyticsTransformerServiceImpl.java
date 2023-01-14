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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ExportTo;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@Service
public class MediaAnalyticsTransformerServiceImpl implements MediaAnalyticsTransformerService {
	private static final Logger log = LogManager.getLogger();

	@Override
	public void exportMediaAnalytics(final String source,
									 final MediaAnalyserResult maResult,
									 final List<Ebur128StrErrFilterEvent> ebur128events,
									 final List<RawStdErrFilterEvent> rawStdErrEvents,
									 final Optional<FFprobeJAXB> oFFprobeResult,
									 final ExportTo exportTo) {
		// TODO Auto-generated method stub

		/*
		 * 				exportTo.getExport();
					exportTo.getFormat();
		*/
		maResult.isEmpty();
	}

	@Override
	public void exportContainerAnalytics(final String name,
										 final ContainerAnalyserResult caResult,
										 final ExportTo exportTo) {
		// TODO Auto-generated method stub
		caResult.isEmpty();
	}

}
