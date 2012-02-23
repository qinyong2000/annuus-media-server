package com.ams.flv;

import java.io.IOException;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferArray;

public interface SampleDeserializer {
	public AmfValue metaData();
	public ByteBufferArray videoHeaderData();
	public ByteBufferArray audioHeaderData();
	public Sample seek(long seekTime) throws IOException;
	public Sample readNext() throws IOException;
	public void close() throws IOException;
}
