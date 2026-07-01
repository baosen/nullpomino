package nullpomino.game.net;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * File persistence for the netplay server's ban list
 * (config/setting/netserver_banlist.cfg). The in-memory list itself stays on
 * NetServer; this helper only reads and writes the file.
 */
final class NetServerBanList {
	/** Log */
	static Logger log = Logger.getLogger(NetServerBanList.class);

	private NetServerBanList() {}

	/**
	 * Read the ban list from a file (expired entries are dropped).
	 * @return the loaded ban list (empty if the file is missing or unreadable)
	 */
	static LinkedList<NetServerBan> load() {
		LinkedList<NetServerBan> banList = new LinkedList<NetServerBan>();

		try {
			BufferedReader txtBanList = new BufferedReader(new FileReader("config/setting/netserver_banlist.cfg"));

			String str;
			while((str = txtBanList.readLine()) != null) {
				if(str.length() > 0) {
					NetServerBan ban = new NetServerBan();
					ban.importString(str);
					if(!ban.isExpired()) banList.add(ban);
				}
			}
		} catch (IOException e) {
			log.debug("Ban list file doesn't exist");
		} catch (Exception e) {
			log.warn("Failed to load ban list", e);
		}

		return banList;
	}

	/**
	 * Write the ban list to a file.
	 * @param banList the ban list to persist
	 */
	static void save(LinkedList<NetServerBan> banList) {
		try {
			FileWriter outFile = new FileWriter("config/setting/netserver_banlist.cfg");
			PrintWriter out = new PrintWriter(outFile);

			for(NetServerBan ban: banList) {
				out.println(ban.exportString());
			}

			out.flush();
			out.close();

			log.info("Ban list saved");
		} catch (Exception e) {
			log.error("Failed to save ban list", e);
		}
	}
}
