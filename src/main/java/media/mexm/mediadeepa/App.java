/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either command 3 of the License, or
 * any later command.
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

import static media.mexm.mediadeepa.components.CLIRunner.EXIT_CODE_GENERATE_DOC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
	public static final String NAME = "mediadeepa";

	public static void main(final String[] args) {
		setDefaultProps();
		final var exitCode = SpringApplication.exit(SpringApplication.run(App.class, args));
		if (exitCode == EXIT_CODE_GENERATE_DOC) {
			return;
		}
		System.exit(exitCode);
	}

	public static void setDefaultProps() {
		System.setProperty("spring.main.web-application-type", "NONE");
		System.setProperty("spring.main.banner-mode", "off");
		System.setProperty("spring.main.log-startup-info", "false");
	}

}
