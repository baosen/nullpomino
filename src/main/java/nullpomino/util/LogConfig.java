// SPDX-FileCopyrightText: 2026 baosen
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.File;
import java.io.InputStream;

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

	/** Set once a configuration has been applied; later configure() calls no-op. */
	private static boolean configured;

	/**
	 * Configure logback from an XML file. If the file does not exist,
	 * logback's default configuration (console) stays active so errors
	 * remain visible. No-op when a configuration was already applied via
	 * {@link #configureFromResource} (the web entry point configures logging
	 * before handing off to the regular main).
	 * @param path Path to a logback XML configuration file
	 */
	public static void configure(String path) {
		if(configured) return;
		File file = new File(path);
		if(!file.isFile()) {
			System.err.println("LogConfig: " + path + " not found; using logback defaults");
			return;
		}
		try {
			doConfigure(new JoranConfigurator(), file);
			configured = true;
		} catch (JoranException e) {
			// details land in the context status, printed below
		}
	}

	/**
	 * Configure logback from a classpath resource. Used by the web entry
	 * point, where the config tree lives on a virtual filesystem but the
	 * logging setup should ship inside the jar.
	 * @param resourcePath absolute resource path (e.g. "/web/logback-web.xml")
	 */
	public static void configureFromResource(String resourcePath) {
		try(InputStream in = LogConfig.class.getResourceAsStream(resourcePath)) {
			if(in == null) {
				System.err.println("LogConfig: resource " + resourcePath + " not found; using logback defaults");
				return;
			}
			doConfigure(new JoranConfigurator(), in);
			configured = true;
		} catch (JoranException e) {
			// details land in the context status, printed below
		} catch (java.io.IOException e) {
			System.err.println("LogConfig: failed to read " + resourcePath + ": " + e);
		}
	}

	private static void doConfigure(JoranConfigurator configurator, Object source) throws JoranException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		configurator.setContext(context);
		context.reset();
		try {
			if(source instanceof File) {
				configurator.doConfigure((File) source);
			} else {
				configurator.doConfigure((InputStream) source);
			}
		} finally {
			new StatusPrinter2().printInCaseOfErrorsOrWarnings(context);
		}
	}
}
