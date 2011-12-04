package com.ams.flv;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.amf.Amf0Deserializer;
import com.ams.amf.AmfException;
import com.ams.amf.AmfValue;
import com.ams.io.ByteBufferInputStream;
import com.ams.util.ByteBufferHelper;

public class MetaTag extends FlvTag {
	private String event = null;
	private AmfValue metaData = null;
	
	public MetaTag(ByteBuffer[] data, long timestamp) {
		super(FlvTag.FLV_META, data, timestamp);
	}
	
	public void getParameters() throws IOException {
		ByteBufferInputStream bi = new ByteBufferInputStream(ByteBufferHelper.duplicate(data));
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
