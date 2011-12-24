package com.ams.flv;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.amf.AmfValue;

public interface SampleDeserializer {
	public AmfValue metaData();
	public ByteBuffer[] videoHeaderData();
	public ByteBuffer[] audioHeaderData();
	public Sample seek(long seekTime) throws IOException;
	public Sample readNext() throws IOException;
	public void close() throws IOException;
}
