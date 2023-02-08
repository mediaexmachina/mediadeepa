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
 * Copyright (C) hdsdi3g for hd3g.tv 2023
 *
 */
package media.mexm.mediadeepa;

import java.io.InputStream;
import java.util.Objects;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeyPressToExit implements Runnable {
	private static final Logger log = LogManager.getLogger();

	private final InputStream source;

	public KeyPressToExit(final InputStream source) {
		this.source = Objects.requireNonNull(source, "\"source\" can't to be null");
	}

	@Override
	public void run() {
		log.info("Press [ENTER] to quit...");
		final var keyboard = new Scanner(source);
		keyboard.nextLine();
		keyboard.close();
		log.info("Wait to exit...");
		System.exit(0);
	}

	public void start() {
		final var consoleT = new Thread(this);
		consoleT.setDaemon(true);
		consoleT.setPriority(Thread.MIN_PRIORITY);
		consoleT.setName("Wait to console key press");
		consoleT.start();
	}

}
