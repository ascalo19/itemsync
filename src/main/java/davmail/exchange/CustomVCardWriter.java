package davmail.exchange;

public class CustomVCardWriter extends ICSBufferedWriter {

	public void startCard() {
		writeLine("BEGIN:VCARD");
		writeLine("VERSION:3.0");
	}

	@Override
	public void appendProperty(String propertyName, String propertyValue) {
		if ((propertyValue != null) && (propertyValue.length() > 0)) {
			StringBuilder lineBuffer = new StringBuilder();
			lineBuffer.append(propertyName);
			lineBuffer.append(':');
			appendMultilineEncodedValue(lineBuffer, propertyValue);
			writeLine(lineBuffer.toString());
		}

	}

	public void appendProperty(String propertyName, String... propertyValue) {
		boolean hasValue = false;
		for (String value : propertyValue) {
			if ((value != null) && (value.length() > 0)) {
				hasValue = true;
				break;
			}
		}
		if (hasValue) {
			boolean first = true;
			StringBuilder lineBuffer = new StringBuilder();
			lineBuffer.append(propertyName);
			lineBuffer.append(':');
			for (String value : propertyValue) {
				if (first) {
					first = false;
				} else {
					lineBuffer.append(';');
				}
				appendMultilineEncodedValue(lineBuffer, value);
			}
			writeLine(lineBuffer.toString());
		}
	}

	@Override
	protected void appendMultilineEncodedValue(StringBuilder buffer, String value) {
		if (value != null) {
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == ',' || c == ';') {
					buffer.append('\\');
				}
				if (c == '\n') {
					buffer.append("\\n");
				} else if (c != '\r') {
					buffer.append(value.charAt(i));
				}
			}
		}
	}

	public void endCard() {
		writeLine("END:VCARD");
	}
}
