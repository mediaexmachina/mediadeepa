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
package media.mexm.mediadeepa.service;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile;
import media.mexm.mediadeepa.components.CLIRunner.AppCommand.ProcessFile.TypeExclusive;
import tv.hd3g.fflauncher.about.FFAbout;
import tv.hd3g.fflauncher.about.FFAboutFilter;
import tv.hd3g.fflauncher.enums.Channel;
import tv.hd3g.fflauncher.enums.ChannelLayout;
import tv.hd3g.fflauncher.filtering.AbstractFilterMetadata;
import tv.hd3g.fflauncher.filtering.AbstractFilterMetadata.Mode;
import tv.hd3g.fflauncher.filtering.AudioFilterAMetadata;
import tv.hd3g.fflauncher.filtering.AudioFilterAPhasemeter;
import tv.hd3g.fflauncher.filtering.AudioFilterAmerge;
import tv.hd3g.fflauncher.filtering.AudioFilterAstats;
import tv.hd3g.fflauncher.filtering.AudioFilterChannelmap;
import tv.hd3g.fflauncher.filtering.AudioFilterChannelsplit;
import tv.hd3g.fflauncher.filtering.AudioFilterEbur128;
import tv.hd3g.fflauncher.filtering.AudioFilterEbur128.Peak;
import tv.hd3g.fflauncher.filtering.AudioFilterJoin;
import tv.hd3g.fflauncher.filtering.AudioFilterSilencedetect;
import tv.hd3g.fflauncher.filtering.AudioFilterSupplier;
import tv.hd3g.fflauncher.filtering.AudioFilterVolumedetect;
import tv.hd3g.fflauncher.filtering.Filter;
import tv.hd3g.fflauncher.filtering.VideoFilterBlackdetect;
import tv.hd3g.fflauncher.filtering.VideoFilterBlockdetect;
import tv.hd3g.fflauncher.filtering.VideoFilterBlurdetect;
import tv.hd3g.fflauncher.filtering.VideoFilterCropdetect;
import tv.hd3g.fflauncher.filtering.VideoFilterFreezedetect;
import tv.hd3g.fflauncher.filtering.VideoFilterIdet;
import tv.hd3g.fflauncher.filtering.VideoFilterMEstimate;
import tv.hd3g.fflauncher.filtering.VideoFilterMetadata;
import tv.hd3g.fflauncher.filtering.VideoFilterSiti;
import tv.hd3g.fflauncher.filtering.VideoFilterSupplier;
import tv.hd3g.fflauncher.progress.ProgressBlock;
import tv.hd3g.fflauncher.progress.ProgressCallback;
import tv.hd3g.fflauncher.progress.ProgressListener;
import tv.hd3g.fflauncher.recipes.ContainerAnalyser;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserSession;
import tv.hd3g.fflauncher.recipes.MediaAnalyser;
import tv.hd3g.fflauncher.recipes.MediaAnalyserSession;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Service
@Slf4j
public class FFmpegServiceImpl implements FFmpegService {

	private static final Set<AudioFilterSupplier> allAudioFilters = Set.of(
			new AudioFilterAMetadata(Mode.PRINT),
			new AudioFilterAmerge(2),
			new AudioFilterAPhasemeter(),
			new AudioFilterAstats(),
			new AudioFilterChannelmap(ChannelLayout.MONO, Map.of()),
			new AudioFilterChannelsplit(ChannelLayout.MONO, List.of()),
			new AudioFilterEbur128(),
			new AudioFilterJoin(1, ChannelLayout.MONO, Map.of(Channel.FC, "1")),
			new AudioFilterSilencedetect(),
			new AudioFilterVolumedetect());

	private static final Set<VideoFilterSupplier> allVideoFilters = Set.of(
			new VideoFilterBlackdetect(),
			new VideoFilterBlockdetect(),
			new VideoFilterBlurdetect(),
			new VideoFilterCropdetect(),
			new VideoFilterFreezedetect(),
			new VideoFilterIdet(),
			new VideoFilterMEstimate(),
			new VideoFilterMetadata(Mode.PRINT),
			new VideoFilterSiti());

	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private String ffmpegExecName;
	@Autowired
	private String ffprobeExecName;
	@Autowired
	private FFAbout ffmpegAbout;
	@Autowired
	private FFAbout ffprobeAbout;
	@Autowired
	private ScheduledExecutorService maxExecTimeScheduler;
	@Autowired
	private ProgressListener progressListener;
	@Autowired
	private Supplier<ProgressCLI> progressSupplier;

