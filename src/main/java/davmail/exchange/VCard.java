package davmail.exchange;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class VCard extends HashMap<String, String> {

	public static final String BEGIN_TAG = "BEGIN:VCARD";
	public static final String END_TAG = "END:VCARD";

	public static final SimpleTimeZone GMT_TIMEZONE = new SimpleTimeZone(0, "GMT");
	public static final Set<String> CONTACT_ATTRIBUTES = new HashSet<String>();

	static {
		CONTACT_ATTRIBUTES.add("imapUid");
		CONTACT_ATTRIBUTES.add("etag");
		CONTACT_ATTRIBUTES.add("urlcompname");

		CONTACT_ATTRIBUTES.add("extensionattribute1");
		CONTACT_ATTRIBUTES.add("extensionattribute2");
		CONTACT_ATTRIBUTES.add("extensionattribute3");
		CONTACT_ATTRIBUTES.add("extensionattribute4");
		CONTACT_ATTRIBUTES.add("bday");
		CONTACT_ATTRIBUTES.add("anniversary");
		CONTACT_ATTRIBUTES.add("businesshomepage");
		CONTACT_ATTRIBUTES.add("personalHomePage");
		CONTACT_ATTRIBUTES.add("cn");
		CONTACT_ATTRIBUTES.add("co");
		CONTACT_ATTRIBUTES.add("department");
		CONTACT_ATTRIBUTES.add("smtpemail1");
		CONTACT_ATTRIBUTES.add("smtpemail2");
		CONTACT_ATTRIBUTES.add("smtpemail3");
		CONTACT_ATTRIBUTES.add("facsimiletelephonenumber");
		CONTACT_ATTRIBUTES.add("givenName");
		CONTACT_ATTRIBUTES.add("homeCity");
		CONTACT_ATTRIBUTES.add("homeCountry");
		CONTACT_ATTRIBUTES.add("homePhone");
		CONTACT_ATTRIBUTES.add("homePostalCode");
		CONTACT_ATTRIBUTES.add("homeState");
		CONTACT_ATTRIBUTES.add("homeStreet");
		CONTACT_ATTRIBUTES.add("homepostofficebox");
		CONTACT_ATTRIBUTES.add("l");
		CONTACT_ATTRIBUTES.add("manager");
		CONTACT_ATTRIBUTES.add("mobile");
		CONTACT_ATTRIBUTES.add("namesuffix");
		CONTACT_ATTRIBUTES.add("nickname");
		CONTACT_ATTRIBUTES.add("o");
		CONTACT_ATTRIBUTES.add("pager");
		CONTACT_ATTRIBUTES.add("personaltitle");
		CONTACT_ATTRIBUTES.add("postalcode");
		CONTACT_ATTRIBUTES.add("postofficebox");
		CONTACT_ATTRIBUTES.add("profession");
		CONTACT_ATTRIBUTES.add("roomnumber");
		CONTACT_ATTRIBUTES.add("secretarycn");
		CONTACT_ATTRIBUTES.add("sn");
		CONTACT_ATTRIBUTES.add("spousecn");
		CONTACT_ATTRIBUTES.add("st");
		CONTACT_ATTRIBUTES.add("street");
		CONTACT_ATTRIBUTES.add("telephoneNumber");
		CONTACT_ATTRIBUTES.add("title");
		CONTACT_ATTRIBUTES.add("description");
		CONTACT_ATTRIBUTES.add("im");
		CONTACT_ATTRIBUTES.add("middlename");
		CONTACT_ATTRIBUTES.add("lastmodified");
		CONTACT_ATTRIBUTES.add("otherstreet");
		CONTACT_ATTRIBUTES.add("otherstate");
		CONTACT_ATTRIBUTES.add("otherpostofficebox");
		CONTACT_ATTRIBUTES.add("otherpostalcode");
		CONTACT_ATTRIBUTES.add("othercountry");
		CONTACT_ATTRIBUTES.add("othercity");
		CONTACT_ATTRIBUTES.add("haspicture");
		CONTACT_ATTRIBUTES.add("keywords");
		CONTACT_ATTRIBUTES.add("othermobile");
		CONTACT_ATTRIBUTES.add("otherTelephone");
		CONTACT_ATTRIBUTES.add("gender");
		CONTACT_ATTRIBUTES.add("private");
		CONTACT_ATTRIBUTES.add("sensitivity");
		CONTACT_ATTRIBUTES.add("fburl");
	}

	protected static final String YYYY_MM_DD_HH_MM_SS = "yyyy/MM/dd HH:mm:ss";
	protected static final String YYYY_MM_DD_T_HHMMSS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	static final String[] VCARD_N_PROPERTIES = {"sn", "givenName", "middlename", "personaltitle", "namesuffix"};
	static final String[] VCARD_ADR_HOME_PROPERTIES = {"homepostofficebox", null, "homeStreet", "homeCity", "homeState", "homePostalCode", "homeCountry"};
	static final String[] VCARD_ADR_WORK_PROPERTIES = {"postofficebox", "roomnumber", "street", "l", "st", "postalcode", "co"};
	static final String[] VCARD_ADR_OTHER_PROPERTIES = {"otherpostofficebox", null, "otherstreet", "othercity", "otherstate", "otherpostalcode", "othercountry"};
	static final String[] VCARD_ORG_PROPERTIES = {"o", "department"};
	private static final String YYYYMMDD_T_HHMMSS_Z = "yyyyMMdd'T'HHmmss'Z'";
	private static final String YYYY_MM_DD = "yyyy-MM-dd";
	private static final String YYYY_MM_DD_T_HHMMSS_SSS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public static Date convertZuluDateToDate(String value) {
		Date result = null;
		if (value != null && value.length() > 0) {
			try {
				SimpleDateFormat parser = getZuluDateFormat();
				result = new Timestamp(parser.parse(value).getTime());
			} catch (ParseException e) {
				//e.printStackTrace();
			}
		}
		return result;
	}

	public static String convertDateToZuluDate(Date value) {
		String result = null;
		if (value != null) {
			try {
				SimpleDateFormat formater = getZuluDateFormat();
				result = formater.format(value);
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}
		return result;
	}

	public static String convertZuluDateToBday(String value) {
		String result = null;
		if (value != null && value.length() > 0) {
			try {
				SimpleDateFormat parser = getZuluDateFormat();
				Calendar cal = Calendar.getInstance();
				cal.setTime(parser.parse(value));
				cal.add(Calendar.HOUR_OF_DAY, 12);
				result = getVcardBdayFormat().format(cal.getTime());
			} catch (ParseException e) {
				//e.printStackTrace();
			}
		}
		return result;
	}

	public static String convertBDayToZulu(String value) {
		String result = null;
		if (value != null && value.length() > 0) {
			try {
				SimpleDateFormat parser;
				if (value.length() == 10) {
					parser = getVcardBdayFormat();
				} else if (value.length() == 15) {
					parser = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);
					parser.setTimeZone(GMT_TIMEZONE);
				} else {
					parser = getExchangeZuluDateFormat();
				}
				result = getZuluDateFormat().format(parser.parse(value));
			} catch (ParseException e) {
				//e.printStackTrace();
			}
		}

		return result;
	}

	public static SimpleDateFormat getZuluDateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYYMMDD_T_HHMMSS_Z, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static SimpleDateFormat getVcardBdayFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static SimpleDateFormat getExchangeZuluDateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HHMMSS_Z, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	protected static SimpleDateFormat getExchangeZuluDateFormatMillisecond() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD_T_HHMMSS_SSS_Z, Locale.ENGLISH);
		dateFormat.setTimeZone(GMT_TIMEZONE);
		return dateFormat;
	}

	public String getUid() throws Exception {
		return get("uid");
	}

	public Date getLastModifiedDate() {
		return convertZuluDateToDate(get("lastmodified"));
	}

	public String getBody() throws Exception {
		// build RFC 2426 VCard from contact information
		CustomVCardWriter writer = new CustomVCardWriter();
		writer.startCard();
		writer.appendProperty("UID", getUid());
		// common name
		writer.appendProperty("FN", get("cn"));
		// RFC 2426: Family Name, Given Name, Additional Names, Honorific Prefixes, and Honorific Suffixes
		writer.appendProperty("N", get("sn"), get("givenName"), get("middlename"), get("personaltitle"), get("namesuffix"));

		writer.appendProperty("TEL;TYPE=cell", get("mobile"));
		writer.appendProperty("TEL;TYPE=work", get("telephoneNumber"));
		writer.appendProperty("TEL;TYPE=home", get("homePhone"));
		writer.appendProperty("TEL;TYPE=fax", get("facsimiletelephonenumber"));
		writer.appendProperty("TEL;TYPE=pager", get("pager"));
		writer.appendProperty("TEL;TYPE=car", get("othermobile"));
		writer.appendProperty("TEL;TYPE=home,fax", get("homefax"));
		writer.appendProperty("TEL;TYPE=isdn", get("internationalisdnnumber"));
		writer.appendProperty("TEL;TYPE=msg", get("otherTelephone"));

		// The structured type value corresponds, in sequence, to the post office box; the extended address;
		// the street address; the locality (e.g., city); the region (e.g., state or province);
		// the postal code; the country name
		writer.appendProperty("ADR;TYPE=home",
				get("homepostofficebox"), null, get("homeStreet"), get("homeCity"), get("homeState"), get("homePostalCode"), get("homeCountry"));
		writer.appendProperty("ADR;TYPE=work",
				get("postofficebox"), get("roomnumber"), get("street"), get("l"), get("st"), get("postalcode"), get("co"));
		writer.appendProperty("ADR;TYPE=other",
				get("otherpostofficebox"), null, get("otherstreet"), get("othercity"), get("otherstate"), get("otherpostalcode"), get("othercountry"));

		writer.appendProperty("EMAIL;TYPE=work", get("smtpemail1"));
		writer.appendProperty("EMAIL;TYPE=home", get("smtpemail2"));
		writer.appendProperty("EMAIL;TYPE=other", get("smtpemail3"));

		String[] org = StringUtils.split(get("o"), '\n');
		String orgLine1 = null;
		String orgLine2 = null;
		if (org.length > 0) {
			orgLine1 = org[0];
		}
		if (org.length > 1) {
			orgLine2 = org[1];
		}
		writer.appendProperty("ORG", orgLine1, orgLine2);
		writer.appendProperty("URL;TYPE=work", get("businesshomepage"));
		writer.appendProperty("URL;TYPE=home", get("personalHomePage"));
		writer.appendProperty("TITLE", get("title"));
		writer.appendProperty("NOTE", get("description"));

		writer.appendProperty("CUSTOM1", get("extensionattribute1"));
		writer.appendProperty("CUSTOM2", get("extensionattribute2"));
		writer.appendProperty("CUSTOM3", get("extensionattribute3"));
		writer.appendProperty("CUSTOM4", get("extensionattribute4"));

		writer.appendProperty("ROLE", get("profession"));
		writer.appendProperty("NICKNAME", get("nickname"));
		writer.appendProperty("X-AIM", get("im"));

		writer.appendProperty("BDAY", convertZuluDateToBday(get("bday")));
		writer.appendProperty("ANNIVERSARY", convertZuluDateToBday(get("anniversary")));

		String gender = get("gender");
		if ("1".equals(gender)) {
			writer.appendProperty("SEX", "2");
		} else if ("2".equals(gender)) {
			writer.appendProperty("SEX", "1");
		}

		writer.appendProperty("CATEGORIES", get("keywords"));

		writer.appendProperty("FBURL", get("fburl"));

		if ("1".equals(get("private"))) {
			writer.appendProperty("CLASS", "PRIVATE");
		}

		writer.appendProperty("X-ASSISTANT", get("secretarycn"));
		writer.appendProperty("X-MANAGER", get("manager"));
		String spouse = StringUtils.defaultIfBlank(get("spousecnLastname"), "");
		String spousecnFirstname = StringUtils.defaultIfBlank(get("spousecnFirstname"), "");
		if (StringUtils.isNotBlank(spousecnFirstname)) {
			spouse += ", " + spousecnFirstname;
		}
		writer.appendProperty("X-SPOUSE", spouse);

		writer.appendProperty("REV", get("lastmodified"));

		writer.endCard();
		return writer.toString();
	}

	public void load(VProperty beginProperty, BufferedReader reader) throws Exception {
		// parse VCARD body to build contact property map
		put("outlookmessageclass", "IPM.Contact");

		VObject vcard = new VObject(beginProperty, reader);
		for (VProperty property : vcard.getProperties()) {
			if ("UID".equals(property.getKey())) {
				put("uid", property.getValue());
			} else if ("REV".equals(property.getKey())) {
				put("lastmodified", property.getValue());
			} else if ("FN".equals(property.getKey())) {
				put("cn", property.getValue());
				put("subject", property.getValue());
				put("fileas", property.getValue());

			} else if ("N".equals(property.getKey())) {
				convertContactProperties(this, VCARD_N_PROPERTIES, property.getValues());
			} else if ("NICKNAME".equals(property.getKey())) {
				put("nickname", property.getValue());
			} else if ("TEL".equals(property.getKey())) {
				if (property.hasParam("TYPE", "cell") || property.hasParam("X-GROUP", "cell")) {
					put("mobile", property.getValue());
				} else if (property.hasParam("TYPE", "work") || property.hasParam("X-GROUP", "work")) {
					put("telephoneNumber", property.getValue());
				} else if (property.hasParam("TYPE", "home") || property.hasParam("X-GROUP", "home")) {
					put("homePhone", property.getValue());
				} else if (property.hasParam("TYPE", "fax")) {
					if (property.hasParam("TYPE", "home")) {
						put("homefax", property.getValue());
					} else {
						put("facsimiletelephonenumber", property.getValue());
					}
				} else if (property.hasParam("TYPE", "pager")) {
					put("pager", property.getValue());
				} else if (property.hasParam("TYPE", "car")) {
					put("othermobile", property.getValue());
				} else {
					put("otherTelephone", property.getValue());
				}
			} else if ("ADR".equals(property.getKey())) {
				// address
				if (property.hasParam("TYPE", "home")) {
					convertContactProperties(this, VCARD_ADR_HOME_PROPERTIES, property.getValues());
				} else if (property.hasParam("TYPE", "work")) {
					convertContactProperties(this, VCARD_ADR_WORK_PROPERTIES, property.getValues());
					// any other type goes to other address
				} else {
					convertContactProperties(this, VCARD_ADR_OTHER_PROPERTIES, property.getValues());
				}
			} else if ("EMAIL".equals(property.getKey())) {
				if (property.hasParam("TYPE", "home")) {
					put("email2", property.getValue());
					put("smtpemail2", property.getValue());
				} else if (property.hasParam("TYPE", "other")) {
					put("email3", property.getValue());
					put("smtpemail3", property.getValue());
				} else {
					put("email1", property.getValue());
					put("smtpemail1", property.getValue());
				}
			} else if ("ORG".equals(property.getKey())) {
				String o = null;
				if (property.getValues() != null && property.getValues().size() > 0) {
					o = property.getValues().get(0);
				}
				if (property.getValues() != null && property.getValues().size() > 1) {
					o = o + '\n' + property.getValues().get(1);
				}
				put("o", o);
//				convertContactProperties(this, VCARD_ORG_PROPERTIES, property.getValues());
			} else if ("URL".equals(property.getKey())) {
				if (property.hasParam("TYPE", "work")) {
					put("businesshomepage", property.getValue());
				} else if (property.hasParam("TYPE", "home")) {
					put("personalHomePage", property.getValue());
				} else {
					// default: set personal home page
					put("personalHomePage", property.getValue());
				}
			} else if ("TITLE".equals(property.getKey())) {
				put("title", property.getValue());
			} else if ("NOTE".equals(property.getKey())) {
				put("description", property.getValue());
			} else if ("CUSTOM1".equals(property.getKey())) {
				put("extensionattribute1", property.getValue());
			} else if ("CUSTOM2".equals(property.getKey())) {
				put("extensionattribute2", property.getValue());
			} else if ("CUSTOM3".equals(property.getKey())) {
				put("extensionattribute3", property.getValue());
			} else if ("CUSTOM4".equals(property.getKey())) {
				put("extensionattribute4", property.getValue());
			} else if ("ROLE".equals(property.getKey())) {
				put("profession", property.getValue());
			} else if ("X-AIM".equals(property.getKey())) {
				put("im", property.getValue());
			} else if ("BDAY".equals(property.getKey())) {
				put("bday", convertBDayToZulu(property.getValue()));
			} else if ("ANNIVERSARY".equals(property.getKey()) || "X-ANNIVERSARY".equals(property.getKey())) {
				put("anniversary", convertBDayToZulu(property.getValue()));
			} else if ("CATEGORIES".equals(property.getKey())) {
				put("keywords", property.getValue());
			} else if ("CLASS".equals(property.getKey())) {
				if ("PUBLIC".equals(property.getValue())) {
					put("sensitivity", "0");
					put("private", "false");
				} else {
					put("sensitivity", "2");
					put("private", "true");
				}
			} else if ("SEX".equals(property.getKey())) {
				String propertyValue = property.getValue();
				if ("1".equals(propertyValue)) {
					put("gender", "2");
				} else if ("2".equals(propertyValue)) {
					put("gender", "1");
				}
			} else if ("FBURL".equals(property.getKey())) {
				put("fburl", property.getValue());
			} else if ("X-ASSISTANT".equals(property.getKey())) {
				put("secretarycn", property.getValue());
			} else if ("X-MANAGER".equals(property.getKey())) {
				put("manager", property.getValue());
			} else if ("X-SPOUSE".equals(property.getKey())) {
				String[] spouse = StringUtils.splitPreserveAllTokens(property.getValue(), ',');
				if (spouse.length > 0) {
					put("spousecnLastname", StringUtils.trim(spouse[0]));
				}
				if (spouse.length > 1) {
					put("spousecnFirstname", StringUtils.trim(spouse[1]));
				}
			} else if ("PHOTO".equals(property.getKey())) {
				put("photo", property.getValue());
				put("haspicture", "true");
			}
		}

		// reset missing properties to null
		for (String key : CONTACT_ATTRIBUTES) {
			if (!"imapUid".equals(key) && !"etag".equals(key) && !"urlcompname".equals(key)
					&& !"lastmodified".equals(key) && !"sensitivity".equals(key) &&
					!containsKey(key)) {
				put(key, null);
			}
		}
	}

	protected void convertContactProperties(Map<String, String> properties, String[] contactProperties, List<String> values) {
		for (int i = 0; i < values.size() && i < contactProperties.length; i++) {
			if (contactProperties[i] != null) {
				properties.put(contactProperties[i], values.get(i));
			}
		}
	}

}
