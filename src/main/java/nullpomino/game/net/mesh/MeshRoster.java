// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatcher-confined mesh membership table: which uid is reachable over
 * which link. Purely transport-level - room/seat state lives in
 * {@link MeshAuthority}/the mirror.
 */
public class MeshRoster {
	/** One mesh member as known at transport level */
	public static final class Entry {
		public final int uid;
		public String name;
		public String host;
		public int listenPort;
		/** Link to this member; null for the local peer's own entry */
		public MeshPeerLink link;
		/** true once the member reported all its peer links are up */
		public boolean meshOk;

		public Entry(int uid, String name, String host, int listenPort, MeshPeerLink link) {
			this.uid = uid;
			this.name = name;
			this.host = host;
			this.listenPort = listenPort;
			this.link = link;
		}
	}

	private final LinkedHashMap<Integer, Entry> members = new LinkedHashMap<Integer, Entry>();

	public void add(Entry entry) {
		members.put(entry.uid, entry);
	}

	public Entry remove(int uid) {
		return members.remove(uid);
	}

	public Entry get(int uid) {
		return members.get(uid);
	}

	/** @return The entry bound to this link, or null */
	public Entry getByLink(MeshPeerLink link) {
		for(Entry entry: members.values()) {
			if(entry.link == link) return entry;
		}
		return null;
	}

	/** @return Lowest member uid (the migration successor rule), or -1 if empty */
	public int lowestUid() {
		int lowest = -1;
		for(Entry entry: members.values()) {
			if((lowest == -1) || (entry.uid < lowest)) lowest = entry.uid;
		}
		return lowest;
	}

	/** @return All members with a live link (excludes the local peer) */
	public List<Entry> linkedMembers() {
		List<Entry> result = new ArrayList<Entry>();
		for(Entry entry: members.values()) {
			if(entry.link != null) result.add(entry);
		}
		return result;
	}

	public Map<Integer, Entry> all() {
		return members;
	}

	public int size() {
		return members.size();
	}
}
