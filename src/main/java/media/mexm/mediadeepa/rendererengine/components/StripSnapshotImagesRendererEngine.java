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
 * Copyright (C) hdsdi3g for hd3g.tv 2024
 *
 */
package media.mexm.mediadeepa.rendererengine.components;

import static media.mexm.mediadeepa.exportformat.ImageArtifact.IMAGE_JPEG;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ImageArtifact;

@Component
@Slf4j
public class StripSnapshotImagesRendererEngine implements ConstStrings {

	@Autowired
	private AppConfig appConfig;

	public Optional<List<ImageArtifact>> makeimages(final DataResult result) {
		final var oResult = result.getVideoImageSnapshots()
				.map(vis -> {
					final var stripList = Optional.ofNullable(vis.stripImagesData())
							.orElse(List.of());

					return IntStream.range(0, stripList.size())
							.mapToObj(i -> new ImageArtifact(
									"strip" + i,
									vis.imageSize(),
									IMAGE_JPEG,
									stripList.get(i)))
							.toList();
				});

		if (oResult.isEmpty() || oResult.get().isEmpty()) {
			return Optional.empty();
		}

		return oResult;
	}

	public String getDefaultInternalFileName() {
		return appConfig.getSnapshotImageConfig().getStripJpegFilename();
	}

}
