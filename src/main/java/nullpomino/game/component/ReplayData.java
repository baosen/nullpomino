// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;
import java.util.ArrayList;

import nullpomino.util.CustomProperties;

/**
 * Used in the replay button input dataClass of
 */
public class ReplayData implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = 737226985994393117L;

	/** Button input dataOf default The length of the */
	public static final int DEFAULT_ARRAYLIST_SIZE = 60 * 60 * 10;

	/** Button input data */
	public ArrayList<Integer> inputDataArray;

	/**
	 * Default constructor
	 */
	public ReplayData() {
		reset();
	}

	/**
	 * Copy constructor
	 * @param r Copy source
	 */
	public ReplayData(ReplayData r) {
		copy(r);
	}

	/**
	 * Reset to defaults
	 */
	public void reset() {
		if(inputDataArray == null)
			inputDataArray = new ArrayList<Integer>(DEFAULT_ARRAYLIST_SIZE);
		else
			inputDataArray.clear();
	}

	/**
	 * OtherReplayDataCopied from the
	 * @param r Copy source
	 */
	public void copy(ReplayData r) {
		reset();
		inputDataArray.addAll(r.inputDataArray);
	}

	/**
	 *  button inputSet the status
	 * @param input  button inputBit of status flag
	 * @param frame  frame  (Course time)
	 */
	public void setInputData(int input, int frame) {
		if((frame < 0) || (frame >= inputDataArray.size())) {
			inputDataArray.add(input);
		} else {
			inputDataArray.set(frame, input);
		}
	}

	/**
	 *  button inputGet status
	 * @param frame  frame  (Course time)
	 * @return  button inputBit of status flag
	 */
	public int getInputData(int frame) {
		if((frame < 0) || (frame >= inputDataArray.size())) {
			return 0;
		}
		return inputDataArray.get(frame);
	}

	/**
	 * Stored in the property set
	 * @param p Property Set
	 * @param id AnyID (Player IDEtc.)
	 * @param maxFrame Save frame count (-1Save in all)
	 */
	public void writeProperty(CustomProperties p, int id, int maxFrame) {
		int max = replayFrameCount(maxFrame);
		int previous = 0;

		for(int i = 0; i < max; i++) {
			int input = inputDataArray.get(i);
			if(input != previous) p.setProperty(replayFrameKey(id, i), input);
			previous = input;
		}
		p.setProperty(replayMaxKey(id), max);
	}

	/**
	 * Read from the property set
	 * @param p Property Set
	 * @param id AnyID (Player IDEtc.)
	 */
	public void readProperty(CustomProperties p, int id) {
		reset();
		int max = p.getProperty(replayMaxKey(id), 0);
		int input = 0;

		for(int i = 0; i < max; i++) {
			int data = p.getProperty(replayFrameKey(id, i), -1);
			if(data != -1) input = data;
			setInputData(input, i);
		}
	}

	private int replayFrameCount(int maxFrame) {
		return ((maxFrame < 0) || (maxFrame > inputDataArray.size())) ? inputDataArray.size() : maxFrame;
	}

	private static String replayFrameKey(int id, int frame) {
		return id + ".r." + frame;
	}

	private static String replayMaxKey(int id) {
		return id + ".r.max";
	}
}
