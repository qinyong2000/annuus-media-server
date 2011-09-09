package com.ams.http;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.ams.util.Log;
import com.ams.util.ObjectCache;

public class DefaultServlet {
	private class MapedFile {
		public long size;
		public String contentType;
		public long lastModified;
		public ByteBuffer data;
	}

	private ServletContext context = null;
	private static ObjectCache<MapedFile> fileCache = new ObjectCache<MapedFile>();

	public DefaultServlet(ServletContext context) {
		this.context = context;
	}

	public void service(HttpRequest req, HttpResponse res) throws IOException {
		String realPath = null;
		try {
			realPath = context.getRealPath(req.getLocation());
		} catch (Exception e) {
			e.printStackTrace();
			Log.logger.warning(e.getMessage());
		}

		File file = new File(realPath);
		if (!file.exists()) {
			res.setHttpResult(HTTP.HTTP_NOT_FOUND);
			res.flush();
		} else if (!context.securize(file)) {
			res.setHttpResult(HTTP.HTTP_FORBIDDEN);
			res.flush();
		} else {
			if (!writeFile(req.getLocation(), file, res)) {
				res.setHttpResult(HTTP.HTTP_INTERNAL_ERROR);
				res.flush();
			}
		}
	}

	private boolean writeFile(String url, File file, HttpResponse res) {
		boolean result = true;
		try {
			MapedFile mapedFile = (MapedFile) fileCache.get(url);
			if (mapedFile == null) {
				// open the resource stream
				mapedFile = new MapedFile();
				mapedFile.lastModified = file.lastModified();
				mapedFile.size = file.length();
				mapedFile.contentType = context.getMimeType(file.getName());
				FileChannel fileChannel = new FileInputStream(file)
						.getChannel();
				mapedFile.data = fileChannel.map(FileChannel.MapMode.READ_ONLY,
						0, fileChannel.size());
				fileChannel.close();
				fileCache.put(url, mapedFile, 60);
			}

			res.setContentLength(mapedFile.size);
			res.setContentType(mapedFile.contentType);
			res.setLastModified(mapedFile.lastModified);
			res.setHttpResult(HTTP.HTTP_OK);

			// read all bytes and send them
			res.flush(mapedFile.data.slice());

		} catch (IOException e) {
			result = false;
			Log.logger.warning(e.getMessage());
		}
		return result;
	}

}