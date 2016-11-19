package ascalo19.itemsync.impl;

import ascalo19.itemsync.Context;
import ascalo19.itemsync.Item;
import ascalo19.itemsync.MapTransformer;
import ascalo19.itemsync.SynchronizedSystem;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.text.DecimalFormat;
import java.util.*;

public class JdbcSystem extends SynchronizedSystem {

	public static final String UID_PREFIX = "X-FMP-";

	protected String driver;
	protected String from;

	private Connection conn;
	private MapTransformer transformer;
	private List<Item> created;
	private List<Item> updated;
	private List<Item> deleted;

	@Override
	protected void initInternal() throws Exception {

		driver = Context.getCurrent().getConf().getProperty(getClass().getSimpleName().toLowerCase() + ".driver");
		from = Context.getCurrent().getConf().getProperty(getClass().getSimpleName().toLowerCase() + ".from");

		DbUtils.loadDriver(driver);
		conn = DriverManager.getConnection(url, username, password);

		transformer = new MapTransformer(getClass().getSimpleName().toLowerCase()) {
			@Override
			protected <V> V convertToSource(String oldKey, String newKey, V value) {
				if ("daterac".equalsIgnoreCase(oldKey) && value instanceof String) {
					String[] parts = StringUtils.split((String) value, '.');
					if (parts.length == 3) {
						if (NumberUtils.isDigits(parts[0]) && NumberUtils.isDigits(parts[1]) && NumberUtils.isDigits(parts[2])) {
							int day = NumberUtils.toInt(parts[0]);
							int month = NumberUtils.toInt(parts[1]);
							int year = NumberUtils.toInt(parts[2]);
							Calendar cal = Calendar.getInstance();
							cal.set(Calendar.DATE, day);
							cal.set(Calendar.MONTH, month - 1);
							if (year < 100) {
								year += 1900;
							}
							cal.set(Calendar.YEAR, year);
							value = (V) DateUtils.truncate(cal.getTime(), Calendar.DATE);
						}
					}
				}
				if (value instanceof String) {
					String s = StringUtils.trimToNull((String) value);
					value = (V) (s != null ? s.replaceAll("\r\n|\r", "\n") : null);
				}
				return super.convertToSource(oldKey, newKey, value);
			}

			@Override
			protected <V> V convertToTarget(String oldKey, String newKey, V value) {
				if ("daterac".equalsIgnoreCase(newKey) && value instanceof Date) {
					value = (V) DateFormatUtils.format((Date) value, "dd.MM.yyyy");
				}
				if (value instanceof String) {
					String s = StringUtils.trimToNull((String) value);
					value = (V) (s != null ? s.replaceAll("\r\n|\n", "\r") : null);
				}
				return super.convertToTarget(oldKey, newKey, value);
			}
		};

		created = new ArrayList<Item>();
		updated = new ArrayList<Item>();
		deleted = new ArrayList<Item>();

		Map<String, Date> tmpLastModifiedDateMap = new HashMap<String, Date>(lastModifiedDateMap);

		QueryRunner run = new QueryRunner(true);
		MapListHandler h = new MapListHandler();
		for (Map<String, Object> contact : run.query(conn, "SELECT * FROM " + from, h)) {
			if ((created.size() + updated.size() + deleted.size()) >= Context.getCurrent().getConf().getBatchSize()) {
				break;
			}
			String uid = ObjectUtils.toString(contact.get("uid"));
			if (StringUtils.isBlank(uid)) {
				uid = generateUid(contact);
			}
			Item item = new Item(uid, transformer.toSource(contact));
			//System.out.println("CHECK => "+item+" "+tmpLastModifiedDateMap.get(uid));
			Date lastModifiedDate = item.getLastModifiedDate();
			Date previousSyncLastModifiedDate = tmpLastModifiedDateMap.remove(uid);
			if (previousSyncLastModifiedDate == null) {
				created.add(item);
			} else if (!previousSyncLastModifiedDate.equals(lastModifiedDate)) {
				updated.add(item);
			}
			lastModifiedDateMap.put(uid, lastModifiedDate);
		}

		for (String uid : tmpLastModifiedDateMap.keySet()) {
			if ((created.size() + updated.size() + deleted.size()) >= Context.getCurrent().getConf().getBatchSize()) {
				break;
			}
			deleted.add(new Item(uid));
			lastModifiedDateMap.remove(uid);
		}

//		dump();

	}

	private String generateUid(Map<String, Object> contact) {
		return UID_PREFIX + (contact.get("no fiche") != null ? new DecimalFormat("0000000000").format(((Double) contact.get("no fiche")).longValue()) : "");
	}

