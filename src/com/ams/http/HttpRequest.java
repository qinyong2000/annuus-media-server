package com.ams.http;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import com.ams.io.ByteBufferInputStream;

public class HttpRequest {
	private ByteBufferInputStream in = null;

	private int method;
	private String location;
	private String rawGet;
	private String rawPost;

	private Map<String, String> headers = new LinkedHashMap<String, String>();
	private Map<String, String> cookies = new LinkedHashMap<String, String>();
	private Map<String, String> getValues = new LinkedHashMap<String, String>();
	private Map<String, String> postValues = new LinkedHashMap<String, String>();
	private Map<String, HttpFileUpload> postFiles = new LinkedHashMap<String, HttpFileUpload>();

	private static SimpleDateFormat dateFormatGMT;
	static {
		dateFormatGMT = new SimpleDateFormat("d MMM yyyy HH:mm:ss 'GMT'");
		dateFormatGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public HttpRequest(ByteBufferInputStream in) throws IOException {
		super();
		this.in = in;
	}

	public void clear() {
		method = -1;
		location = null;
		rawGet = null;
		rawPost = null;
		headers.clear();
		cookies.clear();
		getValues.clear();
		postValues.clear();
		postFiles.clear();
	}

	public void parse() throws IOException {
		if (in == null) {
			throw new IOException("no input stream");
		}
		if (HTTP.HTTP_OK != parseHttp()) {
			throw new IOException("bad http");
		}
	}

	private void parseRawString(String options, Map<String, String> output,
			String seperator) {
		String[] tokens = options.split(seperator);

		for (int i = 0; i < tokens.length; i++) {
			String[] items = tokens[i].split("=");
			String key = "";
			String value = "";
			key = items[0];

			if (items.length > 1) {
				try {
					value = URLDecoder.decode(items[1], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

			output.put(key, value);
		}
	}

	private int parseRequestLine() throws IOException {
		// first read and parse the first line
		String line = in.readLine();

		// parse the request line
		String[] tokens = line.split(" ");

		if (tokens.length < 2) {
			return HTTP.HTTP_BAD_REQUEST;
		}

		// get the method
		String token = tokens[0].toUpperCase();

		if ("GET".equals(token)) {
			method = HTTP.HTTP_METHOD_GET;
		} else if ("POST".equals(token)) {
			method = HTTP.HTTP_METHOD_POST;
		} else if ("HEAD".equals(token)) {
			method = HTTP.HTTP_METHOD_HEAD;
		} else {
			return HTTP.HTTP_BAD_METHOD;
		}

		// get the raw url
		String rawUrl = tokens[1];

		// parse the get methods
		// remove anchor tag
		location = (rawUrl.indexOf('#') > 0) ? rawUrl.split("#")[0] : rawUrl;
		// get 'GET' part
		rawGet = "";

		if (location.indexOf('?') > 0) {
			tokens = location.split("\\?");
			location = tokens[0];

			rawGet = tokens[1];
			parseRawString(rawGet, getValues, "&");
		}

		// return ok
		return HTTP.HTTP_OK;
	}

	private int parseHeader() throws IOException {
		String line = null;

		// parse the header
		while (((line = in.readLine()) != null) && !line.equals("")) {
			String[] tokens = line.split(": ");
			headers.put(tokens[0].toLowerCase(), tokens[1].toLowerCase());
		}

		// get cookies
		if (headers.containsKey("cookie")) {
			parseRawString(headers.get("cookie"), cookies, ";");
		}

		return HTTP.HTTP_OK;
	}

	private int parseMessageBody() throws IOException {
		// if method is post, parse the post values
		if (method == HTTP.HTTP_METHOD_POST) {
			String contentType = headers.get("content-type");

			// if multi-form part
			if ((contentType != null)
					&& contentType.startsWith("multipart/form-data")) {

				rawPost = "";

				int result = parseMultipartBody();

				if (result != HTTP.HTTP_OK) {
					return result;
				}
			} else {
				if (!headers.containsKey("content-length")) {
					return HTTP.HTTP_LENGTH_REQUIRED;
				}

				int len = Integer.parseInt(
						headers.get("content-length"), 10);
				byte[] buf = new byte[len];
				in.read(buf, 0, len);

				rawPost = new String(buf, "UTF-8");
				parseRawString(rawPost, postValues, "&");
			}
		} // handle POST end
		return HTTP.HTTP_OK;
	}

	private int parseMultipartBody() throws IOException {
		String contentType = headers.get("content-type");
		String boundary = "--"
				+ contentType.replaceAll("^.*boundary=\"?([^\";,]+)\"?.*$",
						"$1");

		int blength = boundary.length();

		if ((blength > 80) || (blength < 2)) {
			return HTTP.HTTP_BAD_REQUEST;
		}

		boolean done = false;

		String line = in.readLine();

		while (!done && (line != null)) {
			if (boundary.equals(line)) {
				line = in.readLine();
			}

			// parse the header
			HashMap<String, String> item = new HashMap<String, String>();

			while (!line.equals("")) {
				String[] tokens = line.split(": ");
				item.put(tokens[0].toLowerCase(), tokens[1].toLowerCase());
				line = in.readLine();
			}

			// read body
			String disposition = item.get("content-disposition");
			String name = disposition.replaceAll("^.* name=\"?([^\";]*)\"?.*$",
					"$1");
			String filename = disposition.replaceAll(
					"^.* filename=\"?([^\";]*)\"?.*$", "$1");
			if (disposition.equals(filename)) {
				filename = null;
			}

			// normal field
			if (filename == null) {
				String value = "";

				// shouldn't be used more than once.
				while (((line = in.readLine()) != null)
						&& !line.startsWith(boundary)) {
					value += line;
				}

				// read end
				if ((boundary + "--").equals(line)) {
					done = true;
				}

				// save post field
				postValues.put(name, value);

				// make rawPost
				if (rawPost.length() > 0) {
					rawPost += "&";
				}

				try {
					rawPost += name + "=" + URLEncoder.encode(value, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else { // read file data

				// should we create a temporary file ?
				File tempfile = File.createTempFile("httpd", ".file");
				FileOutputStream out = new FileOutputStream(tempfile);

				// read the data until we much the content boundry
				byte[] buf = new byte[boundary.length() + 2];
				byte[] bound = ("\r\n" + boundary).getBytes();

				int len = in.read(buf, 0, buf.length);
				if (len < buf.length) {
					return HTTP.HTTP_BAD_REQUEST;
				}

				while (!Arrays.equals(buf, bound)) { // check boundry
					out.write(buf[0]); // write a byte to file

					// shift buffer to left
					System.arraycopy(buf, 1, buf, 0, buf.length - 1);

					// read a byte
					if (0 > in.read(buf, buf.length - 1, 1)) {
						return HTTP.HTTP_BAD_REQUEST;
					}
				}

				// close tempofle
				if (tempfile != null) {
					out.close();
				}

				// result of reading file
				int fileResult = HttpFileUpload.RESULT_NOFILE;

				if (tempfile.length() > HttpFileUpload.MAXSIZE_FILE_UPLOAD) {
					fileResult = HttpFileUpload.RESULT_SIZE;
				} else if (tempfile.length() > 0) {
					fileResult = HttpFileUpload.RESULT_OK;
				}

				postFiles.put(name, new HttpFileUpload(filename, tempfile,
						fileResult));

				// read the newline and check the end
				int ch = in.read();

				if (ch != '\r') {
					if ((ch == '-') && (in.read() == '-')) { // is "--"
						done = true;
					}
				} else { // '\n'
					ch = in.read();
				}

				if (ch == -1) { // eof
					done = true;
				}

				line = in.readLine();
			} // if normal field or file
		} // while readLine

		// return ok
		return HTTP.HTTP_OK;
	}

	private int parseHttp() throws IOException {
		// parse request line
		int result = parseRequestLine();
		if (result != HTTP.HTTP_OK) {
			return result;
		}

		// parse eader part
		result = parseHeader();
		if (result != HTTP.HTTP_OK) {
			return result;
		}

		// parse body part
		result = parseMessageBody();
		if (result != HTTP.HTTP_OK) {
			return result;
		}
		return HTTP.HTTP_OK;
	}

	public int getMethod() {
		return method;
	}

	public boolean isKeepAlive() {
		String connection = getHeader("connection");

		if (connection != null) {
			return connection.charAt(0) == 'k'; // keep-alive or close
		}
		return false;
	}

	public String getHeader(String key) {
		return headers.get(key);
	}

	public String getCookie(String key) {
		return cookies.get(key);
	}

	public String getGet(String key) {
		return getValues.get(key);
	}

	public String getPost(String key) {
		return postValues.get(key);
	}

	public String getParameter(String key) {
		String value = getGet(key);
		if (value == null) {
			value = getPost(key);
		}
		if (value == null) {
			value = getCookie(key);
		}
		return value;
	}

	public String getLocation() {
		return location;
	}

	public Map<String, String> getCookies() {
		return cookies;
	}

	public Map<String, String> getGetValues() {
		return getValues;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Map<String, HttpFileUpload> getPostFiles() {
		return postFiles;
	}

	public Map<String, String> getPostValues() {
		return postValues;
	}

	public String getRawGet() {
		return rawGet;
	}

	public String getRawPost() {
		return rawPost;
	}

}
