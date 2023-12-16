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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;

@Data
public class RunnedJavaCmdLine {

	private String appRunner;
	private String args;
	private String hostname;
	private String username;
	private String workingDir;
	private RunnedJavaCmdLine archiveJavaCmdLine;

	public String makeFullExtendedCommandline() {
		return username + "@" + hostname + ":" + workingDir.replace("\\", "/") + "$ " + appRunner + " " + args;
	}

	public Optional<String> makeArchiveCommandline() {
		return Optional.ofNullable(archiveJavaCmdLine)
				.map(RunnedJavaCmdLine::makeFullExtendedCommandline);
	}

	static String escape(final String value) {
		final var result = value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
		if (result.contains(" ")) {
			return "\"" + result + "\"";
		}
		return result;
	}

	public void setup(final String[] args, final String appRunner) {
		this.appRunner = appRunner;
		this.args = Stream.of(args)
				.map(RunnedJavaCmdLine::escape)
				.collect(Collectors.joining(" "));
		workingDir = new File("").getAbsolutePath();
		username = System.getProperties().getProperty("user.name");
		hostname = Optional.ofNullable(System.getenv().get("COMPUTERNAME"))
				.or(() -> Optional.ofNullable(System.getenv().get("HOSTNAME")))
				.orElseGet(() -> {
					try {
						return InetAddress.getLocalHost().getHostName();
					} catch (final UnknownHostException e) {
						return "localhost";
					}
				});
	}

}
