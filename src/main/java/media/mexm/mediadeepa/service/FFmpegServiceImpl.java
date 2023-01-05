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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.components.FFmpegSupplier;
import tv.hd3g.fflauncher.about.FFAbout;
import tv.hd3g.fflauncher.about.FFAboutFilter;
import tv.hd3g.fflauncher.enums.Channel;
import tv.hd3g.fflauncher.enums.ChannelLayout;
import tv.hd3g.fflauncher.filtering.AbstractFilterMetadata.Mode;
import tv.hd3g.fflauncher.filtering.AudioFilterAMetadata;
import tv.hd3g.fflauncher.filtering.AudioFilterAPhasemeter;
import tv.hd3g.fflauncher.filtering.AudioFilterAmerge;
import tv.hd3g.fflauncher.filtering.AudioFilterAstats;
import tv.hd3g.fflauncher.filtering.AudioFilterChannelmap;
import tv.hd3g.fflauncher.filtering.AudioFilterChannelsplit;
import tv.hd3g.fflauncher.filtering.AudioFilterEbur128;
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
import tv.hd3g.fflauncher.recipes.MediaAnalyser;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Service
public class FFmpegServiceImpl implements FFmpegService {
	private static Logger log = LogManager.getLogger();

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
			new VideoFilterCropdetect(VideoFilterCropdetect.Mode.BLACK),
			new VideoFilterFreezedetect(),
			new VideoFilterIdet(),
			new VideoFilterMEstimate(),
			new VideoFilterMetadata(Mode.PRINT),
			new VideoFilterSiti());

	@Autowired
	private FFmpegSupplier ffmpegSupplier;
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
		result.put("ffprobe", ffprobeAbout.getVersion().headerVersion);// TODO2 correct with https://github.com/hdsdi3g/medialib/issues/27
		return result;
	}

	private AudioFilterSupplier filterIfPresent(final AudioFilterSupplier f) {
		log.info("Setup {} audio filter", f.toFilter());
		return f;
	}

	private void filterIfPresent(final VideoFilterSupplier f) {
		log.info("Setup {} video filter", f.toFilter());
	}

	@Override
	public MediaAnalyserResult doExtractMtd(final File source,
											final ProgressCLI progressCLI,
											final boolean audioNo,
											final boolean videoNo) {
		final var pm = new ProbeMedia(executableFinder, maxExecTimeScheduler);
		final var ffprobeJAXB = pm.doAnalysing(source);

		final var programDurationSec = ffprobeJAXB.getFormat().getDuration();
		log.info("ffprobe result: {} ({} sec)",
				ffprobeJAXB.getFormat().getFormatName(),
				programDurationSec);// TODO2 better display ffprobeJAXB https://github.com/hdsdi3g/medialib/issues/28

		final var ma = new MediaAnalyser(ffmpegExecName, executableFinder, ffmpegAbout);
		setProgress(progressCLI, programDurationSec, ma);

		if (audioNo == false) {
			ma.addFilterPhasemeter(this::filterIfPresent);
			ma.addFilterAstats(a -> filterIfPresent(a.setSelectedMetadatas()));
			ma.addFilterSilencedetect(this::filterIfPresent);
			ma.addFilterEbur128(this::filterIfPresent);
		}

		if (videoNo == false) {
			ma.addFilterBlackdetect(this::filterIfPresent);
			ma.addFilterBlockdetect(this::filterIfPresent);
			ma.addFilterBlurdetect(this::filterIfPresent);
			ma.addFilterCropdetect(VideoFilterCropdetect.Mode.BLACK, this::filterIfPresent);
			ma.addFilterIdet(this::filterIfPresent);
			ma.addFilterSiti(this::filterIfPresent);
			ma.addFilterFreezedetect(this::filterIfPresent);
		}

		final var maSession = ma.createSession(source);
		maSession.setFFprobeResult(ffprobeJAXB);
		// FIXME missing R128
		// TODO2 graph maSession.setEbur128EventConsumer(ebur128EventConsumer);

		return maSession.process();
	}

	// TODO create ContainerAnalyser

	private void setProgress(final ProgressCLI progressCLI, final float programDurationSec, final MediaAnalyser ma) {
		ma.setProgress(progressListener, new ProgressCallback() {

			@Override
			public void onFFmpegConnection(final int localhostTcpPort) {
				progressCLI.startProgress();
			}

			@Override
			public void onProgress(final int localhostTcpPort, final ProgressBlock progressBlock) {
				progressCLI.displayProgress(progressBlock.getOutTimeMs().toSeconds() / programDurationSec);
			}

			@Override
			public void onEndProgress(final int localhostTcpPort) {
				progressCLI.endsProgress();
			}

			@Override
			public void onConnectionReset(final int localhostTcpPort, final SocketException e) {
				log.warn("Lost ffmpeg connection...");
			}

		});
	}
}
