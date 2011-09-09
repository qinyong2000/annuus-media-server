package com.ams.http;

public final class HTTP {
	/** HTTP method */
	public final static int HTTP_METHOD_GET = 0;
	public final static int HTTP_METHOD_POST = 1;
	public final static int HTTP_METHOD_HEAD = 2;
	public final static int HTTP_METHOD_PUT = 3;
	public final static int HTTP_METHOD_DELETE = 4;
	public final static int HTTP_METHOD_TRACE = 5;
	public final static int HTTP_METHOD_OPTIONS = 6;

	/** HTTP Status-Code */
	public final static int HTTP_OK = 200;
	public final static int HTTP_MOVED_PERMANENTLY = 301;
	public final static int HTTP_BAD_REQUEST = 400;
	public final static int HTTP_UNAUTHORIZED = 401;
	public final static int HTTP_FORBIDDEN = 403;
	public final static int HTTP_NOT_FOUND = 404;
	public final static int HTTP_BAD_METHOD = 405;
	public final static int HTTP_LENGTH_REQUIRED = 411;
	public final static int HTTP_INTERNAL_ERROR = 500;

	/** HTTP header definitions */
	public static final String HEADER_ACCEPT = "Accept";
	public static final String HEADER_ACCEPT_CHARSET = "Accept-Charset";
	public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
	public static final String HEADER_AGE = "Age";
	public static final String HEADER_ALLOW = "Allow";
	public static final String HEADER_AUTHORIZATION = "Authorization";
	public static final String HEADER_CACHE_CONTROL = "Cache-Control";
	public static final String HEADER_CONN_DIRECTIVE = "Connection";
	public static final String HEADER_CONTENT_LANGUAGE = "Content-Language";
	public static final String HEADER_CONTENT_LENGTH = "Content-Length";
	public static final String HEADER_CONTENT_LOCATION = "Content-Location";
	public static final String HEADER_CONTENT_MD5 = "Content-MD5";
	public static final String HEADER_CONTENT_RANGE = "Content-Range";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_DATE = "Date";
	public static final String HEADER_EXPECT = "Expect";
	public static final String HEADER_EXPIRES = "Expires";
	public static final String HEADER_FROM = "From";
	public static final String HEADER_HOST = "Host";
	public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String HEADER_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String HEADER_LAST_MODIFIED = "Last-Modified";
	public static final String HEADER_LOCATION = "Location";
	public static final String HEADER_MAX_FORWARDS = "Max-Forwards";
	public static final String HEADER_PRAGMA = "Pragma";
	public static final String HEADER_RANGE = "Range";
	public static final String HEADER_REFER = "Referer";
	public static final String HEADER_REFER_AFTER = "Retry-After";
	public static final String HEADER_SERVER = "Server";
	public static final String HEADER_UPGRADE = "Upgrade";
	public static final String HEADER_USER_AGENT = "User-Agent";
	public static final String HEADER_VARY = "Vary";
	public static final String HEADER_VIA = "Via";
	public static final String HEADER_WWW_AUTHORIZATION = "WWW-Authenticate";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String HEADER_COOKIE = "Cookie";
	public static final String HEADER_SET_COOKIE = "Set-Cookie";
	public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
	public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

	/** HTTP expectations */
	public static final String EXPECT_CONTINUE = "100-Continue";

	/** HTTP connection control */
	public static final String CONN_CLOSE = "Close";
	public static final String CONN_KEEP_ALIVE = "Keep-Alive";

	/** Transfer encoding definitions */
	public static final String CHUNK_CODING = "chunked";
	public static final String IDENTITY_CODING = "identity";

	/** Common charset definitions */
	public static final String UTF_8 = "UTF-8";
	public static final String UTF_16 = "UTF-16";
	public static final String US_ASCII = "US-ASCII";
	public static final String ASCII = "ASCII";
	public static final String ISO_8859_1 = "ISO-8859-1";

	/** Default charsets */
	public static final String DEFAULT_CONTENT_CHARSET = ISO_8859_1;
	public static final String DEFAULT_PROTOCOL_CHARSET = US_ASCII;

	/** Content type definitions */
	public final static String OCTET_STREAM_TYPE = "application/octet-stream";
	public final static String PLAIN_TEXT_TYPE = "text/plain";
	public final static String HTML_TEXT_TYPE = "html/text";
	public final static String CHARSET_PARAM = "; charset=";

	/** Default content type */
	public final static String DEFAULT_CONTENT_TYPE = OCTET_STREAM_TYPE;

}
