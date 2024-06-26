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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mediadeepa.cli.OutputCmd;
import media.mexm.mediadeepa.cli.SingleExportCmd;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class ExportOnlyParamConfigurationTest {

	@Fake
	String internalFileName;
	@Fake
	String outputDest;

	@Mock
	OutputCmd outputCmd;
	@Mock
	CommandLine commandLine;
	@Mock
	SingleExportCmd singleExportCmd;

	@Test
	void testFromOutputCmd_noSingleExport() {
		final var result = ExportOnlyParamConfiguration.fromOutputCmd(outputCmd, commandLine);
		assertThat(result).isEmpty();
		verify(outputCmd, times(1)).getSingleExportCmd();
	}

	@Test
	void testFromOutputCmd_withSingleExport() {
		when(outputCmd.getSingleExportCmd()).thenReturn(singleExportCmd);
		when(singleExportCmd.getSingleExport()).thenReturn(internalFileName + ":" + outputDest);

		final var result = ExportOnlyParamConfiguration.fromOutputCmd(outputCmd, commandLine);
		assertThat(result).isNotEmpty();
		assertEquals(internalFileName, result.get().internalFileName());
		assertEquals(outputDest, result.get().outputDest());

		verify(outputCmd, times(1)).getSingleExportCmd();
		verify(singleExportCmd, times(1)).getSingleExport();
	}

	@Test
	void testFromOutputCmd_missingSinglExport() {
		when(outputCmd.getSingleExportCmd()).thenReturn(singleExportCmd);
		when(singleExportCmd.getSingleExport()).thenReturn(internalFileName);

		assertThrows(ParameterException.class,
				() -> ExportOnlyParamConfiguration.fromOutputCmd(outputCmd, commandLine));

		verify(outputCmd, times(1)).getSingleExportCmd();
		verify(singleExportCmd, times(1)).getSingleExport();
	}

	@Test
	void testIsOutToStdOut() {
		assertFalse(new ExportOnlyParamConfiguration(internalFileName, outputDest).isOutToStdOut());
		assertTrue(new ExportOnlyParamConfiguration(internalFileName, "-").isOutToStdOut());
	}

}
