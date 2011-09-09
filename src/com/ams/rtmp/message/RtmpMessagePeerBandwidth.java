package com.ams.rtmp.message;

public class RtmpMessagePeerBandwidth extends RtmpMessage {
	private int windowAckSize;
	private byte limitType;
	

	public RtmpMessagePeerBandwidth(int windowAckSize, byte limitTypemitType) {
		super(MESSAGE_PEER_BANDWIDTH);
		this.windowAckSize = windowAckSize;
		this.limitType = limitTypemitType;
	}


	public int getWindowAckSize() {
		return windowAckSize;
	}


	public byte getLimitType() {
		return limitType;
	}

}
