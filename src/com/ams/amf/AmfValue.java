package com.ams.amf;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class AmfValue {
	public final static int AMF_INT = 1;
	public final static int AMF_NUMBER = 2;
	public final static int AMF_BOOL = 3;
	public final static int AMF_STRING = 4;
	public final static int AMF_OBJECT = 5;
	public final static int AMF_ARRAY = 6;
	public final static int AMF_DATE = 7;
	public final static int AMF_XML = 8;
	public final static int AMF_NULL = 9;
	public final static int AMF_UNDEFINED = 0;

	protected int kind = 0;
	protected Object value;

	public AmfValue() {
		this.kind = AMF_UNDEFINED;
	}
		
	public AmfValue(int value) {
		this.kind  = AMF_INT;
		this.value = value; 
	}

	public AmfValue(double value) {
		this.kind  = AMF_NUMBER;
		this.value = value; 
	}

	public AmfValue(boolean value) {
		this.kind  = AMF_BOOL;
		this.value = value; 
	}

	public AmfValue(String value) {
		this.kind  = AMF_STRING;
		this.value = value; 
	}
	
	public AmfValue(HashMap<String, AmfValue> value) {
		this.kind  = AMF_OBJECT;
		this.value = value; 
	}

	public AmfValue(ArrayList<AmfValue> value) {
		this.kind  = AMF_ARRAY;
		this.value = value; 
	}
	
	public AmfValue(Date value) {
		this.kind  = AMF_DATE;
		this.value = value; 
	}
	
	public int getKind() {
		return kind;
	}

	public int integer() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_INT && kind != AmfValue.AMF_NUMBER ) {
			throw new IllegalArgumentException("parameter is not a Amf Integer or Amf Number");
		}
		return ((Number)value).intValue();
	}

	public double number() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_INT && kind != AmfValue.AMF_NUMBER ) {
			throw new IllegalArgumentException("parameter is not a Amf Integer or Amf Number");
		}
		return ((Number)value).doubleValue();
	}

	public boolean bool() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_BOOL ) {
			throw new IllegalArgumentException("parameter is not a Amf Bool");
		}
		return (Boolean)value;
	}
	
	public String string() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_STRING ) {
			throw new IllegalArgumentException("parameter is not a Amf String");
		}
		return (String)value;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<AmfValue> array() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_ARRAY ) {
			throw new IllegalArgumentException("parameter is not a Amf Array");
		}
		return (ArrayList<AmfValue>)value;
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, AmfValue> object() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_ARRAY && kind != AmfValue.AMF_OBJECT) {
			throw new IllegalArgumentException("parameter is not a Amf Object");
		}
		return (HashMap<String, AmfValue>)value;
	}

	public Date date() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_DATE ) {
			throw new IllegalArgumentException("parameter is not a Amf Date");
		}
		return (Date)value;
	}

	public String xml() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_XML ) {
			throw new IllegalArgumentException("parameter is not a Amf Xml");
		}
		return (String)value;
	}

	public boolean isNull() {
		return kind == AmfValue.AMF_NULL;
	}

	public boolean isUndefined() {
		return kind == AmfValue.AMF_UNDEFINED;
	}
	
	public String toString() {
		String result;
		boolean first;
		switch(kind) {
		case AmfValue.AMF_INT:
			return ((Integer)value).toString();
		case AmfValue.AMF_NUMBER:
			return ((Number)value).toString();
		case AmfValue.AMF_BOOL:
			return ((Boolean)value).toString();
		case AmfValue.AMF_STRING:
			return "'" + (String)value + "'";
		case AmfValue.AMF_OBJECT:
			HashMap<String, AmfValue> v = object();
			result = "{";
			first = true;
			for(String key : v.keySet()) {
				result += (first ? " " : ", ") + key + " => " + v.get(key).toString();
				first = false;
			}
			result += "}";
			return result;
		case AmfValue.AMF_ARRAY:
			ArrayList<AmfValue> array = array();
			result = "[";
			first = true;
			int len = array.size();
			for(int i = 0; i < len; i++) {
				result += (first ? " " : ", ") + array.get(i).toString();
				first = false;
			}
			result += "]";
			return result;
		case AmfValue.AMF_DATE:
			return ((Date)value).toString();
		case AmfValue.AMF_XML:
			return (String)value;
		case AmfValue.AMF_NULL:
			return "null";
		case AmfValue.AMF_UNDEFINED:
			return "undefined";
		}
		return "";
	}
}
