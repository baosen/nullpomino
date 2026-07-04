// SPDX-FileCopyrightText: 2026 baosen
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.File;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter2;

/**
 * Loads a logback configuration from a filesystem path relative to the
 * working directory, like the rest of config/. Replaces log4j's
 * PropertyConfigurator.configure for the various entry points.
 */
public class LogConfig {
	private LogConfig() {}

	/**
	 * Configure logback from an XML file. If the file does not exist,
	 * logback's default configuration (console) stays active so errors
	 * remain visible.
	 * @param path Path to a logback XML configuration file
	 */
	public static void configure(String path) {
		File file = new File(path);
		if(!file.isFile()) {
			System.err.println("LogConfig: " + path + " not found; using logback defaults");
			return;
		}
		try {
			LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			try {
				configurator.doConfigure(file);
			} finally {
				new StatusPrinter2().printInCaseOfErrorsOrWarnings(context);
			}
		} catch (JoranException e) {
			// details land in the context status, printed above
		}
	}
}
