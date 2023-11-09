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
package media.mexm.mediadeepa.cli;

import lombok.Data;
import picocli.CommandLine.Option;

@Data
public class FilterCmd {

	@Option(names = { "--filter-ebur128-target" }, paramLabel = "DBFS")
	private Integer ebur128Target;

	@Option(names = { "--filter-freeze-noisetolerance" }, paramLabel = "DB")
	private Integer freezedetectNoiseTolerance;
	@Option(names = { "--filter-freeze-duration" }, paramLabel = "SECONDS")
	private Integer freezedetectDuration;

	@Option(names = { "--filter-idet-intl" }, paramLabel = "THRESHOLD_FLOAT")
	private Float idetIntlThres;
	@Option(names = { "--filter-idet-prog" }, paramLabel = "THRESHOLD_FLOAT")
	private Float idetProgThres;
	@Option(names = { "--filter-idet-rep" }, paramLabel = "THRESHOLD_FLOAT")
	private Float idetRepThres;
	@Option(names = { "--filter-idet-hl" }, paramLabel = "FRAMES")
	private Integer idetHalfLife;

	@Option(names = { "--filter-crop-limit" }, paramLabel = "INT")
	private Integer cropLimit;
	@Option(names = { "--filter-crop-round" }, paramLabel = "INT")
	private Integer cropRound;
	@Option(names = { "--filter-crop-skip" }, paramLabel = "FRAMES")
	private Integer cropSkip;
	@Option(names = { "--filter-crop-reset" }, paramLabel = "FRAMES")
	private Integer cropReset;
	@Option(names = { "--filter-crop-low" }, paramLabel = "INT")
	private Integer cropLow;
	@Option(names = { "--filter-crop-high" }, paramLabel = "INT")
	private Integer cropHigh;

	@Option(names = { "--filter-blur-low" }, paramLabel = "THRESHOLD_FLOAT")
	private Float blurLow;
	@Option(names = { "--filter-blur-high" }, paramLabel = "THRESHOLD_FLOAT")
	private Float blurHigh;
	@Option(names = { "--filter-blur-radius" }, paramLabel = "PIXELS")
	private Integer blurRadius;
	@Option(names = { "--filter-blur-block-pct" }, paramLabel = "PERCENT")
	private Float blurBlockPct;
	@Option(names = { "--filter-blur-block-width" }, paramLabel = "PIXELS")
	private Float blurBlockWidth;
	@Option(names = { "--filter-blur-block-height" }, paramLabel = "PIXELS")
	private Float blurBlockHeight;
	@Option(names = { "--filter-blur-planes" }, paramLabel = "INDEX")
	private Integer blurPlanes;

	@Option(names = { "--filter-block-period-min" }, paramLabel = "INT")
	private Integer blockPeriodMin;
	@Option(names = { "--filter-block-period-max" }, paramLabel = "INT")
	private Integer blockPeriodMax;
	@Option(names = { "--filter-block-planes" }, paramLabel = "INDEX")
	private Integer blockPlanes;

	@Option(names = { "--filter-black-duration" }, paramLabel = "MILLISECONDS")
	private Integer blackMinDuration;
	@Option(names = { "--filter-black-ratio-th" }, paramLabel = "THRESHOLD_FLOAT")
	private Float blackPictureBlackRatioTh;
	@Option(names = { "--filter-black-th" }, paramLabel = "THRESHOLD_FLOAT")
	private Float blackPixelBlackTh;

	@Option(names = { "--filter-aphase-tolerance" }, paramLabel = "RATIO")
	private Float aPhaseTolerance;
	@Option(names = { "--filter-aphase-angle" }, paramLabel = "DEGREES")
	private Integer aPhaseAngle;
	@Option(names = { "--filter-aphase-duration" }, paramLabel = "MILLISECONDS")
	private Integer aPhaseDuration;

	@Option(names = { "--filter-silence-noise" }, paramLabel = "DBFS")
	private Integer silenceNoise;
	@Option(names = { "--filter-silence-duration" }, paramLabel = "SECONDS")
	private Integer silenceDuration;

}