	@Override
	public Map<String, String> getMtdFiltersAvaliable() {
		final var allAudioFiltersNames = allAudioFilters.stream()
				.map(AudioFilterSupplier::toFilter)
				.map(Filter::getFilterName)
				.sorted()
				.toList();
		final var allVideoFiltersNames = allVideoFilters.stream()
				.map(VideoFilterSupplier::toFilter)
				.map(Filter::getFilterName)
				.sorted()
				.toList();

		final var filters = ffmpegAbout.getFilters().stream()
				.collect(toUnmodifiableMap(FFAboutFilter::getTag, FFAboutFilter::getLongName));

		final var result = new LinkedHashMap<String, String>();

		allAudioFiltersNames.stream()
				.filter(filters::containsKey)
				.forEach(n -> result.put(n, filters.get(n)));
		allAudioFiltersNames.stream()
				.filter(Predicate.not(filters::containsKey))
				.forEach(n -> result.put(n, "UNAVAILABLE"));
		allVideoFiltersNames.stream()
				.filter(filters::containsKey)
				.forEach(n -> result.put(n, filters.get(n)));
		allVideoFiltersNames.stream()
				.filter(Predicate.not(filters::containsKey))
				.forEach(n -> result.put(n, "UNAVAILABLE"));

		return result;
	}

	@Override
	public Map<String, String> getVersions() {
		final var result = new LinkedHashMap<String, String>();
		result.put("ffmpeg", ffmpegAbout.getVersion().headerVersion);
		result.put("ffprobe", ffprobeAbout.getVersion().headerVersion);
		return result;
	}

	@Override
	public MediaAnalyserSession createMediaAnalyserSession(final ProcessFile processFile,
														   final File lavfiSecondaryVideoFile,
														   final FFprobeJAXB ffprobeJAXB) {
		final var ma = new MediaAnalyser(ffmpegExecName, executableFinder, ffmpegAbout);

		final var programDurationSec = ffprobeJAXB.getFormat().getDuration();
		setProgress(progressSupplier.get(), programDurationSec, ma);

		applyMediaAnalyserFilterChain(
				processFile,
				lavfiSecondaryVideoFile,
				ffprobeJAXB.getFirstVideoStream().isPresent(),
				ffprobeJAXB.getAudiosStreams().findAny().isPresent(),
				ma);

		final var session = ma.createSession(processFile.getInput());
		session.setPgmFFDuration(processFile.getDuration());
		session.setPgmFFStartTime(processFile.getStartTime());
		return session;
	}

	private boolean countFilter(final List<Boolean> countFilter) {
		return countFilter.stream().anyMatch(Boolean::booleanValue);
	}

	@Override
	public void applyMediaAnalyserFilterChain(final ProcessFile processFile,
											  final File lavfiSecondaryVideoFile,
											  final boolean sourceHasVideo,
											  final boolean sourceHasAudio,
											  final MediaAnalyser ma) {
		var useMtdAudio = false;
		final var fIgnore = Optional.ofNullable(processFile.getFiltersIgnore()).orElse(Set.of());
		final var fOnly = Optional.ofNullable(processFile.getFiltersOnly()).orElse(Set.of());

		final var audioNo = Optional.ofNullable(processFile.getTypeExclusive())
				.map(TypeExclusive::isAudioNo)
				.orElse(false)
				.booleanValue();
		final var videoNo = Optional.ofNullable(processFile.getTypeExclusive())
				.map(TypeExclusive::isVideoNo)
				.orElse(false)
				.booleanValue();

		if (sourceHasAudio && audioNo == false) {
			final var countFilter = new ArrayList<Boolean>();

			countFilter.add(addFilter(ma, fIgnore, fOnly, new AudioFilterAPhasemeter()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new AudioFilterAstats().setSelectedMetadatas()));
			final var silence = new AudioFilterSilencedetect();
			silence.setMono(true);
			countFilter.add(addFilter(ma, fIgnore, fOnly, silence));

			if (countFilter(countFilter)) {
				final var aMetadata = new AudioFilterAMetadata(AbstractFilterMetadata.Mode.PRINT);
				aMetadata.setFile("-");
				addFilter(ma, fIgnore, fOnly, aMetadata);
			}

			final var ebur128 = new AudioFilterEbur128();
			ebur128.setPeakMode(Set.of(Peak.SAMPLE, Peak.TRUE));
			countFilter.add(addFilter(ma, fIgnore, fOnly, ebur128));

			useMtdAudio = countFilter(countFilter);
		}

		if (sourceHasVideo && videoNo == false) {
			final var countFilter = new ArrayList<Boolean>();

			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterBlackdetect()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterBlockdetect()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterBlurdetect()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterCropdetect()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterIdet()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterSiti()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterFreezedetect()));

