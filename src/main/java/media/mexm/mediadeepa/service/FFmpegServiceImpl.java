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
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.File;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.cli.FilterCmd;
import media.mexm.mediadeepa.cli.ProcessFileCmd;
import media.mexm.mediadeepa.cli.TypeExclusiveCmd;
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
import tv.hd3g.fflauncher.filtering.FilterSupplier;
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

	private static final Set<String> mandatoryFilters = List.of(
			new AudioFilterAMetadata(Mode.PRINT),
			new AudioFilterAmerge(2),
			new AudioFilterChannelmap(ChannelLayout.MONO, Map.of()),
			new AudioFilterChannelsplit(ChannelLayout.MONO, List.of()),
			new AudioFilterJoin(1, ChannelLayout.MONO, Map.of(Channel.FC, "1")),
			new VideoFilterMEstimate(),
			new VideoFilterMetadata(Mode.PRINT))
			.stream()
			.map(FilterSupplier::toFilter)
			.map(Filter::getFilterName)
			.distinct()
			.collect(toUnmodifiableSet());

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
	public MediaAnalyserSession createMediaAnalyserSession(final ProcessFileCmd processFileCmd,
														   final File lavfiSecondaryVideoFile,
														   final FFprobeJAXB ffprobeJAXB,
														   final FilterCmd options) {
		final var ma = new MediaAnalyser(ffmpegExecName, executableFinder, ffmpegAbout);

		final var programDurationSec = ffprobeJAXB.getFormat().getDuration();
		setProgress(progressSupplier.get(), programDurationSec, ma);

		applyMediaAnalyserFilterChain(
				processFileCmd,
				lavfiSecondaryVideoFile,
				ffprobeJAXB.getFirstVideoStream().isPresent(),
				ffprobeJAXB.getAudiosStreams().findAny().isPresent(),
				ma,
				Optional.ofNullable(options).orElseGet(FilterCmd::new));

		final var session = ma.createSession(processFileCmd.getInput());
		session.setPgmFFDuration(processFileCmd.getDuration());
		session.setPgmFFStartTime(processFileCmd.getStartTime());
		return session;
	}

	private boolean countFilter(final List<Boolean> countFilter) {
		return countFilter.stream().anyMatch(Boolean::booleanValue);
	}

	@Override
	public void applyMediaAnalyserFilterChain(final ProcessFileCmd processFileCmd,
											  final File lavfiSecondaryVideoFile,
											  final boolean sourceHasVideo,
											  final boolean sourceHasAudio,
											  final MediaAnalyser ma,
											  final FilterCmd nullableOptions) {
		var useMtdAudio = false;
		final var fIgnore = Optional.ofNullable(processFileCmd.getFiltersIgnore()).orElse(Set.of());
		final var fOnly = Optional.ofNullable(processFileCmd.getFiltersOnly()).orElse(Set.of());

		final var audioNo = Optional.ofNullable(processFileCmd.getTypeExclusiveCmd())
				.map(TypeExclusiveCmd::isAudioNo)
				.orElse(false)
				.booleanValue();
		final var videoNo = Optional.ofNullable(processFileCmd.getTypeExclusiveCmd())
				.map(TypeExclusiveCmd::isVideoNo)
				.orElse(false)
				.booleanValue();

		final var options = Optional.ofNullable(nullableOptions).orElseGet(FilterCmd::new);
		if (sourceHasAudio && audioNo == false) {
			final var countFilter = new ArrayList<Boolean>();

			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new AudioFilterAPhasemeter(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new AudioFilterAstats().setSelectedMetadatas()));
			final var silence = filterSetup(new AudioFilterSilencedetect(), options);
			silence.setMono(true);
			countFilter.add(addFilter(ma, fIgnore, fOnly, silence));

			if (countFilter(countFilter)) {
				final var aMetadata = new AudioFilterAMetadata(AbstractFilterMetadata.Mode.PRINT);
				aMetadata.setFile("-");
				addFilter(ma, fIgnore, fOnly, aMetadata);
			}

			final var ebur128 = filterSetup(new AudioFilterEbur128(), options);
			ebur128.setPeakMode(new TreeSet<>(List.of(Peak.SAMPLE, Peak.TRUE)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, ebur128));

			useMtdAudio = countFilter(countFilter);
		}

		if (sourceHasVideo && videoNo == false) {
			final var countFilter = new ArrayList<Boolean>();

			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterBlackdetect(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterBlockdetect(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterBlurdetect(), options)));

			final var videoFilterCropdetect = new VideoFilterCropdetect();
			videoFilterCropdetect.setSkip(0);
			videoFilterCropdetect.setReset(1);
			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(videoFilterCropdetect, options)));

			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterIdet(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new VideoFilterSiti()));
			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterFreezedetect(), options)));

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

	private AudioFilterEbur128 filterSetup(final AudioFilterEbur128 audioFilterEbur128, final FilterCmd options) {
		Optional.ofNullable(options.getEbur128Target())
				.ifPresent(audioFilterEbur128::setTarget);
		return audioFilterEbur128;
	}

	private VideoFilterFreezedetect filterSetup(final VideoFilterFreezedetect videoFilterFreezedetect,
												final FilterCmd options) {
		Optional.ofNullable(options.getFreezedetectNoiseTolerance())
				.ifPresent(videoFilterFreezedetect::setNoiseToleranceDb);
		Optional.ofNullable(options.getFreezedetectDuration())
				.stream()
				.filter(d -> d > 0)
				.map(Duration::ofSeconds)
				.findFirst()
				.ifPresent(videoFilterFreezedetect::setFreezeDuration);
		return videoFilterFreezedetect;
	}

	private VideoFilterIdet filterSetup(final VideoFilterIdet videoFilterIdet, final FilterCmd options) {
		Optional.ofNullable(options.getIdetIntlThres())
				.ifPresent(videoFilterIdet::setIntlThres);
		Optional.ofNullable(options.getIdetProgThres())
				.ifPresent(videoFilterIdet::setProgThres);
		Optional.ofNullable(options.getIdetRepThres())
				.ifPresent(videoFilterIdet::setRepThres);
		Optional.ofNullable(options.getIdetHalfLife())
				.ifPresent(videoFilterIdet::setHalfLife);
		return videoFilterIdet;
	}

	private VideoFilterCropdetect filterSetup(final VideoFilterCropdetect videoFilterCropdetect,
											  final FilterCmd options) {
		Optional.ofNullable(options.getCropHigh())
				.ifPresent(videoFilterCropdetect::setHigh);
		Optional.ofNullable(options.getCropLow())
				.ifPresent(videoFilterCropdetect::setLow);
		Optional.ofNullable(options.getCropSkip())
				.ifPresent(videoFilterCropdetect::setSkip);
		Optional.ofNullable(options.getCropRound())
				.ifPresent(videoFilterCropdetect::setRound);
		Optional.ofNullable(options.getCropLimit())
				.ifPresent(videoFilterCropdetect::setLimit);
		Optional.ofNullable(options.getCropReset())
				.ifPresent(videoFilterCropdetect::setReset);
		return videoFilterCropdetect;
	}

	private VideoFilterBlurdetect filterSetup(final VideoFilterBlurdetect videoFilterBlurdetect,
											  final FilterCmd options) {
		Optional.ofNullable(options.getBlurHigh())
				.ifPresent(videoFilterBlurdetect::setHigh);
		Optional.ofNullable(options.getBlurLow())
				.ifPresent(videoFilterBlurdetect::setLow);
		Optional.ofNullable(options.getBlurRadius())
				.ifPresent(videoFilterBlurdetect::setRadius);
		Optional.ofNullable(options.getBlurBlockPct())
				.ifPresent(videoFilterBlurdetect::setBlockPct);
		Optional.ofNullable(options.getBlurBlockHeight())
				.ifPresent(videoFilterBlurdetect::setBlockHeight);
		Optional.ofNullable(options.getBlurBlockWidth())
				.ifPresent(videoFilterBlurdetect::setBlockWidth);
		Optional.ofNullable(options.getBlurPlanes())
				.ifPresent(videoFilterBlurdetect::setPlanes);
		return videoFilterBlurdetect;
	}

	private VideoFilterBlockdetect filterSetup(final VideoFilterBlockdetect videoFilterBlockdetect,
											   final FilterCmd options) {
		Optional.ofNullable(options.getBlockPeriodMax())
				.ifPresent(videoFilterBlockdetect::setPeriodMax);
		Optional.ofNullable(options.getBlockPeriodMin())
				.ifPresent(videoFilterBlockdetect::setPeriodMin);
		Optional.ofNullable(options.getBlockPlanes())
				.ifPresent(videoFilterBlockdetect::setPlanes);
		return videoFilterBlockdetect;
	}

	private VideoFilterBlackdetect filterSetup(final VideoFilterBlackdetect videoFilterBlackdetect,
											   final FilterCmd options) {
		Optional.ofNullable(options.getBlackPictureBlackRatioTh())
				.ifPresent(videoFilterBlackdetect::setPictureBlackRatioTh);
		Optional.ofNullable(options.getBlackPixelBlackTh())
				.ifPresent(videoFilterBlackdetect::setPixelBlackTh);
		Optional.ofNullable(options.getBlackMinDuration())
				.stream()
				.filter(d -> d > 0)
				.map(Duration::ofMillis)
				.findFirst()
				.ifPresent(videoFilterBlackdetect::setBlackMinDuration);
		return videoFilterBlackdetect;
	}

	private AudioFilterAPhasemeter filterSetup(final AudioFilterAPhasemeter audioFilterAPhasemeter,
											   final FilterCmd options) {
		Optional.ofNullable(options.getAPhaseAngle())
				.ifPresent(audioFilterAPhasemeter::setAngle);
		Optional.ofNullable(options.getAPhaseTolerance())
				.ifPresent(audioFilterAPhasemeter::setTolerance);
		Optional.ofNullable(options.getAPhaseDuration())
				.stream()
				.filter(d -> d > 0)
				.map(Duration::ofMillis)
				.findFirst()
				.ifPresent(audioFilterAPhasemeter::setDuration);
		return audioFilterAPhasemeter;
	}

	private AudioFilterSilencedetect filterSetup(final AudioFilterSilencedetect audioFilterSilencedetect,
												 final FilterCmd options) {
		Optional.ofNullable(options.getSilenceNoise())
				.ifPresent(audioFilterSilencedetect::setNoiseDb);
		Optional.ofNullable(options.getSilenceDuration())
				.stream()
				.filter(d -> d > 0)
				.map(Duration::ofSeconds)
				.findFirst()
				.ifPresent(audioFilterSilencedetect::setDuration);
		return audioFilterSilencedetect;
	}

	private boolean addFilter(final MediaAnalyser ma,
							  final Set<String> fIgnore,
							  final Set<String> fOnly,
							  final FilterSupplier filter) {
		var added = false;
		final var filterName = filter.toFilter().getFilterName();
		if (mandatoryFilters.contains(filterName)) {
			added = true;
		} else if (fOnly.isEmpty()) {
			if (fIgnore.contains(filterName) == false) {
				added = true;
			}
		} else {
			if (fOnly.contains(filterName)) {
				added = true;
			}
		}
		if (added == false) {
			return false;
		}

		if (filter instanceof final VideoFilterSupplier vfs) {
			return ma.addOptionalFilter(vfs, this::logFilterPresence);
		} else if (filter instanceof final AudioFilterSupplier afs) {
			return ma.addOptionalFilter(afs, this::logFilterPresence);
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
	public ContainerAnalyserSession createContainerAnalyserSession(final ProcessFileCmd processFileCmd) {
		final var ca = new ContainerAnalyser(ffprobeExecName, executableFinder);
		return ca.createSession(processFileCmd.getInput());
	}

	@Override
	public FFprobeJAXB getFFprobeJAXBFromFileToProcess(final ProcessFileCmd processFileCmd) {
		return new ProbeMedia(executableFinder, maxExecTimeScheduler)
				.doAnalysing(processFileCmd.getInput());
	}

}
