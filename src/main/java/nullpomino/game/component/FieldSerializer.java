// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

public class FieldSerializer {

	/**
	 * @param row Row of blocks
	 * @return a String representing the row
	 */
	public static String rowToString(Block[] row){
		StringBuilder strResult = new StringBuilder(row.length);

		for(int x = 0; x < row.length; x++) {
			strResult.append(row[x].blockToChar());
		}

		return strResult.toString();
	}

	/**
	 * fieldConverted to a string
	 * @return It was converted to a stringfield
	 */
	public static String fieldToString(Field field) {
		StringBuilder strResult = new StringBuilder();

		for(int i = field.getHeight() - 1; i >= Math.max(-1, field.getHighestBlockY()); i--) {
			strResult.append(rowToString(field.getRow(i)));
		}

		// Closing0Remove the
		while((strResult.length() > 0) && (strResult.charAt(strResult.length() - 1) == '0')) {
			strResult.setLength(strResult.length() - 1);
		}

		return strResult.toString();
	}

	public static Block[] stringToRow(Field field, String str){
		return stringToRow(field, str, 0, false, false);
	}

	/**
	 * @param str String representing field state
	 * @param skin Block skin being used in this field
	 * @param isGarbage Row is a garbage row
	 * @param isWall Row is a wall (i.e. hurry-up rows)
	 * @return The row array
	 */
	public static Block[] stringToRow(Field field, String str, int skin, boolean isGarbage, boolean isWall){
		Block[] row = new Block[field.getWidth()];
		for(int j = 0; j < field.getWidth(); j++) {

			int blkColor = Block.BLOCK_COLOR_NONE;

			/*
			 * NullNoname's original approach from the old stringToField:
			 * If a character outside the row string is referenced,
			 * default to an empty block by ignoring the exception.
			 */
			try {
				char c = str.charAt(j);
				blkColor = Block.charToBlockColor(c);
			} catch (Exception e) {}

			row[j] = new Block();
			row[j].color = blkColor;
			row[j].skin = skin;
			row[j].elapsedFrames = -1;
			row[j].setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
			row[j].setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true);

			if(isGarbage) { //TODO: This may need extension when garbage does not only sport one hole (i.e. TGM garbage)
				row[j].setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
			}
			if(isWall) {
				row[j].setAttribute(Block.BLOCK_ATTRIBUTE_WALL, true);
			}
		}

		return row;
	}


	/**
	 * Based on a stringfieldChange
	 * @param str String
	 */
	public static void stringToField(Field field, String str) {
		stringToField(field, str, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Based on a stringfieldChange
	 * @param str String
	 * @param skin BlockPicture of
	 * @param highestGarbageY The highestgarbage blockThe position of the
	 * @param highestWallY The highestHurryupBlockThe position of the
	 */
	public static void stringToField(Field field, String str, int skin, int highestGarbageY, int highestWallY) {
		for(int i = -1; i < field.getHeight(); i++) {
			int index = (field.getHeight() - 1 - i) * field.getWidth();
			/*
			 * Much like NullNoname's try/catch from the old stringToField that is now in stringToRow,
			 * we need to skip over substrings referenced outside the field string -- empty rows.
			 */
			try{
				String substr = str.substring(index, Math.min(str.length(), index+field.getWidth()));
				Block[] row = stringToRow(field, substr, skin, (i >= highestGarbageY), (i >= highestWallY));
				for(int j = 0; j < field.getWidth(); j++){
					field.setBlock(j, i, row[j]);
				}
			}
			catch(Exception e){
				for(int j = 0; j < field.getWidth(); j++){
					field.setBlock(j, i, new Block(Block.BLOCK_COLOR_NONE));
				}
			}
		}
	}

	/**
	 * @param row Row of blocks
	 * @return a String representing the row with attributes
	 */
	public static String attrRowToString(Block[] row){
		StringBuilder strResult = new StringBuilder(row.length * 4);

		for(int x = 0; x < row.length; x++) {
			strResult.append(Integer.toString(row[x].color, 16)).append('/');
			strResult.append(Integer.toString(row[x].attribute, 16)).append(';');
		}

		return strResult.toString();
	}

	/**
	 * Convert this field to a String with attributes
	 * @return a String representing the field with attributes
	 */
	public static String attrFieldToString(Field field) {
		StringBuilder strResult = new StringBuilder();

		for(int i = field.getHeight() - 1; i >= Math.max(-1, field.getHighestBlockY()); i--) {
			strResult.append(attrRowToString(field.getRow(i)));
		}
		while(endsWith(strResult, "0/0;")) {
			strResult.setLength(strResult.length() - 4);
		}

		return strResult.toString();
	}

	private static boolean endsWith(StringBuilder value, String suffix) {
		if(value.length() < suffix.length()) return false;
		int offset = value.length() - suffix.length();
		for(int i = 0; i < suffix.length(); i++) {
			if(value.charAt(offset + i) != suffix.charAt(i)) return false;
		}
		return true;
	}

	public static Block[] attrStringToRow(Field field, String str, int skin) {
		return attrStringToRow(field, str.split(";"), skin);
	}

	public static Block[] attrStringToRow(Field field, String[] strArray, int skin) {
		Block[] row = new Block[field.getWidth()];

		for(int j = 0; j < field.getWidth(); j++) {
			int blkColor = Block.BLOCK_COLOR_NONE;
			int attr = 0;

			try {
				String[] strSubArray = strArray[j].split("/");
				if(strSubArray.length > 0)
					blkColor = Integer.parseInt(strSubArray[0], 16);
				if(strSubArray.length > 1)
					attr = Integer.parseInt(strSubArray[1], 16);
			} catch (Exception e) {}

			row[j] = new Block();
			row[j].color = blkColor;
			row[j].skin = skin;
			row[j].elapsedFrames = -1;
			row[j].attribute = attr;
			row[j].setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
			row[j].setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		}

		return row;
	}

	public static void attrStringToField(Field field, String str, int skin) {
		String[] strArray = str.split(";", -1);

		for(int i = -1; i < field.getHeight(); i++) {
			int index = (field.getHeight() - 1 - i) * field.getWidth();

			try{
				String[] strArray2 = new String[field.getWidth()];
				for(int j = 0; j < field.getWidth(); j++){
					if(index + j < strArray.length)
						strArray2[j] = strArray[index + j];
					else
						strArray2[j] = "";
				}

				Block[] row = attrStringToRow(field, strArray2, skin);
				for(int j = 0; j < field.getWidth(); j++){
					field.setBlock(j, i, row[j]);
				}
			}
			catch(Exception e){
				for(int j = 0; j < field.getWidth(); j++){
					field.setBlock(j, i, new Block(Block.BLOCK_COLOR_NONE));
				}
			}
		}
	}
}
