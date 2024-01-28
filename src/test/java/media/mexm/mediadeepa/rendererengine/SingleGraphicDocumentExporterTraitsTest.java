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
 * Copyright (C) Media ex Machina 2024
 *
 */
package media.mexm.mediadeepa.rendererengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;
import net.datafaker.Faker;

class SingleGraphicDocumentExporterTraitsTest {

	static Faker faker = net.datafaker.Faker.instance();

	String singleUniqGraphicBaseFileName;
	SingleGraphicDocumentExporterTraitsImpl s;

	@Mock
	GraphicArtifact graphicArtifact;
	@Mock
	DataResult dataResult;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		singleUniqGraphicBaseFileName = faker.numerify("singleUniqTabularDocumentBaseFileName###");
		s = new SingleGraphicDocumentExporterTraitsImpl();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				graphicArtifact,
				dataResult);
	}

	class SingleGraphicDocumentExporterTraitsImpl implements SingleGraphicDocumentExporterTraits {

		@Override
		public List<GraphicArtifact> toGraphic(final DataResult result) {
			assertEquals(dataResult, result);
			return List.of(graphicArtifact, mock(GraphicArtifact.class));
		}

		@Override
		public String getSingleUniqGraphicBaseFileName() {
			return singleUniqGraphicBaseFileName;
		}

	}

	@Test
	void testToSingleGraphic() {
		final var internalGraphicBaseFileName = faker.numerify("internalGraphicBaseFileName###");
		final var oGA = s.toSingleGraphic(internalGraphicBaseFileName, dataResult);
		assertThat(oGA).contains(graphicArtifact);
	}

	@Test
	void testGetGraphicInternalProducedBaseFileNames() {
		assertThat(s.getGraphicInternalProducedBaseFileNames()).isEqualTo(Set.of(singleUniqGraphicBaseFileName));
	}

}
