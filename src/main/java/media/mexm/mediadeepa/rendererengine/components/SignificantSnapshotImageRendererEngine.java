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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mediadeepa.ConstStrings;
import media.mexm.mediadeepa.config.AppConfig;
import media.mexm.mediadeepa.exportformat.DataResult;
import media.mexm.mediadeepa.exportformat.ImageArtifact;

@Component
public class SignificantSnapshotImageRendererEngine implements ConstStrings {

	@Autowired
	private AppConfig appConfig;

	public Optional<ImageArtifact> makeimage(final DataResult result) {
		return result.getVideoImageSnapshots()
				.map(vis -> new ImageArtifact(
						"main",
						vis.imageSize(),
						IMAGE_JPEG,
						vis.significantImageData()));
	}

	public String getDefaultInternalFileName() {
		return appConfig.getSnapshotImageConfig().getSignificantJpegFilename();
	}

}
