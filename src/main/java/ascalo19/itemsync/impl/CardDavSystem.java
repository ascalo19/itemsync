package ascalo19.itemsync.impl;

import ascalo19.itemsync.Context;
import davmail.exchange.ICSBufferedReader;
import davmail.exchange.VCard;
import davmail.exchange.VProperty;
import ascalo19.itemsync.Item;
import ascalo19.itemsync.MapTransformer;
import ascalo19.itemsync.SynchronizedSystem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.Consts;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class CardDavSystem extends SynchronizedSystem {

	private Executor executor;
	private MapTransformer transformer;
	private List<Item> created;
	private List<Item> updated;
	private List<Item> deleted;

	@Override
	protected void initInternal() throws Exception {
		executor = Executor.newInstance().auth(username, password);
		transformer = new MapTransformer(getClass().getSimpleName().toLowerCase()) {
			@Override
			protected <V> V convertToSource(String oldKey, String newKey, V value) {
				if ((("bday".equalsIgnoreCase(oldKey) && "birthday".equalsIgnoreCase(newKey)) || ("anniversary".equalsIgnoreCase(oldKey) && "anniversary".equalsIgnoreCase(newKey))) && !(value instanceof Date)) {
					value = (V) VCard.convertZuluDateToDate(ObjectUtils.toString(value));
					if (value != null) {
						value = (V) DateUtils.truncate(value, Calendar.DAY_OF_MONTH);
					}
				}
				if (("lastmodified".equalsIgnoreCase(oldKey) && "lastModifiedDate".equalsIgnoreCase(newKey)) && !(value instanceof Date)) {
					value = (V) VCard.convertZuluDateToDate(ObjectUtils.toString(value));
				}
				if (value instanceof String) {
					value = (V) StringUtils.trimToNull((String) value);
				}
				return super.convertToSource(oldKey, newKey, value);
			}

			@Override
			protected <V> V convertToTarget(String oldKey, String newKey, V value) {
				if ((("bday".equalsIgnoreCase(newKey) && "birthday".equalsIgnoreCase(oldKey)) || ("anniversary".equalsIgnoreCase(newKey) && "anniversary".equalsIgnoreCase(oldKey))) && (value instanceof Date)) {
					value = (V) VCard.convertZuluDateToBday(VCard.convertDateToZuluDate((Date) value));
				}
				if (("lastmodified".equalsIgnoreCase(newKey) && "lastModifiedDate".equalsIgnoreCase(oldKey)) && (value instanceof Date)) {
					value = (V) VCard.convertDateToZuluDate((Date) value);
				}
				return super.convertToTarget(oldKey, newKey, value);
			}
		};

		created = new ArrayList<Item>();
		updated = new ArrayList<Item>();
		deleted = new ArrayList<Item>();

		Map<String, Date> tmpLastModifiedDateMap = new HashMap<String, Date>(lastModifiedDateMap);

		InputStream stream = null;
		BufferedReader reader = null;
		try {
			stream = executor.execute(Request.Get(url)).returnContent().asStream();
			reader = new ICSBufferedReader(new InputStreamReader(stream));

			String line = null;
			while ((line = reader.readLine()) != null) {
				if ((created.size() + updated.size() + deleted.size()) >= Context.getCurrent().getConf().getBatchSize()) {
					break;
				}
				if (line.startsWith(VCard.BEGIN_TAG)) {
					VCard contact = new VCard();
					contact.load(new VProperty(line), reader);

					Item item = new Item(contact.getUid(), transformer.toSource(contact));

					Date lastModifiedDate = item.getLastModifiedDate();
					Date previousSyncLastModifiedDate = tmpLastModifiedDateMap.remove(contact.getUid());
					if (previousSyncLastModifiedDate == null) {
						created.add(item);
					} else if (!previousSyncLastModifiedDate.equals(lastModifiedDate)) {
						updated.add(item);
					}
					lastModifiedDateMap.put(contact.getUid(), lastModifiedDate);

				}
			}

			for (String uid : tmpLastModifiedDateMap.keySet()) {
				if ((created.size() + updated.size() + deleted.size()) >= Context.getCurrent().getConf().getBatchSize()) {
					break;
				}
				deleted.add(new Item(uid));
				lastModifiedDateMap.remove(uid);
			}

		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(stream);
		}

//		dump();

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
			String cardUrl = url + uid + ".vcf";
			InputStream stream = null;
			BufferedReader reader = null;
			try {
				stream = executor.execute(Request.Get(cardUrl)).returnContent().asStream();
				reader = new ICSBufferedReader(new InputStreamReader(stream));

				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith(VCard.BEGIN_TAG)) {
						VCard contact = new VCard();
						contact.load(new VProperty(line), reader);
						result = new Item(contact.getUid(), transformer.toSource(contact));
						break;
					}
				}
			} finally {
				IOUtils.closeQuietly(reader);
				IOUtils.closeQuietly(stream);
			}
		}
		return result;
	}

	@Override
	public void create(Item item) throws Exception {
		Map<String, String> properties = new HashMap<String, String>();
		for (Map.Entry<String, Object> property : transformer.toTarget(item.getProperties()).entrySet()) {
			properties.put(property.getKey(), ObjectUtils.toString(property.getValue()));
		}

		forceCN(properties);

		VCard contact = new VCard();
		contact.putAll(properties);
		String cardUrl = url + item.getUid() + ".vcf";
		String body = contact.getBody();
		Context.getCurrent().getLog().preCreate(this, item, cardUrl + "\n" + body);

		if (!Context.getCurrent().getConf().isDryrun()) {
			executor.execute(Request.Put(cardUrl).useExpectContinue().bodyString(body, ContentType.create("text/vcard", Consts.UTF_8)));
			updateLastModifiedDateMap(item);
		}

		Context.getCurrent().getLog().info("Done " + item);
	}

	@Override
	public void update(Item item) throws Exception {
		Map<String, String> properties = new HashMap<String, String>();
		for (Map.Entry<String, Object> property : transformer.toTarget(item.getProperties()).entrySet()) {
			properties.put(property.getKey(), ObjectUtils.toString(property.getValue()));
		}

		forceCN(properties);

		VCard contact = new VCard();
		contact.putAll(properties);
		String cardUrl = url + item.getUid() + ".vcf";
		String body = contact.getBody();
		Context.getCurrent().getLog().preUpdate(this, read(item.getUid()), item, cardUrl + "\n" + body);

		if (!Context.getCurrent().getConf().isDryrun()) {
			executor.execute(Request.Put(cardUrl).useExpectContinue().bodyString(body, ContentType.create("text/vcard", Consts.UTF_8)));
			updateLastModifiedDateMap(item);
		}

		Context.getCurrent().getLog().info("Done " + item);
	}

	private void forceCN(Map<String, String> properties) {
		String cn = StringUtils.defaultIfBlank(properties.get("sn"), "");
		if (StringUtils.isNotBlank(cn)) {
			cn += ", ";
		}
		cn += StringUtils.defaultIfBlank(properties.get("givenName"), "");
		if (StringUtils.isNotBlank(cn)) {
			cn += " ";
		}
		cn += StringUtils.defaultIfBlank(properties.get("middlename"), "");
		if (StringUtils.isNotBlank(cn)) {
			cn += " ";
		}
		cn += StringUtils.defaultIfBlank(properties.get("namesuffix"), "");
		properties.put("cn", cn);
	}

	@Override
	public void delete(Item item) throws Exception {
		String cardUrl = url + item.getUid() + ".vcf";
		Context.getCurrent().getLog().preDelete(this, read(item.getUid()), item, cardUrl);

		if (!Context.getCurrent().getConf().isDryrun()) {
			executor.execute(Request.Delete(cardUrl));
			deleteLastModifiedDateMap(item);
		}

		Context.getCurrent().getLog().info("Done " + item);
	}

	@Override
	protected void closeInternal() throws Exception {

	}
}