			if (countFilter(countFilter)) {
				final var vMetadata = new VideoFilterMetadata(AbstractFilterMetadata.Mode.PRINT);
				if (useMtdAudio) {
					vMetadata.setFile("'" +
									  lavfiSecondaryVideoFile.getPath()
											  .replace("\\", "\\\\")
											  .replace("'", "\\'")
											  .replace(":", "\\:")
									  + "'");
				} else {
					vMetadata.setFile("-");
				}
				addFilter(ma, fIgnore, fOnly, vMetadata);
			}
		}
	}

	private boolean addFilter(final MediaAnalyser ma,
							  final Set<String> fIgnore,
							  final Set<String> fOnly,
							  final AudioFilterSupplier filter) {
		final var filterName = filter.toFilter().getFilterName();
		if (fOnly.isEmpty()) {
			if (fIgnore.contains(filterName) == false) {
				return ma.addOptionalFilter(filter, this::logFilterPresence);
			}
		} else {
			if (fOnly.contains(filterName)) {
				return ma.addOptionalFilter(filter, this::logFilterPresence);
			}
		}
		return false;
	}

	private boolean addFilter(final MediaAnalyser ma,
							  final Set<String> fIgnore,
							  final Set<String> fOnly,
							  final VideoFilterSupplier filter) {
		final var filterName = filter.toFilter().getFilterName();
		if (fOnly.isEmpty()) {
			if (fIgnore.contains(filterName) == false) {
				return ma.addOptionalFilter(filter, this::logFilterPresence);
			}
		} else {
			if (fOnly.contains(filterName)) {
				return ma.addOptionalFilter(filter, this::logFilterPresence);
			}
		}
		return false;
	}

	private void logFilterPresence(final VideoFilterSupplier f) {
		log.info("Setup {} video filter", f.toFilter());
	}

	private void logFilterPresence(final AudioFilterSupplier f) {
		log.info("Setup {} audio filter", f.toFilter());
	}

	private void setProgress(final ProgressCLI progressCLI, final float programDurationSec, final MediaAnalyser ma) {
		ma.setProgress(progressListener, new ProgressCallback() {

			@Override
			public void onProgress(final int localhostTcpPort, final ProgressBlock progressBlock) {
				progressCLI.displayProgress(progressBlock.getOutTimeDuration().toSeconds() / programDurationSec,
						progressBlock.getSpeedX());
			}

			@Override
			public void onConnectionReset(final int localhostTcpPort, final SocketException e) {
				log.warn("Lost ffmpeg connection...");
			}

			@Override
			public void onEndProgress(final int localhostTcpPort) {
				progressCLI.end();
			}
		});
	}

	@Override
	public ContainerAnalyserSession createContainerAnalyserSession(final ProcessFile processFile) {
		final var ca = new ContainerAnalyser(ffprobeExecName, executableFinder);
		return ca.createSession(processFile.getInput());
	}

	@Override
	public FFprobeJAXB getFFprobeJAXBFromFileToProcess(final ProcessFile processFile) {
		return new ProbeMedia(executableFinder, maxExecTimeScheduler)
				.doAnalysing(processFile.getInput());
	}

}
