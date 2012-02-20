package com.ams.so;

import java.util.Map;

import com.ams.amf.AmfValue;

public class SoEventChange extends SoEvent {
	private Map<String, AmfValue> data;

	public SoEventChange(Map<String, AmfValue> data) {
		super(SO_EVT_CHANGE);
		this.data = data;
	}

	public Map<String, AmfValue> getData() {
		return data;
	}
}