	@Override
	public List<Item> getCreated() {
		return created;
	}

	@Override
	public List<Item> getUpdated() {
		return updated;
	}

	@Override
	public List<Item> getDeleted() {
		return deleted;
	}

	@Override
	public Item read(String uid) throws Exception {
		Item result = null;
		if (StringUtils.isNotBlank(uid)) {
			List params = new ArrayList();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM " + from + " WHERE ");
			if (StringUtils.startsWith(uid, UID_PREFIX)) {
				params.add(StringUtils.removeStart(uid, UID_PREFIX));
				sql.append("\"no fiche\"=?");
			} else {
				params.add(uid);
				sql.append("\"uid\"=?");
			}
			QueryRunner run = new QueryRunner(true);
			MapListHandler h = new MapListHandler();
			for (Map<String, Object> contact : run.query(conn, sql.toString(), h, params.toArray())) {
				String uid_ = ObjectUtils.toString(contact.get("uid"));
				if (StringUtils.isBlank(uid_)) {
					uid_ = generateUid(contact);
				}
				result = new Item(uid_, transformer.toSource(contact));
				break;
			}
		}
		return result;
	}

	@Override
	public void create(Item item) throws Exception {
		Map<String, Object> properties = transformer.toTarget(item.getProperties());
		properties.remove("adresses_de_concat_ro"); // Hack for read-only field
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + from + " (\"");
		sql.append(StringUtils.join(properties.keySet(), "\",\""));
		sql.append("\") VALUES (");
		sql.append(StringUtils.repeat("?", ",", properties.size()));
		sql.append(")");

		Context.getCurrent().getLog().preCreate(this, item, sql.toString() + "\nbind => " + properties.values() + "\n");

		if (!Context.getCurrent().getConf().isDryrun()) {
			QueryRunner run = new QueryRunner(true);
			run.update(conn, sql.toString(), properties.values().toArray());
			updateLastModifiedDateMap(item);
		}

		Context.getCurrent().getLog().info("Done " + item);
	}

	@Override
	public void update(Item item) throws Exception {

		Item old = read(item.getUid());
		// If birthday will be erased
		if (old.getProperty("birthday") != null && item.getProperty("birthday") == null) {
			// Ignore it and keep old value
			item.setProperty("birthday", old.getProperty("birthday"));
		}

		Map<String, Object> properties = transformer.toTarget(item.getProperties());
		properties.remove("adresses_de_concat_ro"); // Hack for read-only field
		List<String> set = new ArrayList<String>();
		for (String name : properties.keySet()) {
			set.add("\"" + name + "\"=?");
		}
		List params = new ArrayList(properties.values());
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + from + " SET ");
		sql.append(StringUtils.join(set, ","));
		sql.append(" WHERE ");
		if (StringUtils.startsWith(item.getUid(), UID_PREFIX)) {
			params.add(StringUtils.removeStart(item.getUid(), UID_PREFIX));
			sql.append("\"no fiche\"=?");
		} else {
			params.add(item.getUid());
			sql.append("\"uid\"=?");
		}

		Context.getCurrent().getLog().preUpdate(this, old, item, sql.toString() + "\nbind => " + properties.values() + "\n");

		if (!Context.getCurrent().getConf().isDryrun()) {
			QueryRunner run = new QueryRunner(true);
			run.update(conn, sql.toString(), params.toArray());
			updateLastModifiedDateMap(item);
		}

		Context.getCurrent().getLog().info("Done " + item);
	}

	@Override
	public void delete(Item item) throws Exception {
		List params = new ArrayList();
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM " + from + " WHERE ");
		if (StringUtils.startsWith(item.getUid(), UID_PREFIX)) {
			params.add(NumberUtils.createDouble(StringUtils.removeStart(item.getUid(), UID_PREFIX)));
			sql.append("\"no fiche\"=?");
		} else {
			params.add(item.getUid());
			sql.append("\"uid\"=?");
		}

		Context.getCurrent().getLog().preDelete(this, read(item.getUid()), item, sql.toString() + "\nbind => " + params + "\n");

		if (!Context.getCurrent().getConf().isDryrun()) {
			QueryRunner run = new QueryRunner(true);
			run.update(conn, sql.toString(), params.toArray());
			deleteLastModifiedDateMap(item);
		}

		Context.getCurrent().getLog().info("Done " + item);
	}

	@Override
	protected void closeInternal() throws Exception {
		DbUtils.closeQuietly(conn);
	}
}
