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

import java.io.File;
import java.util.Set;

import lombok.Data;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@Data
public class ProcessFileCmd {
	@Option(names = { "-i", "--input" },
			description = "Input (media) file to process",
			paramLabel = "FILE")
	private File input;

	@Option(names = { "-c", "--container" },
			description = "Do a container analysing (ffprobe streams)")
	private boolean containerAnalysing;

	@Option(names = { "-t" },
			description = { "Duration of input file to proces it",
							"See https://ffmpeg.org/ffmpeg-utils.html#time-duration-syntax" },
			paramLabel = "DURATION")
	private String duration;

	@Option(names = { "-ss" },
			description = { "Seek time in input file before to proces it",
							"See https://ffmpeg.org/ffmpeg-utils.html#time-duration-syntax" },
			paramLabel = "DURATION")
	private String startTime;

	@Option(names = { "-max" },
			description = { "Max time let to process a file" },
			paramLabel = "SECONDS")
	private int maxSec;

	@Option(names = { "-fo", "--filter-only" },
			description = { "Allow only this filter(s) to process (-o to get list)" },
			paramLabel = "FILTER")
	private Set<String> filtersOnly;

	@Option(names = { "-fn", "--filter-no" },
			description = { "Not use this filter(s) to process (-o to get list)" },
			paramLabel = "FILTER")
	private Set<String> filtersIgnore;

	@ArgGroup(exclusive = true, heading = "Media type exclusive")
	private TypeExclusiveCmd typeExclusiveCmd;

	@Option(names = { "-mn", "--media-no" },
			description = "Disable media analysing (ffmpeg)")
	private boolean noMediaAnalysing;

	@ArgGroup(exclusive = false, heading = "Internal filters parameters")
	private FilterCmd filterCmd;

}
