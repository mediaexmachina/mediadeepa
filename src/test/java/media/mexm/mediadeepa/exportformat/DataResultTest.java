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
package media.mexm.mediadeepa.exportformat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ffmpeg.ffprobe.StreamType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.fflauncher.resultparser.Ebur128StrErrFilterEvent;
import tv.hd3g.fflauncher.resultparser.RawStdErrFilterEvent;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;

class DataResultTest {
	static Faker faker = net.datafaker.Faker.instance();

	DataResult dr;

	String source;
	int width;
	int height;

	@Mock
	MediaAnalyserResult mediaAnalyserResult;
	@Mock
	Duration sourceDuration;
	@Mock
	FFprobeJAXB ffprobeResult;
	@Mock
	ContainerAnalyserResult containerAnalyserResult;
	@Mock
	List<Ebur128StrErrFilterEvent> ebur128events;
	@Mock
	List<RawStdErrFilterEvent> rawStdErrEvents;
	@Mock
	Map<String, String> versions;
	@Mock
	StreamType streamType;
	@Mock
	FFprobeVideoFrameConst ffprobeVideoFrameConst;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		source = faker.numerify("source###");
		width = faker.random().nextInt(100, 10000);
		height = faker.random().nextInt(100, 10000);
		dr = new DataResult(source, versions);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				mediaAnalyserResult,
				sourceDuration,
				ffprobeResult,
				containerAnalyserResult,
				ebur128events,
				rawStdErrEvents,
				versions,
				streamType,
				ffprobeVideoFrameConst);
	}

	@Test
	void testSetEbur128events() {
		dr.setEbur128events(ebur128events);
		final var result = dr.getEbur128events();
		assertThat(Mockito.mockingDetails(result).isMock()).isFalse();
		assertThrows(UnsupportedOperationException.class,
				() -> result.clear());
	}

	@Test
	void testSetRawStdErrEvents() {
		dr.setRawStdErrEvents(rawStdErrEvents);
		final var result = dr.getRawStdErrEvents();
		assertThat(Mockito.mockingDetails(result).isMock()).isFalse();
		assertThrows(UnsupportedOperationException.class,
				() -> result.clear());
	}

	@Test
	void testGetMediaAnalyserResult() {
		assertFalse(dr.getMediaAnalyserResult().isPresent());
		dr.setMediaAnalyserResult(mediaAnalyserResult);
		assertTrue(dr.getMediaAnalyserResult().isPresent());
		assertEquals(mediaAnalyserResult, dr.getMediaAnalyserResult().get());
	}

	@Test
	void testGetSourceDuration() {
		assertFalse(dr.getSourceDuration().isPresent());
		dr.setSourceDuration(sourceDuration);
		assertTrue(dr.getSourceDuration().isPresent());
		assertEquals(sourceDuration, dr.getSourceDuration().get());
	}

	@Test
	void testGetFFprobeResult() {
		assertFalse(dr.getFFprobeResult().isPresent());
		dr.setFfprobeResult(ffprobeResult);
		assertTrue(dr.getFFprobeResult().isPresent());
		assertEquals(ffprobeResult, dr.getFFprobeResult().get());
	}

	@Test
	void testGetContainerAnalyserResult() {
		assertFalse(dr.getContainerAnalyserResult().isPresent());
		dr.setContainerAnalyserResult(containerAnalyserResult);
		assertTrue(dr.getContainerAnalyserResult().isPresent());
		assertEquals(containerAnalyserResult, dr.getContainerAnalyserResult().get());
	}

	@Test
	void testGetVideoResolution_empty() {
		assertFalse(dr.getVideoResolution().isPresent());
	}

	@Test
	void testGetVideoResolution_jaxb() {
		dr.setFfprobeResult(ffprobeResult);
		when(ffprobeResult.getFirstVideoStream())
				.thenReturn(Optional.ofNullable(streamType));
		when(streamType.getWidth()).thenReturn(width);
		when(streamType.getHeight()).thenReturn(height);

		assertTrue(dr.getVideoResolution().isPresent());
		assertEquals(new Dimension(width, height), dr.getVideoResolution().get());

		verify(ffprobeResult, atLeast(1)).getFirstVideoStream();
		verify(streamType, atLeast(1)).getWidth();
		verify(streamType, atLeast(1)).getHeight();
	}

	@Test
	void testGetVideoResolution_jaxbEmpty() {
		dr.setFfprobeResult(ffprobeResult);
		when(ffprobeResult.getFirstVideoStream())
				.thenReturn(Optional.ofNullable(streamType));
		when(streamType.getWidth()).thenReturn(0);
		when(streamType.getHeight()).thenReturn(0);

		assertFalse(dr.getVideoResolution().isPresent());

		verify(ffprobeResult, atLeast(1)).getFirstVideoStream();
		verify(streamType, atLeast(1)).getWidth();
		verify(streamType, atLeast(1)).getHeight();
	}

	@Test
	void testGetVideoResolution_containerAnalyserResultVideoConst() {
		dr.setContainerAnalyserResult(containerAnalyserResult);
		when(containerAnalyserResult.videoConst())
				.thenReturn(ffprobeVideoFrameConst);
		when(ffprobeVideoFrameConst.width()).thenReturn(width);
		when(ffprobeVideoFrameConst.height()).thenReturn(height);
		assertTrue(dr.getVideoResolution().isPresent());
		assertEquals(new Dimension(width, height), dr.getVideoResolution().get());

		verify(containerAnalyserResult, atLeast(1)).videoConst();
		verify(ffprobeVideoFrameConst, atLeast(1)).width();
		verify(ffprobeVideoFrameConst, atLeast(1)).height();
	}

	@Test
	void testGetVideoResolution_containerAnalyserResultOlderVideoConsts() {
		dr.setContainerAnalyserResult(containerAnalyserResult);
		when(containerAnalyserResult.olderVideoConsts())
				.thenReturn(List.of(ffprobeVideoFrameConst));
		when(ffprobeVideoFrameConst.width()).thenReturn(width);
		when(ffprobeVideoFrameConst.height()).thenReturn(height);
		assertTrue(dr.getVideoResolution().isPresent());
		assertEquals(new Dimension(width, height), dr.getVideoResolution().get());

		verify(containerAnalyserResult, atLeast(1)).videoConst();
		verify(containerAnalyserResult, atLeast(1)).olderVideoConsts();
		verify(ffprobeVideoFrameConst, atLeast(1)).width();
		verify(ffprobeVideoFrameConst, atLeast(1)).height();
	}
}
