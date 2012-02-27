package com.ams.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import com.ams.config.Configuration;
import com.ams.util.Log;

public class Main {
	public static void main(String[] args) {
		System.setSecurityManager(null);
		DaemonThread daemon = null;
		
		Configuration config = new Configuration();
		try {
			if (!config.read()) {
				Log.logger.info("read config error!");
				return;
			}
		} catch (FileNotFoundException e) {
			Log.logger.info("Not found server.conf file, using default setting!");
			return;
		}
		
		
		try {
			daemon = new DaemonThread(config);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (daemon != null) {
			daemon.start();
		}
		// send control command to daemon thread
		String cmd = args.length == 1 ? args[0] : "start";
		if ("start".equalsIgnoreCase(cmd) ||
			"stop".equalsIgnoreCase(cmd) ||	
			"restart".equalsIgnoreCase(cmd)) {
			try{
				Socket socket = new Socket("127.0.0.1", config.getCommandPort());
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println(cmd);
				out.close();
				socket.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
