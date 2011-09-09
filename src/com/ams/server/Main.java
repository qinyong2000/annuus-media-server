package com.ams.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Main {
	public static void main(String[] args) {
		System.setSecurityManager(null);
		DaemonThread daemon = null;
		try {
			daemon = new DaemonThread();
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
				Socket socket = new Socket("127.0.0.1", 5555);
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
