package com.ams.so;

public class SoEventStatus extends SoEvent {
	private String msg;
	private String msgType;
	
	public SoEventStatus(String msg, String msgType) {
		super(SO_EVT_STATUS);
		this.msg = msg;
		this.msgType = msgType;
	}

	public String getMsg() {
		return msg;
	}

	public String getMsgType() {
		return msgType;
	}
}
