package com.ams.amf;

import java.io.DataOutputStream;
import java.util.Date;
import java.util.Map;
import java.io.IOException;

public class Amf0Serializer {
	protected DataOutputStream out;
	
	public Amf0Serializer(DataOutputStream out) {
		this.out = out;
	}

	private void writeLongString(String s) throws IOException {
		byte[] b = s.getBytes("UTF-8");
		out.writeInt(b.length);
		out.write(b);
	}
	
	public void write(AmfValue amfValue) throws IOException {
		switch(amfValue.getKind()) {
		case AmfValue.AMF_INT:
		case AmfValue.AMF_NUMBER:
			out.writeByte(0x00);
			out.writeDouble(amfValue.number());
			break;
		case AmfValue.AMF_BOOL:
			out.writeByte(0x01);
			out.writeBoolean(amfValue.bool());
			break;
		case AmfValue.AMF_STRING:
			String s = amfValue.string();
			if( s.length() <= 0xFFFF ) {
				out.writeByte(0x02);
				out.writeUTF(s);
			} else {
				out.writeByte(0x0C);
				writeLongString(s);
			}
			break;
		case AmfValue.AMF_OBJECT:
			if (amfValue.isEcmaArray()) {
				out.writeByte(0x08);	// ECMA Array
				out.writeInt(0);
			} else {
				out.writeByte(0x03);
			}
			Map<String, AmfValue> v = amfValue.object();
			for(String key : v.keySet()) {
				out.writeUTF(key);
				write((AmfValue) v.get(key));
			}
			//end of Object
			out.writeByte(0);
			out.writeByte(0);
			out.writeByte(0x09);
			break;
		case AmfValue.AMF_ARRAY:
			out.writeByte(0x0A);
			AmfValue[] array = amfValue.array();
			int len = array.length;
			out.writeInt(len);
			for(int i = 0; i < len; i++) {
				write((AmfValue) array[i]);
			}
			break;
		case AmfValue.AMF_DATE:
			Date d = amfValue.date();
			out.writeDouble(d.getTime());
			out.writeShort(0); // loose TZ
			break;
		case AmfValue.AMF_XML:
			String xml = amfValue.xml();
			out.writeByte(0x0F);
			writeLongString(xml);
			break;
		case AmfValue.AMF_NULL:
			out.writeByte(0x05);
			break;
		case AmfValue.AMF_UNDEFINED:
			out.writeByte(0x06);
			break;
		}
	}
}
