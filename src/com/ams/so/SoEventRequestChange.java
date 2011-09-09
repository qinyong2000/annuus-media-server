package com.ams.so;

import com.ams.amf.AmfValue;

public class SoEventRequestChange extends SoEvent {
	private String name;
	private AmfValue value;
	
	public SoEventRequestChange(String name, AmfValue value) {
		super(SO_EVT_REQUEST_CHANGE);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public AmfValue getValue() {
		return value;
	}
}
