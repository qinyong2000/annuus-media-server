/**
 * 
 */
package com.ams.http;

import java.io.File;

public class HttpFileUpload {
	public final static int RESULT_OK = 0;
	public final static int RESULT_SIZE = 1;
	public final static int RESULT_PARTIAL = 3;
	public final static int RESULT_NOFILE = 4;

	public final static long MAXSIZE_FILE_UPLOAD = 10 * 1024 * 1024; // max 10M

	public String filename;
	public File tempFile;
	public int result;

	public HttpFileUpload(String filename, File tempFile, int result) {
		this.filename = filename;
		this.tempFile = tempFile;
		this.result = result;
	}

}