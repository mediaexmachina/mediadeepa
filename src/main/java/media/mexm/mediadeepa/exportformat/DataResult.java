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
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.exportformat;

import static java.util.Objects.requireNonNull;

import java.awt.Dimension;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import media.mexm.mediadeepa.exportformat.report.ReportDocument;
import media.mexm.mediadeepa.exportformat.report.SimpleKeyValueReportEntry;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.filtering.lavfimtd.LavfiMtdEvent;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.wavmeasure.MeasuredWav;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.data.FFProbeFormat;

@EqualsAndHashCode
public class DataResult {

	@Getter
	private final String source;
	@Getter
	private final boolean inMultipleSourcesSet;

	private MediaAnalyserProcessResult mediaAnalyserResult;
	@Setter
	private Duration sourceDuration;
	private FFprobeJAXB ffprobeResult;
	private ContainerAnalyserProcessResult containerAnalyserResult;
	@Getter
	private final Map<String, String> versions;
	@Setter
	private String ffmpegCommandLine;
	@Setter
	private String ffprobeCommandLine;
	@Setter
	private MeasuredWav wavForm;
	@Setter
	private VideoImageSnapshots videoImageSnapshots;

	public DataResult(final String source, final Map<String, String> versions, final boolean inMultipleSourcesSet) {
		this.source = requireNonNull(source);
		this.versions = versions;
		this.inMultipleSourcesSet = inMultipleSourcesSet;
	}

	public void setMediaAnalyserProcessResult(final MediaAnalyserProcessResult mediaAnalyserResult) {
		this.mediaAnalyserResult = mediaAnalyserResult;
		if (mediaAnalyserResult != null && ffmpegCommandLine == null) {
			ffmpegCommandLine = mediaAnalyserResult.ffmpegCommandLine();
		}
	}

	public void setContainerAnalyserProcessResult(final ContainerAnalyserProcessResult containerAnalyserResult) {
		this.containerAnalyserResult = containerAnalyserResult;
		if (containerAnalyserResult != null && ffprobeCommandLine == null) {
			ffprobeCommandLine = containerAnalyserResult.ffprobeCommandLine();
		}
	}

	public Optional<MediaAnalyserProcessResult> getMediaAnalyserProcessResult() {
		return Optional.ofNullable(mediaAnalyserResult);
	}

	public Optional<Duration> getSourceDuration() {
		return Optional.ofNullable(sourceDuration);
	}

	public Optional<FFprobeJAXB> getFFprobeResult() {
		return Optional.ofNullable(ffprobeResult);
	}

	public Optional<String> getFfmpegCommandLine() {
		return Optional.ofNullable(ffmpegCommandLine);
	}

	public Optional<String> getFfprobeCommandLine() {
		return Optional.ofNullable(ffprobeCommandLine);
	}

	public Optional<MeasuredWav> getWavForm() {
		return Optional.ofNullable(wavForm);
	}

	public Optional<VideoImageSnapshots> getVideoImageSnapshots() {
		return Optional.ofNullable(videoImageSnapshots);
	}

	public Optional<ContainerAnalyserProcessResult> getContainerAnalyserProcessResult() {
		return Optional.ofNullable(containerAnalyserResult);
	}

	public Optional<Dimension> getVideoResolution() {
		return getFFprobeResult()
				.flatMap(FFprobeJAXB::getFirstVideoStream)
				.flatMap(v -> {
					final var width = v.width();
					final var height = v.height();
					if (width > 0 && height > 0) {
						return Optional.ofNullable(new Dimension(width, height));
					}
					return Optional.empty();
				})
				.or(() -> getContainerAnalyserProcessResult()
						.map(ContainerAnalyserProcessResult::videoConst)
						.flatMap(Optional::ofNullable)
						.flatMap(videoFrameConstToDimension()))
				.or(() -> getContainerAnalyserProcessResult()
						.map(ContainerAnalyserProcessResult::olderVideoConsts)
						.flatMap(f -> f.stream().findFirst())
						.flatMap(videoFrameConstToDimension()));
	}

	private Function<? super FFprobeVideoFrameConst, ? extends Optional<? extends Dimension>> videoFrameConstToDimension() {
		return v -> {
			final var width = v.width();
			final var height = v.height();
			if (width > 0 && height > 0) {
				return Optional.ofNullable(new Dimension(width, height));
			}
			return Optional.empty();
		};
	}

	public void setFfprobeResult(final FFprobeJAXB ffprobeResult) {
		this.ffprobeResult = ffprobeResult;
		sourceDuration = ffprobeResult.getFormat()
				.map(FFProbeFormat::duration)
				.map(LavfiMtdEvent::secFloatToDuration)
				.orElse(null);
	}

	public void resultMetadatasToReportDocument(final ReportDocument document) {
		versions.entrySet().stream()
				.map(entry -> new SimpleKeyValueReportEntry(entry.getKey(), entry.getValue()))
				.forEach(document::addAboutSection);
	}

}
