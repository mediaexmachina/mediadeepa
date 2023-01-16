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
package media.mexm.mediadeepa.exportformat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import media.mexm.mediadeepa.ExportFormat;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

public class TabularTextExportFormat implements ExportFormat {
	private static final Logger log = LogManager.getLogger();

	public TabularTextExportFormat() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void exportMediaAnalyserResult(final String source,
										  final MediaAnalyserResult maResult,
										  final File exportDirectory) {
		// TODO Auto-generated method stub
		maResult.ebur128Summary();

		maResult.lavfiMetadatas().getAPhaseMeterReport();
		maResult.lavfiMetadatas().getAStatsReport();
		maResult.lavfiMetadatas().getBlockDetectReport();
		maResult.lavfiMetadatas().getBlurDetectReport();
		maResult.lavfiMetadatas().getCropDetectReport();
		maResult.lavfiMetadatas().getIdetReport();
		maResult.lavfiMetadatas().getSitiReport();
		maResult.lavfiMetadatas().computeSitiStats();
		maResult.lavfiMetadatas().getReportCount();

		maResult.lavfiMetadatas().getEventCount();
		maResult.lavfiMetadatas().getBlackEvents();
		maResult.lavfiMetadatas().getFreezeEvents();
		maResult.lavfiMetadatas().getMonoEvents();
		maResult.lavfiMetadatas().getSilenceEvents();

		maResult.session().getAudioFilters();
		maResult.session().getVideoFilters();
	}

	@Override
	public void exportEbur128StrErrFilterEvent(final String source,
											   final List<Ebur128StrErrFilterEvent> ebur128events,
											   final File exportDirectory) {
		final var lines = Stream.concat(
				Stream.of(
						"Integrated\tMomentary\tShort-term\tLoudness Range\tSample-peak\t\tTrue-peak per frame\t\tTrue Peak\t"),
				ebur128events.stream()
						.map(ebu -> ebu.getI()
									+ "\t" + ebu.getM()
									+ "\t" + ebu.getS()
									+ "\t" + ebu.getLra()
									+ "\t" + ebu.getSpk().left()
									+ "\t" + ebu.getSpk().right()
									+ "\t" + ebu.getFtpk().left()
									+ "\t" + ebu.getFtpk().right()
									+ "\t" + ebu.getTpk().left()
									+ "\t" + ebu.getTpk().right()))
				.collect(joining("\r\n"));
		save("ebur128.txt", exportDirectory, lines);
	}

	@Override
	public void exportRawStdErrFilterEvent(final String source,
										   final List<RawStdErrFilterEvent> rawStdErrEvents,
										   final File exportDirectory) {
		final var lines = Stream.concat(
				Stream.of("Filter name\tChain pos\tLine"),
				rawStdErrEvents.stream()
						.map(r -> r.getFilterName()
								  + "\t" + r.getFilterChainPos()
								  + "\t" + r.getLineValue()))
				.collect(joining("\r\n"));
		save("rawstderrfilters.txt", exportDirectory, lines);
	}

	@Override
	public void exportFFprobeJAXB(final String source,
								  final FFprobeJAXB ffprobeResult,
								  final File exportDirectory) {
		try {
			FileUtils.write(
					new File(exportDirectory, "ffprobe.xml"),
					ffprobeResult.getXmlContent(),
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

	private void save(final String fileName, final File exportDirectory, final String lines) {
		try {
			FileUtils.write(
					new File(exportDirectory, fileName),
					lines,
					UTF_8,
					false);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write file", e);
		}
	}

}
