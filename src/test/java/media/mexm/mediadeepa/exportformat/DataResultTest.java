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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.awt.Dimension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.datafaker.Faker;
import tv.hd3g.fflauncher.ffprobecontainer.FFprobeVideoFrameConst;
import tv.hd3g.fflauncher.recipes.ContainerAnalyserResult;
import tv.hd3g.fflauncher.recipes.MediaAnalyserResult;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.data.FFProbeStream;

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
	Map<String, String> versions;
	@Mock
	FFProbeStream streamType;
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
				versions,
				streamType,
				ffprobeVideoFrameConst);
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
		verify(ffprobeResult, times(1)).getFormat();
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
		when(streamType.width()).thenReturn(width);
		when(streamType.height()).thenReturn(height);

		assertTrue(dr.getVideoResolution().isPresent());
		assertEquals(new Dimension(width, height), dr.getVideoResolution().get());

		verify(ffprobeResult, atLeast(1)).getFirstVideoStream();
		verify(ffprobeResult, atLeast(1)).getFormat();
		verify(streamType, atLeast(1)).width();
		verify(streamType, atLeast(1)).height();
	}

	@Test
	void testGetVideoResolution_jaxbEmpty() {
		dr.setFfprobeResult(ffprobeResult);
		when(ffprobeResult.getFirstVideoStream())
				.thenReturn(Optional.ofNullable(streamType));
		when(streamType.width()).thenReturn(0);
		when(streamType.height()).thenReturn(0);

		assertFalse(dr.getVideoResolution().isPresent());

		verify(ffprobeResult, atLeast(1)).getFirstVideoStream();
		verify(ffprobeResult, atLeast(1)).getFormat();
		verify(streamType, atLeast(1)).width();
		verify(streamType, atLeast(1)).height();
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
