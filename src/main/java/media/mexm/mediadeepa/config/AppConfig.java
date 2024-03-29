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

	private ZippedArchive zippedArchive = new ZippedArchive();

	@Data
	public class ZippedArchive {
		private String summaryTxt = "summary.txt";
		private String sourceNameTxt = "sourcename.txt";
		private String versionJson = "version.json";
		private String filtersJson = "filters.json";
		private String commandLineJson = "commandline.json";
		private String containerXml = "container.xml";
		private String stdErrTxt = "stderr.txt";
		private String lavfiTxtBase = "lavfi";
		private String ffprobeTxt = "ffprobe.xml";
	}

	private String jsontableFileName = "media-datas.json";
	private String sqllitetableFileName = "media-datas.sqlite";
	private String xslxtableFileName = "media-datas.xlsx";
	private String xmltableFileName = "media-datas.xml";
	private String ffprobexmlFileName = "ffprobe.xml";

	private ReportConfig reportConfig = new ReportConfig();

	@Data
	public class ReportConfig {
		private String htmlFilename = "report.html";
		private int maxCropEventsDisplay = 20;
	}

	private GraphicConfig graphicConfig = new GraphicConfig();

	@Data
	public class GraphicConfig {
		private String lufsGraphicFilename = "audio-loudness.png";
		private String lufsTPKGraphicFilename = "audio-loundness-truepeak.png";
		private String aPhaseGraphicFilename = "audio-phase.png";
		private String dcOffsetGraphicFilename = "audio-dcoffset.png";
		private String entropyGraphicFilename = "audio-entropy.png";
		private String flatnessGraphicFilename = "audio-flatness.png";
		private String noiseFloorGraphicFilename = "audio-noise-floor.png";
		private String peakLevelGraphicFilename = "audio-peak-level.png";
		private String sitiGraphicFilename = "video-siti.png";
		private String blockGraphicFilename = "video-block.png";
		private String blurGraphicFilename = "video-blur.png";
		private String itetGraphicFilename = "video-idet.png";
		private String cropGraphicFilename = "video-crop.png";
		private String eventsGraphicFilename = "events.png";
		private String vBitrateGraphicFilename = "video-bitrate.png";
		private String aBitrateGraphicFilename = "audio-bitrate.png";
		private String vFrameDurationGraphicFilename = "video-frame-duration.png";
		private String gopCountGraphicFilename = "video-gop-count.png";
		private String gopSizeGraphicFilename = "video-gop-size.png";

		private Dimension imageSizeFullSize = new Dimension(2000, 1200);
		private Dimension imageSizeHalfSize = new Dimension(2000, 600);
		private float jpegCompressionRatio = 0.95f;
	}

	private String logtofilePattern = "%d{ISO8601} %-5level %msg%n";
	private String ffmpegExecName = "ffmpeg";
	private String ffprobeExecName = "ffprobe";

}
