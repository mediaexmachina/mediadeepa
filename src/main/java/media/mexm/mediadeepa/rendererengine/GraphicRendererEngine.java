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
package media.mexm.mediadeepa.rendererengine;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.GraphicArtifact;

public interface GraphicRendererEngine {

	List<GraphicArtifact> toGraphic(DataResult result);

	Optional<GraphicArtifact> toSingleGraphic(String graphicBaseFileName, DataResult result);

	Set<String> getGraphicInternalProducedBaseFileNames();

}
