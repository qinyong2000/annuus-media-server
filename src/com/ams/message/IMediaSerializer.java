package com.ams.message;

import java.io.IOException;

public interface IMediaSerializer {
	public void write(MediaSample sample) throws IOException;
	public void close();
}
