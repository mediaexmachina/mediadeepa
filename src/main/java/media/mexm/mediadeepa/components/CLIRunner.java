/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either command 3 of the License, or
 * any later command.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@Component
public class CLIRunner implements CommandLineRunner, ExitCodeGenerator {

	@Autowired
	private IFactory factory;
	@Autowired
	private MediaDeepACommand command;

	private int exitCode;

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(final String... args) throws Exception {
		exitCode = new CommandLine(command, factory).execute(args);// TODO multiple commands ?
	}

}
