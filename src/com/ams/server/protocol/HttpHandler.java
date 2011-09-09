package com.ams.server.protocol;

import java.io.IOException;

import com.ams.http.DefaultServlet;
import com.ams.http.HttpRequest;
import com.ams.http.HttpResponse;
import com.ams.http.ServletContext;
import com.ams.server.Connector;
import com.ams.util.Log;

public class HttpHandler implements IProtocolHandler {
	private Connector conn;
	private ServletContext context;

	public HttpHandler(String contextRoot) throws IOException {
		this.context = new ServletContext(contextRoot);
	}

	public HttpHandler(ServletContext context) {
		this.context = context;
	}
	
	public void run() {
		try {
			Log.logger.info("http handler start");

			HttpRequest request = new HttpRequest(conn.getInputStream());
			HttpResponse response = new HttpResponse(conn.getOutputStream());
			DefaultServlet servlet = new DefaultServlet(context);

			request.parse();
			servlet.service(request, response);
			if (request.isKeepAlive()) {
				conn.setKeepAlive(true);
			}
			conn.flush();
			conn.close();
			Log.logger.info("http handler end");
		} catch (IOException e) {
			Log.logger.warning(e.getMessage());
		}
	}
	
	public void clear() {
		conn.close();
	}

	public IProtocolHandler newInstance(Connector conn) {
		IProtocolHandler handle = new HttpHandler(context);
		handle.setConnection(conn);
		return handle;
	}

	public void setConnection(Connector conn) {
		this.conn = conn;
	}

	public boolean isKeepAlive() {
		return false;
	}

}