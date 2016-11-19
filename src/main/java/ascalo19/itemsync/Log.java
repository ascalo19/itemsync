package ascalo19.itemsync;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Log {

	private final static Logger LOGGER = Logger.getLogger("MainLog");

	public Log() {
		FileInputStream logConfig = null;
		try {
			logConfig = new FileInputStream("logging.properties");
			LogManager.getLogManager().readConfiguration(logConfig);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(logConfig);
		}
	}

	public void info(String msg) {
		LOGGER.info(msg);
	}

	public void preCreate(SynchronizedSystem system, Item item, String query) {
		preAction("Creating", system, null, item, query);
	}

	public void preUpdate(SynchronizedSystem system, Item old, Item item, String query) {
		preAction("Updating", system, old, item, query);
	}

	public void preDelete(SynchronizedSystem system, Item old, Item item, String query) {
		preAction("Deleting", system, old, item, query);
	}

	public void preAction(String action, SynchronizedSystem system, Item old, Item item, String query) {
		StringBuilder msg = new StringBuilder();
		if (Context.getCurrent().getConf().isDryrun()) {
			msg.append("\n*** DRYRUN / SIMULATION ONLY ***\n");
		}
		msg.append(action).append(" item ").append(item).append(" in ").append(system.getClass().getSimpleName()).append("\n");
		msg.append("-----BEGIN PROPERTIES-----\n");
		Map<String, Object> props = new TreeMap(old != null ? old.getProperties() : item.getProperties());
		for (Map.Entry<String, Object> e : props.entrySet()) {
			Object o = e.getValue();
			Object n = item.getProperty(e.getKey());
			String oStr = o != null ? o.toString().replace("\n", "\\n") : "";
			String nStr = n != null ? n.toString().replace("\n", "\\n") : "";
			if ("lastModifiedDate".equalsIgnoreCase(e.getKey())) {
				oStr += o != null ? " (" + ((Date) o).getTime() + ")" : "";
				nStr += n != null ? " (" + ((Date) n).getTime() + ")" : "";
			}
			if (ObjectUtils.equals(n, o)) {
				msg.append(e.getKey());
				msg.append(" : ");
				msg.append(oStr);
				msg.append("\n");
			} else {
				msg.append("*");
				msg.append(e.getKey());
				msg.append(" : ");
				msg.append(oStr);
				msg.append(" => ");
				msg.append(nStr);
				msg.append("\n");
			}
		}
		msg.append("-----END PROPERTIES-----\n");
		msg.append("-----BEGIN QUERY-----\n");
		msg.append(query);
		msg.append("-----END QUERY-----");
		info(msg.toString());
	}
}
