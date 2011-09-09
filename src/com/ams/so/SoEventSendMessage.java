package com.ams.so;

import com.ams.amf.AmfValue;

public class SoEventSendMessage extends SoEvent {
	private AmfValue msg;

	public SoEventSendMessage(AmfValue msg) {
		super(SO_EVT_SEND_MESSAGE);
		this.msg = msg;
	}

	public AmfValue getMsg() {
		return msg;
	}
}
