// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.tool.musiclisteditor;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A simple file filter
 */
public class SimpleFileFilter extends FileFilter {
	/** Extension */
	protected String extension;

	/** Display name */
	protected String description;

	/**
	 * Constructor
	 */
	public SimpleFileFilter() {
		super();
		this.extension = "";
		this.description = "";
	}

	/**
	 * Constructor
	 * @param extension Extension
	 */
	public SimpleFileFilter(String extension) {
		super();
		this.extension = extension;
		this.description = "";
	}

	/**
	 * Constructor
	 * @param extension Extension
	 * @param description Display name
	 */
	public SimpleFileFilter(String extension, String description) {
		super();
		this.extension = extension;
		this.description = description;
	}

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		if(f.getName().endsWith(extension)) return true;
		return false;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @param description Set in description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * @param extension Set in extension
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}
}
