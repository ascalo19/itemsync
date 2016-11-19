package ascalo19.itemsync;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.io.IOUtils;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;

public abstract class SynchronizedSystem {

	protected String url;
	protected String username;
	protected String password;

	protected Map<String, Date> lastModifiedDateMap;

	public abstract List<Item> getCreated();

	public abstract List<Item> getUpdated();

	public abstract List<Item> getDeleted();

	public abstract Item read(String uid) throws Exception;

	public abstract void create(Item item) throws Exception;

	public abstract void update(Item item) throws Exception;

	public abstract void delete(Item item) throws Exception;

	public final SynchronizedSystem init() throws Exception {

		url = Context.getCurrent().getConf().getUrl(getClass());
		username = Context.getCurrent().getConf().getUsername(getClass());
		password = Context.getCurrent().getConf().getPassword(getClass());


		File file = new File(getLastModifiedDateFileName());
		if (file.exists() && file.canRead()) {
			XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
			lastModifiedDateMap = (HashMap) d.readObject();
			d.close();
		} else {
			lastModifiedDateMap = new HashMap<String, Date>();
		}
		initInternal();
		return this;
	}

	public final void close() throws Exception {
		closeInternal();
		if (!Context.getCurrent().getConf().isDryrun()) {
			XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(getLastModifiedDateFileName())));
			e.writeObject(lastModifiedDateMap);
			e.close();
		}
	}

	protected void updateLastModifiedDateMap(Item item) throws Exception {
		Item lastModified = read(item.getUid());
		lastModifiedDateMap.put(lastModified.getUid(), lastModified.getLastModifiedDate());
	}

	protected void deleteLastModifiedDateMap(Item item) throws Exception {
		lastModifiedDateMap.remove(item.getUid());
	}

	protected void dump() {
		BufferedWriter dumpFile = null;
		try {
			dumpFile = new BufferedWriter(new FileWriter("dump-" + getClass().getSimpleName() + ".txt"));
			List<Item> data = new ArrayList<Item>();
			data.addAll(getCreated());
			data.addAll(getUpdated());
			data.addAll(getDeleted());
			Collections.sort(data, new BeanComparator("uid"));
			for (Item i : data) {
				dumpFile.write("-----" + i.getUid() + "-----");
				dumpFile.newLine();
				TreeMap<String, Object> attrs = new TreeMap(i.getProperties());
				for (Map.Entry<String, Object> a : attrs.entrySet()) {
					dumpFile.write(a.getKey() + "=" + a.getValue());
					dumpFile.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		IOUtils.closeQuietly(dumpFile);
	}

	private String getLastModifiedDateFileName() {
		return "LastModifiedDate" + getClass().getSimpleName() + ".xml";
	}

	protected abstract void initInternal() throws Exception;

	protected abstract void closeInternal() throws Exception;
}
