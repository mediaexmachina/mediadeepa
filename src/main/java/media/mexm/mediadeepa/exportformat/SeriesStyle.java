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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

import lombok.Data;

@Data
public class SeriesStyle {

	private final String name;
	private final Paint paint;
	private final Stroke stroke;

	public String getCSSColor() {
		if (paint instanceof final Color c) {
			return "rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")";
		}
		return "#FFF";
	}

}
