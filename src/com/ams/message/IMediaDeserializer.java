package com.ams.message;

import java.io.IOException;

public interface IMediaDeserializer {
	public MediaSample metaData();
	public MediaSample videoHeaderData();
	public MediaSample audioHeaderData();
	public MediaSample seek(long seekTime) throws IOException;
	public MediaSample readNext() throws IOException;
	public void close() throws IOException;
}
