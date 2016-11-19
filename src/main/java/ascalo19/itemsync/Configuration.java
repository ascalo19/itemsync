package ascalo19.itemsync;

import java.io.IOException;
import java.util.Properties;

public class Configuration extends Properties {

	private int batchSize = Integer.MAX_VALUE;
	private boolean dryrun = false;

	public Configuration() {
		try {
			load(getClass().getResourceAsStream("/config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getUrl(Class<? extends SynchronizedSystem> type) {
		return getProperty(type.getSimpleName().toLowerCase() + ".url");
	}

	public String getUsername(Class<? extends SynchronizedSystem> type) {
		return getProperty(type.getSimpleName().toLowerCase() + ".username");
	}

	public String getPassword(Class<? extends SynchronizedSystem> type) {
		return getProperty(type.getSimpleName().toLowerCase() + ".password");
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean isDryrun() {
		return dryrun;
	}

	public void setDryrun(boolean dryrun) {
		this.dryrun = dryrun;
	}

}
