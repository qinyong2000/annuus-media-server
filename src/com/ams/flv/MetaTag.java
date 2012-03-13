package com.ams.flv;

import java.io.DataInputStream;
import java.io.IOException;
import com.ams.amf.Amf0Deserializer;
import com.ams.amf.AmfException;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;
import com.ams.message.MediaSample;

public class MetaTag extends MediaSample {
	private String event = null;
	private AmfValue metaData = null;
	
	public MetaTag(long timestamp, ByteBufferArray data) {
		super(MediaSample.SAMPLE_META, timestamp, data);
	}

	public MetaTag(long timestamp, long offset, int size) {
		super(MediaSample.SAMPLE_META, timestamp, false, offset, size);
	}
	
	public void getParameters() throws IOException {
		ByteBufferInputStream bi = new ByteBufferInputStream(data.duplicate());
		Amf0Deserializer amf0 = new Amf0Deserializer(new DataInputStream(bi));
		AmfValue value;
		try {
			value = amf0.read();
			event = value.string();
			metaData = amf0.read();
		} catch (AmfException e) {
			e.printStackTrace();
		}
	}

	public String getEvent() {
		return event;
	}

	public AmfValue getMetaData() {
		return metaData;
	}
}
