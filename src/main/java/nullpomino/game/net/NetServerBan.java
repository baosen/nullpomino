package nullpomino.game.net;

import java.util.Calendar;
import java.util.TimeZone;

import nullpomino.util.GeneralUtil;

import org.apache.log4j.Logger;

public class NetServerBan {
	static Logger log = Logger.getLogger(NetServerBan.class);

	public String addr;

	public Calendar startDate;
	public int banLength;

	public static final int BANLENGTH_1HOUR = 0,
							BANLENGTH_6HOURS = 1,
							BANLENGTH_24HOURS = 2,
							BANLENGTH_1WEEK = 3,
							BANLENGTH_1MONTH = 4,
							BANLENGTH_1YEAR = 5,
							BANLENGTH_PERMANENT = 6;

	public static final int BANLENGTH_TOTAL = 7;

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final int[] BAN_CALENDAR_FIELDS = {
		Calendar.HOUR,
		Calendar.HOUR,
		Calendar.HOUR,
		Calendar.WEEK_OF_MONTH,
		Calendar.MONTH,
		Calendar.YEAR
	};
	private static final int[] BAN_CALENDAR_AMOUNTS = {1, 6, 24, 1, 1, 1};

	/**
	 * Empty Constructor
	 */
	public NetServerBan() {
	}

	/**
	 * Creates a new NetServerBan object representing a permanent ban starting now.
	 * @param addr the remote address this NetServerBan affects.
	 */
	public NetServerBan(String addr) {
		this(addr, BANLENGTH_PERMANENT);
	}

	/**
	 * Creates a new NetServerBan object representing a ban starting now.
	 * @param addr the remote address this NetServerBan affects.
	 * @param banLength an integer representing the length of the ban.
	 */
	public NetServerBan(String addr, int banLength) {
		this.addr = addr;
		startDate = Calendar.getInstance(GMT);
		this.banLength = banLength;
	}

	/**
	 * Returns the end date of the ban, or null if no such date exists (i.e. permanent).
	 * @return the end date or null
	 */
	public Calendar getEndDate() {
		if((banLength < 0) || (banLength >= BANLENGTH_PERMANENT)) return null;

		Calendar res = (Calendar) startDate.clone();
		res.add(BAN_CALENDAR_FIELDS[banLength], BAN_CALENDAR_AMOUNTS[banLength]);
		return res;
	}

	/**
	 * Returns a boolean representing whether or not this NetServerBan is expired.
	 * @return true if the ban is expired.
	 */
	public boolean isExpired() {
		Calendar endDate = getEndDate();
		return (endDate != null) && Calendar.getInstance(GMT).after(endDate);
	}

	/**
	 * Export to String. Format is addr;banLength;GMT<calendar>.
	 * @return String
	 */
	public String exportString() {
		String strTemp = GeneralUtil.exportCalendarString(startDate);
		String strStartDate = (strTemp != null) ? ("GMT" + strTemp) : "";
		return addr + ";" + banLength + ";" + strStartDate;
	}

	/**
	 * Import from String produced by {@link #exportString()}.
	 * @param strInput String
	 */
	public void importString(String strInput) {
		String[] strArray = strInput.split(";");
		addr = strArray[0];
		banLength = Integer.parseInt(strArray[1]);
		String dateField = strArray[2];
		if (dateField.startsWith("GMT")) {
			Calendar c = GeneralUtil.importCalendarString(dateField.substring(3));
			if (c != null) startDate = c;
		}
	}
}
