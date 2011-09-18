package com.ams.amf;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Amf3Deserializer {
	protected ArrayList<String> stringRefTable = new ArrayList<String>();
	protected ArrayList<AmfValue> objectRefTable = new ArrayList<AmfValue>();
	protected DataInputStream in;
	
	public Amf3Deserializer(DataInputStream in) {
		this.in = in;
	}

	private int readAmf3Int() throws IOException {
		byte b1 = in.readByte();
		if (b1 >= 0 && b1 <= 0x7f) {
			return b1;
		}
		byte b2 = in.readByte();
		if (b2 >= 0 && b2 <= 0x7f) {
			return (b1 & 0x7f) << 7 | b2;
		}
		byte b3 = in.readByte();
		if (b3 >= 0 && b3 <= 0x7f) {
			return (b1 & 0x7f) << 14 | (b2 & 0x7f) << 7 | b3 ;
		}
		byte b4 = in.readByte();
		return (b1 & 0x7f) << 22 | (b2 & 0x7f) << 15 | (b3 & 0x7f) << 8 | b4 ;
	}

	private String readAmf3String() throws IOException {
		int v = readAmf3Int();
		if (v == 1) {
			return "";
		}
		if ((v & 0x01) == 0) {
			return stringRefTable.get(v >> 1);
		}
		byte[] b = new byte[v >> 1];
		in.read(b);
		String str = new String(b, "UTF-8");
		stringRefTable.add(str);
		return str;
	}

	private AmfValue readAmf3Array() throws IOException, AmfException {
		int v = readAmf3Int();
		if ((v & 0x01) == 0) {	//ref
			return objectRefTable.get(v >> 1);
		}

		int len = v >> 1;
		String s = readAmf3String();
		if (s.equals("")) {	//Strict Array
			ArrayList<AmfValue> array = new ArrayList<AmfValue>();
			for(int i = 0; i < len; i++) {
				int k = in.readByte() & 0xFF;
				array.add(readByType(k));
			}
			AmfValue obj = new AmfValue(array);
			objectRefTable.add(obj);
			return obj;
		} else {		//assoc Array
			HashMap<String, AmfValue> hash = new HashMap<String, AmfValue>();
			String key = s;
			while(true) {
				int k = in.readByte() & 0xFF;
				if (k == 0x01) break;		// end of Object
				hash.put(key, readByType(k));
				key = readAmf3String();
			}
			AmfValue obj = new AmfValue(hash);
			objectRefTable.add(obj);
			return obj;
		}
	}

	private AmfValue readAmf3Object() throws IOException, AmfException {
		int v = readAmf3Int();
		if ((v & 0x01) == 0) {	//ref
			return objectRefTable.get(v >> 1);
		}
		readAmf3String();	//class name
		HashMap<String, AmfValue> hash = new HashMap<String, AmfValue>();
		while(true) {
			String key = readAmf3String();
			int k = in.readByte() & 0xFF;
			if (k == 0x01) break;		// end of Object
			hash.put(key, readByType(k));
		}
		AmfValue obj = new AmfValue(hash);
		objectRefTable.add(obj);
		return obj;
	}
	
	private AmfValue readByType(int type) throws IOException, AmfException {
		AmfValue amfValue = null;
		switch(type) {
		case 0x00:
			//This specifies the data in the AMF packet is a undefined.
			amfValue = new AmfValue();
			break;
		case 0x01:
			//This specifies the data in the AMF packet is a NULL value.
			amfValue = new AmfNull();
			break;
		case 0x02:
			//This specifies the data in the AMF packet is a false boolean value.
			amfValue = new AmfValue(false); 
			break;
		case 0x03:
			//This specifies the data in the AMF packet is a true boolean value.
			amfValue = new AmfValue(true); 
			break;
		case 0x04:
			//This specifies the data in the AMF packet is a integer value.
			amfValue = new AmfValue(readAmf3Int());
			break;
		case 0x05:
			//This specifies the data in the AMF packet is a double value.
			amfValue = new AmfValue(in.readDouble());
			break;
		case 0x06:
			//This specifies the data in the AMF packet is a string value.
			amfValue = new AmfValue(readAmf3String());
			break;
		case 0x07:
			//This specifies the data in the AMF packet is a xml doc value.
			// TODO
			break;
		case 0x08:
			//This specifies the data in the AMF packet is a date value.
			// TODO
			break;
		case 0x09:
			//This specifies the data in the AMF packet is a array value.
			amfValue = readAmf3Array();
			break;
		case 0x0A:
			//This specifies the data in the AMF packet is a object value.
			amfValue = readAmf3Object();
			break;
		case 0x0B:
			//This specifies the data in the AMF packet is a xml value.
			// TODO
			break;
		case 0x0C:
			//This specifies the data in the AMF packet is a byte array value.
			// TODO
			break;
		default:
			throw new AmfException("Unknown AMF3: " + type);
		}
		return amfValue;
	}
	
	public AmfValue read() throws IOException, AmfException {
		int type = in.readByte() & 0xFF;
		return readByType(type);
	}
}
