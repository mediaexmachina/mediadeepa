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
package media.mexm.mediadeepa;

import static org.apache.commons.lang3.StringUtils.repeat;

import java.io.PrintWriter;

public class ProgressCLI {

	private static final int WIDTH = 20;
	private static final char PROGRESS = '=';
	private static final char BLANK = ' ';

	private final PrintWriter out;

	public ProgressCLI(final PrintWriter out) {
		this.out = out;
	}

	public void startProgress() {
		out.print("|");
		out.print(repeat(BLANK, WIDTH));
		out.print("|");
		out.print("\r");
		out.flush();
	}

	// TODO manage last value / duplicate

	public void displayProgress(final double value) {
		if (value <= 0) {
			out.print("|");
			out.print(repeat(BLANK, WIDTH));
			out.print("|");
			out.print("\r");
			out.flush();
			return;
		}
		if (value >= 1) {
			out.print("|");
			out.print(repeat(PROGRESS, WIDTH));
			out.print("|");
			out.print("\r");
			out.flush();
			return;
		}

		final var pSize = Math.round((float) Math.floor(value * WIDTH));
		final var bSize = WIDTH - pSize;

		out.print("|");
		out.print(repeat(PROGRESS, pSize));
		out.print(repeat(BLANK, bSize));
		out.print("|");
		out.print("\r");
		out.flush();
	}

	public void endsProgress() {
		out.print("|");
		out.print(repeat(PROGRESS, WIDTH));
		out.println("|");
	}

}
