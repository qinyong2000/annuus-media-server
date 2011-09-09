package com.ams.amf;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.io.IOException;

public class Amf0Deserializer {
	protected ArrayList<AmfValue> storedObjects = new ArrayList<AmfValue>();
	protected ArrayList<AmfValue> storedStrings = new ArrayList<AmfValue>();
	protected DataInputStream in;
	
	public Amf0Deserializer(DataInputStream in) {
		this.in = in;
	}

	private String readLongString() throws IOException {
		int len = in.readInt();		//32bit read
		byte[] buf = new byte[len];
		in.read(buf, 0, len);
		return new String(buf, "UTF-8");
	}

	private AmfValue readByType(int type) throws IOException, AmfException {
		AmfValue amfValue = null;
		switch(type) {
		case 0x00:
			//This specifies the data in the AMF packet is a numeric value. 
			//All numeric values in Flash are 64 bit, big-endian.
			amfValue = new AmfValue(in.readDouble());
			break;
		case 0x01:
			//This specifies the data in the AMF packet is a boolean value.
			amfValue = new AmfValue(in.readBoolean()); 
			break;
		case 0x02:
			//This specifies the data in the AMF packet is an ASCII string.
			amfValue = new AmfValue(in.readUTF());
			break;
		case 0x04:
			//This specifies the data in the AMF packet is a Flash movie.
			break;
		case 0x05:
			//This specifies the data in the AMF packet is a NULL value.
			amfValue = new AmfNull();
			break;
		case 0x06:
			//This specifies the data in the AMF packet is a undefined.
			amfValue = new AmfValue();
			break;
		case 0x07:
			//This specifies the data in the AMF packet is a reference.
			break;
		case 0x03:
			//This specifies the data in the AMF packet is a Flash object. 
		case 0x08:
			//This specifies the data in the AMF packet is a ECMA array.
			HashMap<String, AmfValue> hash = new HashMap<String, AmfValue>();
			boolean ismixed = (type == 0x08);
			int size = -1;
			if(ismixed) {
				size = in.readInt();		// 32bit read
			}
			while(true) {
				String key = in.readUTF();
				int k = in.readByte() & 0xFF;
				if (k == 0x09) break;		// end of Object
				hash.put(key, readByType(k));
			}
			amfValue = new AmfValue(hash);
			break;
		case 0x09:
			//This specifies the data in the AMF packet is the end of an object definition.
			break;
		case 0x0A:
			//This specifies the data in the AMF packet is a Strict array.
			ArrayList<AmfValue> array = new ArrayList<AmfValue>();
			int len = in.readInt();
			for(int i = 0; i < len; i++) {
				int k = in.readByte() & 0xFF;
				array.add(readByType(k));
			}
			amfValue = new AmfValue(array);
			break;
		case 0x0B:
			//This specifies the data in the AMF packet is a date. 
			double time_ms = in.readDouble();
			int tz_min = in.readInt();	//16bit
			amfValue = new AmfValue(new Date((long)(time_ms + tz_min * 60 * 1000.0)));
			break;
		case 0x0C:
			//This specifies the data in the AMF packet is a multi-byte string.
			amfValue = new AmfValue(readLongString());	//32bit
		case 0x0D:
			//This specifies the data in the AMF packet is a an unsupported feature. 
			break;
		case 0x0E:
			//This specifies the data in the AMF packet is a record set. 
			break;
		case 0x0F:
			//This specifies the data in the AMF packet is a XML object.
			amfValue = new AmfXml(readLongString());	//32bit
			break;
		case 0x10:
			//This specifies the data in the AMF packet is a typed object. 
		case 0x11:
			//the AMF 0 format was extended to allow an AMF 0 encoding context to be switched to AMF 3.
			throw new AmfSwitchToAmf3Exception("Switch To AMF3");
		default:
			throw new AmfException("Unknown AMF0: " + type);
		}
		return amfValue;
	}

	public AmfValue read() throws IOException, AmfException {
		int type = in.readByte() & 0xFF;
		return readByType(type);
	}
}
