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
package media.mexm.mediadeepa.components;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.exportformat.components.FFProbeXMLExportFormat;
import media.mexm.mediadeepa.rendererengine.ReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.AConstsRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.AFramesRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.APhaseMeterRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.AStatsRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.AboutMeasureRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.BlockRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.BlurRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.CropRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.Ebur128RendererEngine;
import media.mexm.mediadeepa.rendererengine.components.Ebur128SummaryRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.EventsRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.FramesDurationRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.GopStatsRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.IdetRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.MediaSummaryRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.PacketsRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.RawstderrfiltersRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.SITIRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.SITIReportRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.VConstsRendererEngine;
import media.mexm.mediadeepa.rendererengine.components.VFramesRendererEngine;

@Component
@Slf4j
public class RendererEngineComparator implements Comparator<ReportRendererEngine> {

	private static final List<Class<?>> ENGINE_DISPLAY_ORDER = List.of(
			MediaSummaryRendererEngine.class,
			FFProbeXMLExportFormat.class,

			Ebur128SummaryRendererEngine.class,
			Ebur128RendererEngine.class,

			EventsRendererEngine.class,

			APhaseMeterRendererEngine.class,
			AStatsRendererEngine.class,

			BlockRendererEngine.class,
			BlurRendererEngine.class,
			CropRendererEngine.class,
			IdetRendererEngine.class,

			SITIRendererEngine.class,
			SITIReportRendererEngine.class,

			FramesDurationRendererEngine.class,

			AConstsRendererEngine.class,
			VConstsRendererEngine.class,
			PacketsRendererEngine.class,
			AFramesRendererEngine.class,
			GopStatsRendererEngine.class,
			VFramesRendererEngine.class,

			RawstderrfiltersRendererEngine.class,

			AboutMeasureRendererEngine.class);

	@Override
	public int compare(final ReportRendererEngine l, final ReportRendererEngine r) {
		var lPos = ENGINE_DISPLAY_ORDER.indexOf(l.getClass());
		if (lPos == -1) {
			lPos = ENGINE_DISPLAY_ORDER.size();
			log.warn("You should add new renderer engine to {}: {}", getClass().getSimpleName(), l.getClass());
		}
		var rPos = ENGINE_DISPLAY_ORDER.indexOf(r.getClass());
		if (rPos == -1) {
			rPos = ENGINE_DISPLAY_ORDER.size();
			log.warn("You should add new renderer engine to {}: {}", getClass().getSimpleName(), r.getClass());
		}
		return Integer.compare(lPos, rPos);
	}

}
