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

import static java.lang.Math.round;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult.R128_DEFAULT_LUFS_TARGET;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.nio.file.Files;
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
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ProgressCLI;
import media.mexm.mediadeepa.cli.FilterCmd;
import media.mexm.mediadeepa.cli.ProcessFileCmd;
import media.mexm.mediadeepa.cli.TypeExclusiveCmd;
import media.mexm.mediadeepa.config.AppConfig;
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
import tv.hd3g.fflauncher.processingtool.FFmpegToolBuilder;
import tv.hd3g.fflauncher.progress.FFprobeXMLProgressWatcher;
import tv.hd3g.fflauncher.progress.ProgressBlock;
import tv.hd3g.fflauncher.progress.ProgressCallback;
import tv.hd3g.fflauncher.progress.ProgressListener;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserBase;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserExtract;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserExtractResult;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcess;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserBase;
import tv.hd3g.fflauncher.recipes.MediaAnalyserExtract;
import tv.hd3g.fflauncher.recipes.MediaAnalyserExtractResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcess;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserProcessSetup;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.fflauncher.recipes.wavmeasure.MeasuredWav;
import tv.hd3g.fflauncher.recipes.wavmeasure.WavMeasure;
import tv.hd3g.fflauncher.recipes.wavmeasure.WavMeasureSetup;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.data.FFProbeFormat;
import tv.hd3g.ffprobejaxb.data.FFProbeStream;
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
	private AppConfig appConfig;
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

	private void internalMedia(final MediaAnalyserBase<?, ?> ma,
							   final File inputFile,
							   final ProcessFileCmd processFileCmd,
							   final File lavfiSecondaryVideoFile,
							   final FFprobeJAXB ffprobeJAXB,
							   final FilterCmd options) {
		final var programDurationSec = ffprobeJAXB.getFormat().map(FFProbeFormat::duration).orElse(0f);
		setProgress(progressSupplier.get(), programDurationSec, ma);

		final var avgFrameRate = ffprobeJAXB.getFirstVideoStream()
				.map(FFProbeStream::avgFrameRate)
				.flatMap(Optional::ofNullable)
				.map(FFmpegServiceImpl::getAvgFrameRate)
				.orElse(1f);

		applyMediaAnalyserFilterChain(
				processFileCmd,
				lavfiSecondaryVideoFile,
				ffprobeJAXB.getFirstVideoStream().isPresent(),
				ffprobeJAXB.getAudioStreams().findAny().isPresent(),
				ma,
				Optional.ofNullable(options).orElseGet(FilterCmd::new),
				avgFrameRate);

		ma.setSource(inputFile);
		ma.setPgmFFDuration(processFileCmd.getDuration());
		ma.setPgmFFStartTime(processFileCmd.getStartTime());
		ma.setFfprobeResult(ffprobeJAXB);
		ma.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), maxExecTimeScheduler);
	}

	@Override
	public MediaAnalyserProcessResult processMedia(final File inputFile,
												   final ProcessFileCmd processFileCmd,
												   final File lavfiSecondaryVideoFile,
												   final FFprobeJAXB ffprobeJAXB,
												   final FilterCmd options) {
		final var ma = new MediaAnalyserProcess(appConfig.getFfmpegExecName(), ffmpegAbout);
		ma.setExecutableFinder(executableFinder);
		internalMedia(ma, inputFile, processFileCmd, lavfiSecondaryVideoFile, ffprobeJAXB, options);
		return ma.process(new MediaAnalyserProcessSetup(
				Optional.ofNullable(() -> openFileToLineStream(lavfiSecondaryVideoFile))))
				.getResult();
	}

	@Override
	public MediaAnalyserExtractResult extractMedia(final File inputFile,
												   final ProcessFileCmd processFileCmd,
												   final File lavfiSecondaryVideoFile,
												   final FFprobeJAXB ffprobeJAXB,
												   final FilterCmd options) {
		final var ma = new MediaAnalyserExtract(appConfig.getFfmpegExecName(), ffmpegAbout);
		ma.setExecutableFinder(executableFinder);
		internalMedia(ma, inputFile, processFileCmd, lavfiSecondaryVideoFile, ffprobeJAXB, options);
		return ma.process(null)
				.getResult();
	}

	private void internalContainer(final ContainerAnalyserBase<?, ?> ca,
								   final ProcessFileCmd processFileCmd,
								   final Duration programDuration) {
		final var progress = progressSupplier.get();
		ca.setProgressWatcher(new FFprobeXMLProgressWatcher(
				programDuration,
				s -> progress.displayProgress(0, 1),
				event -> progress.displayProgress(event.progress(), event.speed()),
				s -> progress.end()));
		ca.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), maxExecTimeScheduler);
	}

	@Override
	public ContainerAnalyserProcessResult processContainer(final File inputFile,
														   final ProcessFileCmd processFileCmd,
														   final Duration programDuration) {
		final var ca = new ContainerAnalyserProcess(appConfig.getFfprobeExecName());
		ca.setExecutableFinder(executableFinder);
		internalContainer(ca, processFileCmd, programDuration);
		return ca.process(inputFile).getResult();
	}

	@Override
	public ContainerAnalyserExtractResult extractContainer(final File inputFile,
														   final ProcessFileCmd processFileCmd,
														   final Duration programDuration) {
		final var ca = new ContainerAnalyserExtract(appConfig.getFfprobeExecName());
		ca.setExecutableFinder(executableFinder);
		internalContainer(ca, processFileCmd, programDuration);
		return ca.process(inputFile).getResult();
	}

	private static float getAvgFrameRate(final String avgFrameRate) {
		final var pos = avgFrameRate.indexOf("/");
		if (pos == -1) {
			return 1f;
		} else {
			final var l = Float.valueOf(avgFrameRate.substring(0, pos));
			final var r = Float.valueOf(avgFrameRate.substring(pos + 1));
			return l / r;
		}
	}

	private boolean countFilter(final List<Boolean> countFilter) {
		return countFilter.stream().anyMatch(Boolean::booleanValue);
	}

	private void applyMediaAnalyserFilterChain(final ProcessFileCmd processFileCmd,
											   final File lavfiSecondaryVideoFile,
											   final boolean sourceHasVideo,
											   final boolean sourceHasAudio,
											   final MediaAnalyserBase<?, ?> ma,
											   final FilterCmd nullableOptions,
											   final float avgFrameRate) {
		var useMtdAudio = false;
		final var fIgnore = Optional.ofNullable(processFileCmd.getFiltersIgnore()).orElse(Set.of());
		final var fOnly = Optional.ofNullable(processFileCmd.getFiltersOnly()).orElse(Set.of());

		final var options = Optional.ofNullable(nullableOptions).orElseGet(FilterCmd::new);
		if (sourceHasAudio && isAudioNo(processFileCmd) == false) {
			final var countFilter = new ArrayList<Boolean>();

			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new AudioFilterAPhasemeter(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, new AudioFilterAstats().setSelectedMetadatas()));
			final var silence = filterSetup(new AudioFilterSilencedetect(), options);
			silence.setMono(true);
			countFilter.add(addFilter(ma, fIgnore, fOnly, silence));

			final var ebur128 = new AudioFilterEbur128();
			Optional.ofNullable(options.getEbur128Target())
					.ifPresentOrElse(
							ebur128::setTarget,
							() -> ebur128.setTarget(R128_DEFAULT_LUFS_TARGET));
			ebur128.setPeakMode(new TreeSet<>(List.of(Peak.SAMPLE, Peak.TRUE)));
			ebur128.setMetadata(true);
			countFilter.add(addFilter(ma, fIgnore, fOnly, ebur128));

			useMtdAudio = countFilter(countFilter);
			if (useMtdAudio) {
				final var aMetadata = new AudioFilterAMetadata(AbstractFilterMetadata.Mode.PRINT);
				aMetadata.setFile("-");
				addFilter(ma, fIgnore, fOnly, aMetadata);
			}
		}

		if (sourceHasVideo && isVideoNo(processFileCmd) == false) {
			final var countFilter = new ArrayList<Boolean>();

			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterBlackdetect(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterBlockdetect(), options)));
			countFilter.add(addFilter(ma, fIgnore, fOnly, filterSetup(new VideoFilterBlurdetect(), options)));

			final var videoFilterCropdetect = new VideoFilterCropdetect();
			videoFilterCropdetect.setSkip(0);
			videoFilterCropdetect.setReset(round(avgFrameRate));
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

	static boolean isVideoNo(final ProcessFileCmd processFileCmd) {
		return Optional.ofNullable(processFileCmd.getTypeExclusiveCmd())
				.map(TypeExclusiveCmd::isVideoNo)
				.orElse(false)
				.booleanValue();
	}

	static boolean isAudioNo(final ProcessFileCmd processFileCmd) {
		return Optional.ofNullable(processFileCmd.getTypeExclusiveCmd())
				.map(TypeExclusiveCmd::isAudioNo)
				.orElse(false)
				.booleanValue();
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

	private boolean addFilter(final MediaAnalyserBase<?, ?> ma,
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

	private void setProgress(final ProgressCLI progressCLI,
							 final float programDurationSec,
							 final FFmpegToolBuilder<?, ?, ?> ma) {
		ma.setProgressListener(progressListener, new ProgressCallback() {

			@Override
			public void onProgress(final int localhostTcpPort, final ProgressBlock progressBlock) {
				progressCLI.displayProgress(
						progressBlock.getOutTimeDuration().toSeconds() / programDurationSec,
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
	public FFprobeJAXB getFFprobeJAXBFromFileToProcess(final File inputFile, final ProcessFileCmd processFileCmd) {
		final var pm = new ProbeMedia(maxExecTimeScheduler);
		pm.setExecutableFinder(executableFinder);
		return pm.process(inputFile).getResult();
	}

	@Override
	public Optional<MeasuredWav> measureWav(final File inputFile,
											final FFprobeJAXB ffprobeJAXB,
											final ProcessFileCmd processFileCmd) {
		if (isAudioNo(processFileCmd)
			|| processFileCmd.isNoWavForm()
			|| ffprobeJAXB.getAudioStreams().count() == 0l) {
			return Optional.empty();
		}
		log.info("Start waveform measure...");

		final var fileDuration = ffprobeJAXB.getDuration()
				.orElseThrow(() -> new IllegalArgumentException("Wav signal extraction need the source file duration"));

		final var wavMeasure = new WavMeasure(appConfig.getFfmpegExecName());
		wavMeasure.setExecutableFinder(executableFinder);
		setProgress(progressSupplier.get(), fileDuration.getSeconds(), wavMeasure);
		wavMeasure.setMaxExecutionTime(Duration.ofSeconds(processFileCmd.getMaxSec()), maxExecTimeScheduler);

		final var ffmpeg = wavMeasure.getFfmpeg();
		if (processFileCmd.getDuration() != null) {
			ffmpeg.addDuration(processFileCmd.getDuration());
		}
		if (processFileCmd.getStartTime() != null) {
			ffmpeg.addStartPosition(processFileCmd.getStartTime());
		}

		return Optional.ofNullable(
				wavMeasure.process(
						new WavMeasureSetup(
								inputFile,
								fileDuration,
								appConfig.getWavFormConfig().getImageSize().width))
						.getResult());
	}

	private static Stream<String> openFileToLineStream(final File file) {
		if (file.exists() == false || file.isFile() == false) {
			return Stream.empty();
		}
		try {
			return Files.lines(file.toPath());
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't open file", e);
		}
	}

}
