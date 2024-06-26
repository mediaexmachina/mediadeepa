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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa;

import static java.util.function.Predicate.not;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import media.mexm.mediadeepa.cli.OutputCmd;
import media.mexm.mediadeepa.cli.SingleExportCmd;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public record ExportOnlyParamConfiguration(String internalFileName, String outputDest) {

	public static Optional<ExportOnlyParamConfiguration> fromOutputCmd(final OutputCmd outputCmd,
																	   final CommandLine commandLine) {
		final var oSingleExportParam = Optional.ofNullable(outputCmd.getSingleExportCmd())
				.map(SingleExportCmd::getSingleExport)
				.flatMap(Optional::ofNullable)
				.filter(not(String::isBlank));
		if (oSingleExportParam.isPresent()) {
			final var exportOnlyParams = StringUtils.split(oSingleExportParam.get(), ":", 2);
			if (exportOnlyParams.length == 1) {
				throw new ParameterException(commandLine,
						"Can't manage singleExport param: missing path separator \":\"");
			}
			return Optional.ofNullable(new ExportOnlyParamConfiguration(exportOnlyParams[0], exportOnlyParams[1]));
		}
		return Optional.empty();
	}

	public boolean isOutToStdOut() {
		return outputDest.equals("-");
	}
}
