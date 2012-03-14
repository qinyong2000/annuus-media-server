package com.ams.rtmp.net;

import java.util.HashMap;

public class MimeTypes {
	public static HashMap<String, String> mimeMap = new HashMap<String, String>();
	static {
		mimeMap.put("", "content/unknown");
		mimeMap.put("f4v", "video/mp4");
		mimeMap.put("f4p", "video/mp4");
		mimeMap.put("f4a", "video/mp4");
		mimeMap.put("f4b", "video/mp4");
		mimeMap.put("mp4", "video/mp4");
		mimeMap.put("flv", "video/x-flv ");
		
	};
	
	public static String getContentType(String extension) {
		String contentType = mimeMap.get(extension);
		if (contentType == null)
			contentType = "unkown/unkown";
		return contentType;
	}
	
	public static String getMimeType(String file) {
		int index = file.lastIndexOf('.');
		return (index++ > 0)
			? MimeTypes.getContentType(file.substring(index))
			: "unkown/unkown";
	}
	
}