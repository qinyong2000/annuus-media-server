package com.ams.so;

public class SoEventSuceess extends SoEvent {
	private String name;

	public SoEventSuceess(String name) {
		super(SO_EVT_SUCCESS);
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
}
