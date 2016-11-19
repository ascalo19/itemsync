package ascalo19.itemsync.cli;

import ascalo19.itemsync.Context;
import ascalo19.itemsync.impl.JdbcSystem;
import ascalo19.itemsync.Configuration;
import ascalo19.itemsync.SynchronizationEngine;
import ascalo19.itemsync.SynchronizedSystem;
import ascalo19.itemsync.impl.CardDavSystem;
import org.apache.commons.lang3.StringEscapeUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Launcher {

	public final static PrintStream stdout = System.out;
	public final static PrintStream stderr = System.err;

	@Option(name = "-b", aliases = "--batch-size", usage = "max entries to process in one run, default 50")
	private int batchSize = 50;
	@Option(name = "-d", aliases = "--dry-run", usage = "only perform a simulation")
	private boolean dryrun = false;
	@Option(name = "-v", aliases = "--verbose", usage = "verbose output")
	private boolean verbose = false;

	public static void main(String[] args) {

		Launcher launcher = new Launcher();
		CmdLineParser parser = new CmdLineParser(launcher);

		try {

			parser.parseArgument(args);
			if (launcher.verbose) {
				StringBuilder log = new StringBuilder();
				log.append("\nitemsync called with the following parameters:");
				log.append("\nbatchSize=");
				log.append(launcher.batchSize);
				log.append("\ndryrun=");
				log.append(launcher.dryrun);
				log.append("\nverbose=");
				log.append(launcher.verbose);
				msg(log.toString());
			}

			launcher.run();
			System.exit(0);

		} catch (CmdLineException e) {
			StringWriter text = new StringWriter();
			text.append(e.getMessage()).append("\n\n");
			text.append("Usage: itemsync [options]\n\n");
			parser.printUsage(text, null);
			msg(text.toString(), true);
			System.exit(1);
		} catch (Exception e) {
			StringWriter text = new StringWriter();
			text.append("\nitemsync ERROR : ").append(e.getMessage()).append("\n");
			if (launcher.verbose) {
				e.printStackTrace(new PrintWriter(text));
			}
			msg(text.toString(), true);
			System.exit(1);
		}
	}

	private static void msg(String text) {
		msg(text, false);
	}

	private static void msg(String text, boolean isError) {
		if (isError) {
			stderr.println(text);
		} else {
			stdout.println(text);
		}
	}

	private static String escapeHtml(String text) {
		return StringEscapeUtils.escapeHtml4(text).replace("\r\n", "<br>").replace("\n", "<br>");
	}

	public void run() throws Exception {

		try {

			Configuration conf = Context.getCurrent().getConf();
			conf.setBatchSize(batchSize);
			conf.setDryrun(dryrun);

			SynchronizedSystem jdbc = new JdbcSystem().init();
			SynchronizedSystem cardDav = new CardDavSystem().init();
			SynchronizationEngine engine = new SynchronizationEngine(jdbc, cardDav);
			engine.run();
			jdbc.close();
			cardDav.close();

		} finally {
			Context.closeCurrent();
		}

	}

}
