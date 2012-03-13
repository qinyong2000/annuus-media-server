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
	
	public String getRealPath(String app, String path, String type) {
		if ("unkown/unkown".equals(MimeTypes.getMimeType(path))) {
			if (type == null || "".equals(type)) {
				path += ".flv";
			} else {
				path += "." + type;
			}
		}
		if (app !=null && app.length() > 0) {
			return new File(contextRoot,  app + File.separatorChar + path).getAbsolutePath();
		}
		return new File(contextRoot, path).getAbsolutePath();
	}

}
