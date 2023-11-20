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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.awt.Dimension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

@EqualsAndHashCode
public class DataResult {

	@Getter
	private final String source;
	@Setter
	private MediaAnalyserResult mediaAnalyserResult;
	@Setter
	private Duration sourceDuration;
	@Setter
	private FFprobeJAXB ffprobeResult;
	@Setter
	private ContainerAnalyserResult containerAnalyserResult;
	@Getter
	private List<Ebur128StrErrFilterEvent> ebur128events;
	@Getter
	private List<RawStdErrFilterEvent> rawStdErrEvents;
	@Getter
	private final Map<String, String> versions;

	public DataResult(final String source, final Map<String, String> versions) {
		this.source = requireNonNull(source);
		this.versions = versions;
		ebur128events = List.of();
		rawStdErrEvents = List.of();
	}

	public void setEbur128events(final List<Ebur128StrErrFilterEvent> ebur128events) {
		this.ebur128events = unmodifiableList(requireNonNull(ebur128events));
	}

	public void setRawStdErrEvents(final List<RawStdErrFilterEvent> rawStdErrEvents) {
		this.rawStdErrEvents = unmodifiableList(requireNonNull(rawStdErrEvents));
	}

	public Optional<MediaAnalyserResult> getMediaAnalyserResult() {
		return Optional.ofNullable(mediaAnalyserResult);
	}

	public Optional<Duration> getSourceDuration() {
		return Optional.ofNullable(sourceDuration);
	}

	public Optional<FFprobeJAXB> getFFprobeResult() {
		return Optional.ofNullable(ffprobeResult);
	}

	public Optional<ContainerAnalyserResult> getContainerAnalyserResult() {
		return Optional.ofNullable(containerAnalyserResult);
	}

	public Optional<Dimension> getVideoResolution() {
		return getFFprobeResult()
				.flatMap(FFprobeJAXB::getFirstVideoStream)
				.flatMap(v -> {
					final var width = v.getWidth();
					final var height = v.getHeight();
					if (width != null && height != null && width > 0 && height > 0) {
						return Optional.ofNullable(new Dimension(width, height));
					}
					return Optional.empty();
				})
				.or(() -> getContainerAnalyserResult()
						.map(ContainerAnalyserResult::videoConst)
						.flatMap(Optional::ofNullable)
						.flatMap(videoFrameConstToDimension()))
				.or(() -> getContainerAnalyserResult()
						.map(ContainerAnalyserResult::olderVideoConsts)
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

}
