package ascalo19.itemsync;

import davmail.exchange.VCard;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MapTransformer {

	private Properties conf = null;

	public MapTransformer(String confName) throws IOException {
		conf = new Properties();
		conf.load(getClass().getResourceAsStream("/" + confName + ".properties"));
	}

	public <V> Map<String, V> toSource(Map<String, V> target) {
		Properties reverse = new Properties();
		for (String name : conf.stringPropertyNames()) {
			reverse.setProperty(conf.getProperty(name), name);
		}
		Map<String, V> result = new HashMap<String, V>();
		for (String key : reverse.stringPropertyNames()) {
			String newKey = reverse.getProperty(key);
			if (StringUtils.isNotBlank(newKey)) {
				result.put(newKey, convertToSource(key, newKey, target.get(key)));
			}
		}
		return result;
	}

	protected <V> V convertToSource(String oldKey, String newKey, V value) {
		return value;
	}

	public <V> Map<String, V> toTarget(Map<String, V> source) {
		Map<String, V> result = new HashMap<String, V>();
		for (String key : conf.stringPropertyNames()) {
			String newKey = conf.getProperty(key);
			if (StringUtils.isNotBlank(newKey)) {
				V value = source.get(key);
				if ((("bday".equalsIgnoreCase(newKey) && "birthday".equalsIgnoreCase(key)) || ("anniversary".equalsIgnoreCase(newKey) && "anniversary".equalsIgnoreCase(key)) || ("lastmodified".equalsIgnoreCase(newKey) && "lastModifiedDate".equalsIgnoreCase(key))) && (value instanceof Date)) {
					value = (V) VCard.convertDateToZuluDate((Date) value);
				}
				result.put(newKey, convertToTarget(key, newKey, value));
			}
		}
		return result;
	}

	protected <V> V convertToTarget(String oldKey, String newKey, V value) {
		return value;
	}
}
