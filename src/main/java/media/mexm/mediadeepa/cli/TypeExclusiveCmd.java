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
public class TypeExclusiveCmd {
	@Option(names = { "-an", "--audio-no" },
			description = "Ignore all video filters",
			required = false)
	private boolean audioNo;

	@Option(names = { "-vn", "--video-no" },
			description = "Ignore all audio filters",
			required = false)
	private boolean videoNo;
}
