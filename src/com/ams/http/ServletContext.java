package com.ams.http;


import java.io.File;
import java.io.IOException;

public final class ServletContext {
	private final File contextRoot;

	public ServletContext(String root) {
		contextRoot = new File(root);
	}

	public String getMimeType(String file) {
		int index = file.lastIndexOf('.');
		return (index++ > 0)
			? MimeTypes.getContentType(file.substring(index))
			: "unkown/unkown";
	}

	public String getRealPath(String path) {
		return new File(contextRoot, path).getAbsolutePath();
	}
	
	// security check
	public boolean securize(File file) throws IOException {
		if (file.getCanonicalPath().startsWith(contextRoot.getCanonicalPath())) {
			return true;
		}
		return false;
	}
}
