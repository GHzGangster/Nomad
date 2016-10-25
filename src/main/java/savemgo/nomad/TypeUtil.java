package savemgo.nomad;

public class TypeUtil {

	@SuppressWarnings({ "unchecked" })
	public static <T> T cast(Object o) {
		return (T) o;
	}

	public static int toInt(Object o) {
		if (o != null) {
			if (o instanceof Integer) {
				return ((Integer) o).intValue();
			} else if (o instanceof Long) {
				return ((Long) o).intValue();
			} else if (o instanceof Double) {
				return ((Double) o).intValue();
			} else if (o instanceof String) {
				return Integer.parseInt((String) o);
			}
		}
		return 0;
	}

	public static String toString(Object o) {
		if (o != null) {
			if (o instanceof String) {
				return (String) o;
			}
		}
		return "";
	}

}
