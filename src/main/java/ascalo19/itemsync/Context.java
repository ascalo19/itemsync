package ascalo19.itemsync;

public class Context {

	private static ThreadLocal<Context> threadLocalContext = null;
	private Configuration conf = null;
	private Log log = null;

	private Context() {
		conf = new Configuration();
		log = new Log();
	}

	public static Context getCurrent() {
		if (threadLocalContext == null) {
			threadLocalContext = new ThreadLocal<Context>();
		}
		if (threadLocalContext.get() == null) {
			threadLocalContext.set(new Context());
		}
		return threadLocalContext.get();
	}

	public static void closeCurrent() {
		if (threadLocalContext == null) {
			threadLocalContext = new ThreadLocal<Context>();
		}
		if (threadLocalContext.get() != null) {
			Context c = threadLocalContext.get();
			// TODO send mail
		}
	}

	public Configuration getConf() {
		return conf;
	}

	public Log getLog() {
		return log;
	}
}
