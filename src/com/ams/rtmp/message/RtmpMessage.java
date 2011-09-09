package com.ams.rtmp.message;

public class RtmpMessage {
	/*	
	01  Protocol control message 1, Set Chunk Size  
	02  Protocol control message 2, Abort Message  
	03  Protocol control message 3, Acknowledgement  
	04  Protocol control message 4, User Control Message 
	05  Protocol control message 5, Window Acknowledgement Size 
	06  Protocol control message 6, Set Peer Bandwidth  
	07  Protocol control message 7, used between edge server and origin server  
	08  Audio Data  packet containing audio  
	09  Video Data  packet containing video data  
	0F  AMF3 data message
	11  AMF3 command message
	12  AMF0 data message  
	13  Shared Object  has subtypes  
	14  AMF0 command message
	16  Aggregate message
	      [FMS3] Set of one or more FLV tags, as documented on the Flash Video (FLV) page. 
			Each tag will have an 11 byte header - 
			[1 byte Type][3 bytes Size][3 bytes Timestamp][1 byte timestamp extention][3 bytes streamID], 
			followed by the body, followed by a 4 byte footer containing the size of the body.  
    */	
	
	public final static int MESSAGE_CHUNK_SIZE = 0x01;
	public final static int MESSAGE_ABORT = 0x02;
	public final static int MESSAGE_ACK = 0x03;
	public final static int MESSAGE_USER_CONTROL = 0x04;
	public final static int MESSAGE_WINDOW_ACK_SIZE = 0x05;
	public final static int MESSAGE_PEER_BANDWIDTH = 0x06;
	public final static int MESSAGE_AUDIO = 0x08;
	public final static int MESSAGE_VIDEO = 0x09;
	public final static int MESSAGE_AMF3_DATA = 0x0F;
	public final static int MESSAGE_AMF3_COMMAND = 0x11;
	public final static int MESSAGE_AMF0_DATA = 0x12;
	public final static int MESSAGE_AMF0_COMMAND = 0x14;
	public final static int MESSAGE_SHARED_OBJECT = 0x13;
	public final static int MESSAGE_AGGREGATE = 0x16;
	public final static int MESSAGE_UNKNOWN = 0xFF;
	
	protected int type = 0;
	
	public RtmpMessage(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

}
