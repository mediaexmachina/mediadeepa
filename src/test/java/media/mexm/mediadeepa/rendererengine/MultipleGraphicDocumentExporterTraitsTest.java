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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.rendererengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import net.datafaker.Faker;

class MultipleGraphicDocumentExporterTraitsTest {
	static Faker faker = net.datafaker.Faker.instance();

	MultipleGraphicDocumentExporterTraitsImpl mgdet;

	@Mock
	DataResult result;
	@Mock
	Object graphicReportItem;
	@Mock
	SingleGraphicMaker<Object> singleGraphicMaker;
	@Mock
	GraphicArtifact graphicArtifact;
	String graphicBaseFileName;

	class MultipleGraphicDocumentExporterTraitsImpl implements MultipleGraphicDocumentExporterTraits<Object> {

		@Override
		public void afterPropertiesSet() throws Exception {
			/**
			 * Not used
			 */
		}

		@Override
		public Optional<Object> makeGraphicReportItem(final DataResult r) {
			assertThat(r).isEqualTo(result);
			return Optional.ofNullable(graphicReportItem);
		}

		@Override
		public List<SingleGraphicMaker<Object>> getGraphicMakerList() {
			return List.of(singleGraphicMaker);
		}

	}

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		graphicBaseFileName = faker.numerify("graphicBaseFileName###");
		mgdet = new MultipleGraphicDocumentExporterTraitsImpl();
		when(singleGraphicMaker.makeGraphic(graphicReportItem)).thenReturn(graphicArtifact);
		when(singleGraphicMaker.getBaseFileName()).thenReturn(graphicBaseFileName);
	}

	@AfterEach
	void end() {
		verifyNoMoreInteractions(result, graphicReportItem, singleGraphicMaker, graphicArtifact);
	}

	@Test
	void testToGraphic() {
		final var r = mgdet.toGraphic(result);
		assertThat(r).hasSize(1).contains(graphicArtifact);
		verify(singleGraphicMaker, times(1)).makeGraphic(graphicReportItem);
	}

	@Test
	void testToSingleGraphic() {
		final var r = mgdet.toSingleGraphic(graphicBaseFileName, result);
		assertThat(r).isNotEmpty().contains(graphicArtifact);
		verify(singleGraphicMaker, times(1)).getBaseFileName();
		verify(singleGraphicMaker, times(1)).makeGraphic(graphicReportItem);
	}

	@Test
	void testGetGraphicInternalProducedBaseFileNames() {
		final var r = mgdet.getGraphicInternalProducedBaseFileNames();
		assertThat(r).hasSize(1).contains(graphicBaseFileName);
		verify(singleGraphicMaker, times(1)).getBaseFileName();
	}

}
