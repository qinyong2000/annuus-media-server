package com.ams.so;

public class SoEventRequestRemove extends SoEvent {
	private String name;

	public SoEventRequestRemove(String name) {
		super(SO_EVT_REQUEST_REMOVE);
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
}
