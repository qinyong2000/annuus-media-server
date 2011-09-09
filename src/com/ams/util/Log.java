package com.ams.util;

import java.util.logging.*;

public final class Log {
	public static final Logger logger = Logger.getLogger(Log.class.getName());
	static {
		try {
			FileHandler handle = new FileHandler("ams_%g.log", 100 * 1024, 10,
					true);
			handle.setFormatter(new SimpleFormatter());
			logger.addHandler(handle);
			logger.setLevel(Level.ALL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
