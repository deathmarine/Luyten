package us.deathmarine.luyten;

import java.text.DecimalFormat;

public class TooLargeFileException extends Exception {
	private static final long serialVersionUID = 6091096838075139962L;
	private long size;

	public TooLargeFileException(long size) {
		this.size = size;
	}

	public String getReadableFileSize() {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
