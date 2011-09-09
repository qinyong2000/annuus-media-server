package com.ams.so;

import java.util.HashMap;

import com.ams.amf.AmfValue;

public class SoEventChange extends SoEvent {
	private HashMap<String, AmfValue> data;

	public SoEventChange(HashMap<String, AmfValue> data) {
		super(SO_EVT_CHANGE);
		this.data = data;
	}

	public HashMap<String, AmfValue> getData() {
		return data;
	}
}
