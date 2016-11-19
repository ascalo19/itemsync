package ascalo19.itemsync;

import davmail.exchange.VCard;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Item implements Serializable {

	static final long serialVersionUID = 1L;

	private String uid;
	private Map<String, Object> properties;

	public Item() {
		this.uid = null;
		this.properties = new HashMap<String, Object>();
	}

	public Item(String uid) {
		if (StringUtils.isBlank(uid)) {
			throw new IllegalArgumentException("uid cannot be blank");
		}
		this.uid = uid;
		this.properties = new HashMap<String, Object>();
		enforceUid();
	}

	public Item(String uid, Map<String, ? extends Object> properties) {
		this(uid);
		this.properties.putAll(properties);
		enforceUid();
	}

	private void enforceUid() {
		properties.put("uid", uid);
	}

	public String getUid() {
		return uid;
	}

	public Date getLastModifiedDate() {
		Object lastModifiedDate = properties.get("lastModifiedDate");
		if (lastModifiedDate instanceof Date) {
			return (Date) lastModifiedDate;
		} else if (lastModifiedDate != null) {
			return VCard.convertZuluDateToDate(lastModifiedDate.toString());
		}
		return null;
	}

	public Object getProperty(String name) {
		return properties.get(name);
	}

	public void setProperty(String name, Object value) {
		this.properties.put(name, value);
		enforceUid();
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties.putAll(properties);
		enforceUid();
	}

	@Override
	public String toString() {
		return "Item{" + "uid='" + uid + "\' " + properties.get("firstname") + " " + properties.get("lastname") + '}';
	}
}
