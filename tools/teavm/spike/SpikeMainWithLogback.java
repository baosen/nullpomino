package nullpomino.teavmspike;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

/**
 * TeaVM feasibility spike, logback variant: drives the same
 * SAX-XML-parsing, reflection-based Joran configuration path
 * NullpoMinoWeb.java exercises today (LogConfig.configureFromResource),
 * to isolate whether a TeaVM failure comes from logback/Joran rather than
 * from the game logic itself (see SpikeMain).
 */
public final class SpikeMainWithLogback {
	private SpikeMainWithLogback() {}

	private static final String CONFIG_XML = "<configuration>"
			+ "<appender name=\"CONSOLE\" class=\"ch.qos.logback.core.ConsoleAppender\">"
			+ "<encoder><pattern>%msg%n</pattern></encoder>"
			+ "</appender>"
			+ "<root level=\"INFO\"><appender-ref ref=\"CONSOLE\"/></root>"
			+ "</configuration>";

	public static void main(String[] args) throws Exception {
		System.out.println("=== SpikeMainWithLogback start ===");
		configureLogback();
		Logger log = LoggerFactory.getLogger(SpikeMainWithLogback.class);
		log.info("hello from logback+Joran under TeaVM");
		SpikeMain.exerciseGameLogic();
		System.out.println("=== SpikeMainWithLogback done ===");
	}

	private static void configureLogback() throws Exception {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset();
		configurator.doConfigure(new ByteArrayInputStream(CONFIG_XML.getBytes(StandardCharsets.UTF_8)));
		System.out.println("logback configured via Joran");
	}
}
