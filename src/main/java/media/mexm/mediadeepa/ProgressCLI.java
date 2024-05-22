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

import static java.util.Locale.US;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.io.PrintStream;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public class ProgressCLI {

	private static final int WIDTH = 20;
	private static final char PROGRESS = '=';
	private static final char BLANK = ' ';

	private final PrintStream out;
	private long startTime;
	private String lastEntry;
	private boolean ended;

	public ProgressCLI(final PrintStream out) {
		this.out = out;
		ended = false;
	}

	private String makePercent(final double value) {
		return StringUtils.right("    " + Math.round(value * 100d) + "%", 4);
	}

	private String makeETA(final double value) {
		if (value == 0d) {
			return "";
		}
		final var agoSec = (System.currentTimeMillis() - startTime) / 1000d;
		final var eta = Duration.ofSeconds(Math.round(agoSec / value * (1d - value)));

		return " ETA " +
			   StringUtils.right("0" + eta.toHoursPart(), 2) + ":" +
			   StringUtils.right("0" + eta.toMinutesPart(), 2) + ":" +
			   StringUtils.right("0" + eta.toSecondsPart(), 2);
	}

	private String makeSpeed(final float speed) {
		return ", x" + String.format(US, "%02.1f", speed);
	}

	private void write(final String value) {
		if (value.equals(lastEntry)) {
			return;
		}
		lastEntry = value;
		out.print(" ");
		out.print(value);
		out.print("\r");
		out.flush();
	}

	public void displayProgress(final double value, final float speed) {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
		}
		if (value >= 1 && ended == false) {
			end();
			return;
		}

		final var pSize = Math.round((float) Math.floor(value * WIDTH));
		final var bSize = WIDTH - pSize;

		write("|" + repeat(PROGRESS, pSize) + repeat(BLANK, bSize) + "|" +
			  makePercent(value)
			  + makeETA(value)
			  + makeSpeed(speed));
	}

	public void end() {
		ended = true;
		final var eta = Duration.ofMillis(System.currentTimeMillis() - startTime);
		final var totalTime = ", total time: " +
							  StringUtils.right("0" + eta.toHoursPart(), 2) + ":" +
							  StringUtils.right("0" + eta.toMinutesPart(), 2) + ":" +
							  StringUtils.right("0" + eta.toSecondsPart(), 2);
		write("|" + repeat(PROGRESS, WIDTH) + "|" + makePercent(1) + totalTime);
		if (SystemUtils.IS_OS_WINDOWS == false) {
			out.println();
		}
	}
}
