package com.ams.rtmp.net;


import java.io.File;
import java.util.HashMap;


public final class NetContext {
	private final String contextRoot;
	private HashMap<String, String> attributes;
	
	public NetContext(String root) {
		contextRoot = root;
		attributes = new HashMap<String, String>();
	}

	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}

	public String getAttribute(String key) {
		return attributes.get(key);
	}
	
	public String getMimeType(String file) {
		int index = file.lastIndexOf('.');
		return (index++ > 0)
			? MimeTypes.getContentType(file.substring(index))
			: "unkown/unkown";
	}

	public String getRealPath(String app, String path) {
		if ("unkown/unkown".equals(getMimeType(path))) {
			path += ".flv";
		}
		if (app !=null && app.length() > 0) {
			return new File(contextRoot,  app + File.separatorChar + path).getAbsolutePath();
		}
		return new File(contextRoot, path).getAbsolutePath();
	}

}
