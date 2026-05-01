package nullpomino.game.net;

final class NetStringArray {
	private NetStringArray() {
	}

	static final class Reader {
		private final String[] values;
		private int index;

		Reader(String[] values) {
			this.values = values;
		}

		boolean hasNext() {
			return index < values.length;
		}

		String readEncoded() {
			return NetUtil.urlDecode(read());
		}

		int readInt() {
			return Integer.parseInt(read());
		}

		int readInt(int defaultValue) {
			return hasNext() ? readInt() : defaultValue;
		}

		boolean readBoolean() {
			return Boolean.parseBoolean(read());
		}

		boolean readBoolean(boolean defaultValue) {
			return hasNext() ? readBoolean() : defaultValue;
		}

		String read() {
			return values[index++];
		}

		String read(String defaultValue) {
			return hasNext() ? read() : defaultValue;
		}
	}

	static final class Writer {
		private final String[] values;
		private int index;

		Writer(int size) {
			values = new String[size];
		}

		void writeEncoded(String value) {
			write(NetUtil.urlEncode(value));
		}

		void write(int value) {
			write(Integer.toString(value));
		}

		void write(boolean value) {
			write(Boolean.toString(value));
		}

		String[] values() {
			return values;
		}

		void write(String value) {
			values[index++] = value;
		}
	}
}
