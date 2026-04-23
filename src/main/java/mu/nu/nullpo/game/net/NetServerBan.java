package mu.nu.nullpo.game.net;

import java.util.Calendar;
import java.util.TimeZone;

import mu.nu.nullpo.util.GeneralUtil;

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
		startDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		this.banLength = banLength;
	}

	/**
	 * Returns the end date of the ban, or null if no such date exists (i.e. permanent).
	 * @return the end date or null
	 */
	public Calendar getEndDate() {
		Calendar res = (Calendar) startDate.clone();
		switch (banLength) {
			case BANLENGTH_1HOUR: res.add(Calendar.HOUR, 1); break;
			case BANLENGTH_6HOURS: res.add(Calendar.HOUR, 6); break;
			case BANLENGTH_24HOURS: res.add(Calendar.HOUR, 24); break;
			case BANLENGTH_1WEEK: res.add(Calendar.WEEK_OF_MONTH, 1); break;
			case BANLENGTH_1MONTH: res.add(Calendar.MONTH, 1); break;
			case BANLENGTH_1YEAR: res.add(Calendar.YEAR, 1); break;
			default: res = null;
		}
		return res;
	}

	/**
	 * Returns a boolean representing whether or not this NetServerBan is expired.
	 * @return true if the ban is expired.
	 */
	public boolean isExpired() {
		Calendar endDate = getEndDate();
		if (endDate == null) {
			return false;
		} else {
			return Calendar.getInstance(TimeZone.getTimeZone("GMT")).after(getEndDate());
		}
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
