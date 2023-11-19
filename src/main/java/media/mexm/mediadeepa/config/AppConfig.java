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
package media.mexm.mediadeepa.config;

import java.awt.Dimension;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "mediadeepa")
@Data
public class AppConfig {

	private String summaryZippedTxtFilename = "summary.txt";
	private String sourceNameZippedTxtFilename = "sourcename.txt";
	private String versionZippedJsonFilename = "version.json";
	private String filtersZippedJsonFilename = "filters.json";
	private String containerZippedXmlFilename = "container.xml";
	private String stdErrZippedTxtFilename = "stderr.txt";
	private String lavfiZippedTxtBaseFilename = "lavfi";
	private String ffprobeZippedTxtFilename = "ffprobe.xml";

	private String reportHtmlFileName = "report.html";
	private String jsonTableFileName = "media-datas.json";
	private String sqlLiteTableFileName = "media-datas.sqlite";
	private String xslxTableFileName = "media-datas.xlsx";
	private String xmlTableFileName = "media-datas.xml";
	private String ffprobeXMLFileName = "ffprobe.xml";

	private String lufsGraphicFilename = "audio-loudness.jpg";
	private String lufsTPKGraphicFilename = "audio-loundness-truepeak.jpg";
	private String aPhaseGraphicFilename = "audio-phase.jpg";
	private String dcOffsetGraphicFilename = "audio-dcoffset.jpg";
	private String entropyGraphicFilename = "audio-entropy.jpg";
	private String flatnessGraphicFilename = "audio-flatness.jpg";
	private String noiseFloorGraphicFilename = "audio-noise-floor.jpg";
	private String peakLevelGraphicFilename = "audio-peak-level.jpg";
	private String sitiGraphicFilename = "video-siti.jpg";
	private String blockGraphicFilename = "video-block.jpg";
	private String blurGraphicFilename = "video-blur.jpg";
	private String itetGraphicFilename = "video-idet.jpg";
	private String cropGraphicFilename = "video-crop.jpg";
	private String eventsGraphicFilename = "events.jpg";
	private String vBitrateGraphicFilename = "video-bitrate.jpg";
	private String aBitrateGraphicFilename = "audio-bitrate.jpg";
	private String vFrameDurationGraphicFilename = "video-frame-duration.jpg";
	private String gopCountGraphicFilename = "video-gop-count.jpg";
	private String gopSizeGraphicFilename = "video-gop-size.jpg";

	private Dimension imageSizeFullSize = new Dimension(2000, 1200);
	private Dimension imageSizeHalfSize = new Dimension(2000, 600);
	private float jpegCompressionRatio = 0.95f;

}
