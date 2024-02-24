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

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;

public interface MultipleGraphicDocumentExporterTraits<T> extends InitializingBean, GraphicRendererEngine {

	Optional<T> makeGraphicReportItem(DataResult result);

	List<SingleGraphicMaker<T>> getGraphicMakerList();

	@Override
	default Set<String> getGraphicInternalProducedBaseFileNames() {
		return getGraphicMakerList().stream()
				.map(SingleGraphicMaker::getBaseFileName)
				.distinct()
				.collect(toUnmodifiableSet());
	}

	@Override
	default Optional<GraphicArtifact> toSingleGraphic(final String graphicBaseFileName, final DataResult result) {
		return getGraphicMakerList().stream()
				.filter(gm -> gm.getBaseFileName().equalsIgnoreCase(graphicBaseFileName))
				.findFirst()
				.flatMap(gm -> makeGraphicReportItem(result).map(gm::makeGraphic));
	}

	@Override
	default List<GraphicArtifact> toGraphic(final DataResult result) {
		return makeGraphicReportItem(result)
				.stream()
				.flatMap(item -> getGraphicMakerList().stream()
						.map(gm -> gm.makeGraphic(item)))
				.toList();
	}

}
