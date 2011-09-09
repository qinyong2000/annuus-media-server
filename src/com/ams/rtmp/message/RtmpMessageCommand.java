package com.ams.rtmp.message;

import com.ams.amf.AmfValue;

public class RtmpMessageCommand extends RtmpMessage {
	private String name;
	private int transactionId;
	private AmfValue[] args;
	
	public RtmpMessageCommand(String name, int transactionId, AmfValue[] args) {
		super(MESSAGE_AMF0_COMMAND);
		this.name = name;
		this.transactionId = transactionId;
		this.args = args;
	}

	public AmfValue[] getArgs() {
		return args;
	}

	public AmfValue getCommandObject() {
		return args[0];
	}
	
	public int getTransactionId() {
		return transactionId;
	}
	
	public String getName() {
		return name;
	}
}
