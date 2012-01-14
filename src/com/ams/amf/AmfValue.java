package com.ams.amf;

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
	protected boolean ecmaArray = false;
	
	public AmfValue() {
		this.kind = AMF_UNDEFINED;
	}

	public AmfValue(Object value) {
		if (value == null)
			this.kind = AMF_NULL;
		else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
			this.kind  = AMF_INT;
		else if (value instanceof Float || value instanceof Double)
			this.kind  = AMF_NUMBER;
		else if (value instanceof Boolean)
			this.kind  = AMF_BOOL;
		else if (value instanceof String)
			this.kind  = AMF_STRING;
		else if (value instanceof HashMap)
			this.kind  = AMF_OBJECT;
		else if (value instanceof Object[])
			this.kind  = AMF_ARRAY;
		else if (value instanceof Date)
			this.kind  = AMF_DATE;
		
		this.value = value; 
	}
	
	public AmfValue put(String key, Object v) {
		object().put(key, v instanceof AmfValue ? (AmfValue) v : new AmfValue(v));
		return this;
	}

	public static AmfValue newObject() {
		return new AmfValue(new HashMap<String, AmfValue>());
	}

	public static AmfValue newEcmaArray() {
		AmfValue value = new AmfValue(new HashMap<String, AmfValue>());
		value.setEcmaArray(true);
		return value;
	}
	
	public static AmfValue newArray(Object ...values) {
		AmfValue[] array = new AmfValue[values.length];
		for(int i = 0; i < values.length; i++) {
			Object v = values[i];
			array[i] = v instanceof AmfValue ? (AmfValue) v : new AmfValue(v);  
		}
		return new AmfValue(array);
	}
	
	public static AmfValue[] array(Object ...values) {
		AmfValue[] array = new AmfValue[values.length];
		for(int i = 0; i < values.length; i++) {
			Object v = values[i];
			array[i] = v instanceof AmfValue ? (AmfValue) v : new AmfValue(v);  
		}
		return array;
	}

	public int getKind() {
		return kind;
	}

	public Integer integer() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_INT && kind != AmfValue.AMF_NUMBER ) {
			throw new IllegalArgumentException("parameter is not a Amf Integer or Amf Number");
		}
		return ((Number)value).intValue();
	}

	public Double number() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_INT && kind != AmfValue.AMF_NUMBER ) {
			throw new IllegalArgumentException("parameter is not a Amf Integer or Amf Number");
		}
		return ((Number)value).doubleValue();
	}

	public Boolean bool() {
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

	public AmfValue[] array() {
		if( value == null ) {
			throw new NullPointerException("parameter is null");
		}
		if( kind != AmfValue.AMF_ARRAY ) {
			throw new IllegalArgumentException("parameter is not a Amf Array");
		}
		return (AmfValue[])value;
	}
	
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
		case AmfValue.AMF_NUMBER:
		case AmfValue.AMF_BOOL:
		case AmfValue.AMF_DATE:
			return value.toString();
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
			AmfValue[] array = array();
			result = "[";
			first = true;
			int len = array.length;
			for(int i = 0; i < len; i++) {
				result += (first ? " " : ", ") + array[i].toString();
				first = false;
			}
			result += "]";
			return result;
		case AmfValue.AMF_XML:
			return (String)value;
		case AmfValue.AMF_NULL:
			return "null";
		case AmfValue.AMF_UNDEFINED:
			return "undefined";
		}
		return "";
	}

	public boolean isEcmaArray() {
		return ecmaArray;
	}

	public void setEcmaArray(boolean ecmaArray) {
		this.ecmaArray = ecmaArray;
	}
}
