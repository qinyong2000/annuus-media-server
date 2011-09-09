package com.ams.amf;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.io.IOException;

public class Amf3Serializer {
	protected ArrayList<String> stringRefTable = new ArrayList<String>();
	protected ArrayList<AmfValue> objectRefTable = new ArrayList<AmfValue>();
	protected DataOutputStream out;
	
	public Amf3Serializer(DataOutputStream out) {
		this.out = out;
	}

	private void writeAmf3Int(int value) throws IOException {
		//Sign contraction - the high order bit of the resulting value must match every bit removed from the number
		//Clear 3 bits 
		value &= 0x1fffffff;
		if(value < 0x80) {
			out.writeByte(value);
		} else if(value < 0x4000) {
			out.writeByte(value >> 7 & 0x7f | 0x80);
			out.writeByte(value & 0x7f);
		} else if(value < 0x200000) {
			out.writeByte(value >> 14 & 0x7f | 0x80);
			out.writeByte(value >> 7 & 0x7f | 0x80);
			out.writeByte(value & 0x7f);
		} else {
			out.writeByte(value >> 22 & 0x7f | 0x80);
			out.writeByte(value >> 15 & 0x7f | 0x80);
			out.writeByte(value >> 8 & 0x7f | 0x80);
			out.writeByte(value & 0xff);
		}
	}

	private void writeAmf3RefInt(int value) throws IOException {
		writeAmf3Int(value << 1);		// low bit is 0
	}

	private void writeAmf3ValueInt(int value) throws IOException {
		writeAmf3Int(value << 1 + 1);		// low bit is 1
	}

	private void writeAmf3EmptyString() throws IOException {
		out.writeByte(0x01);
	}
	
	private void writeAmf3String(String s) throws IOException {
		if( s.length() == 0) {
			//Write 0x01 to specify the empty string
			writeAmf3EmptyString();
		} else {
			for(int i = 0; i < stringRefTable.size(); i++) {
				if (s.equals(stringRefTable.get(i))) {
					writeAmf3RefInt(i);
					return;
				}
			}
			byte[] b = s.getBytes("UTF-8");
			writeAmf3ValueInt(b.length);
			out.write(b);
			stringRefTable.add(s);
		}
	}
/*
	private void writeAmf3Object(AmfValue amfValue) throws IOException {
		for(int i = 0; i < objectRefTable.size(); i++) {
			if (amfValue.equals(objectRefTable.get(i))) {
				writeAmf3RefInt(i);
				return;
			}
		}
		out.writeByte(0x0B);		//dynamic object
		writeAmf3EmptyString();		//anonymous class
		HashMap<String, AmfValue> obj = amfValue.object();
		for(String key : obj.keySet()) {
			writeAmf3String(key);
			write((AmfValue) obj.get(key));
		}
		//end of Object
		writeAmf3EmptyString();
		objectRefTable.add(amfValue);
	}
*/
	private void writeAmf3Object(AmfValue amfValue) throws IOException {
		for(int i = 0; i < objectRefTable.size(); i++) {
			if (amfValue.equals(objectRefTable.get(i))) {
				writeAmf3RefInt(i);
				return;
			}
		}
		writeAmf3ValueInt(0);
		HashMap<String, AmfValue> obj = amfValue.object();
		for(String key : obj.keySet()) {
			writeAmf3String(key);
			write((AmfValue) obj.get(key));
		}
		//end of Object
		writeAmf3EmptyString();
		objectRefTable.add(amfValue);
	}

	private void writeAmf3Array(AmfValue amfValue) throws IOException {
		for(int i = 0; i < objectRefTable.size(); i++) {
			if (amfValue.equals(objectRefTable.get(i))) {
				writeAmf3RefInt(i);
				return;
			}
		}
		
		ArrayList<AmfValue> array = amfValue.array();
		int len = array.size();
		writeAmf3ValueInt(len);
		
		writeAmf3EmptyString();
		for(int i = 0; i < len; i++) {
			write((AmfValue) array.get(i));
		}
		objectRefTable.add(amfValue);
	}
	
	public void write(AmfValue amfValue) throws IOException {
		switch(amfValue.getKind()) {
		case AmfValue.AMF_INT:
			int v = amfValue.integer();
			if (v >= -0x10000000 && v <= 0xFFFFFFF) { //check valid range for 29bits
				out.writeByte(0x04);
				writeAmf3Int(v);
			} else {
				//overflow condition would occur upon int conversion
				out.writeByte(0x05);
				out.writeDouble(v);
			}
			break;
		case AmfValue.AMF_NUMBER:
			out.writeByte(0x05);
			out.writeDouble(amfValue.number());
			break;
		case AmfValue.AMF_BOOL:
			out.writeByte(amfValue.bool() ? 0x02 : 0x03); 
			break;
		case AmfValue.AMF_STRING:
			out.writeByte(0x06);
			writeAmf3String(amfValue.string());
			break;
		case AmfValue.AMF_OBJECT:
			//out.writeByte(0x0A);
System.out.println("amf3 obj");			
			out.writeByte(0x09);
			writeAmf3Object(amfValue);
			break;
		case AmfValue.AMF_ARRAY:
			out.writeByte(0x09);
			writeAmf3Array(amfValue);
			break;
		case AmfValue.AMF_DATE:
			out.writeByte(0x08);
			writeAmf3Int(0x01);
			Date d = amfValue.date();
			out.writeDouble(d.getTime());
			break;
		case AmfValue.AMF_XML:
			out.writeByte(0x07);
			writeAmf3String(amfValue.string());
			break;
		case AmfValue.AMF_NULL:
			out.writeByte(0x01);
			break;
		case AmfValue.AMF_UNDEFINED:
			out.writeByte(0x00);
			break;
		}
	}
}
