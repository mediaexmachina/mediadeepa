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
package media.mexm.mediadeepa;

import java.io.InputStream;
import java.util.Objects;
import java.util.Scanner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeyPressToExit implements Runnable {
	private final InputStream source;
	private final Thread consoleT;

	public KeyPressToExit(final InputStream source) {
		this.source = Objects.requireNonNull(source, "\"source\" can't to be null");
		consoleT = new Thread(this);
	}

	@Override
	public void run() {
		log.info("Press [ENTER] to quit...");
		final var keyboard = new Scanner(source);
		keyboard.nextLine();
		keyboard.close();
		log.info("Wait to exit...");
		exit();
	}

	public void start() {
		consoleT.setDaemon(true);
		consoleT.setPriority(Thread.MIN_PRIORITY);
		consoleT.setName("Wait to console key press");
		consoleT.start();
	}

	protected void exit() {
		System.exit(0);
	}

}
