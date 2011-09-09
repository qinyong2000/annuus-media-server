package com.ams.rtmp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;
import com.ams.server.Connector;

public class RtmpHandShake {
	private final static int HANDSHAKE_SIZE = 0x600;
	private final static int STATE_UNINIT = 0;
	private final static int STATE_VERSION_SENT = 1;
	private final static int STATE_ACK_SENT = 2;
	private final static int STATE_HANDSHAKE_DONE = 3;

	private int state = STATE_UNINIT;
	private long handShakeTime;
	private long handShakeTime2;
	private byte[] handShake;

	private Connector conn;
	private ByteBufferInputStream in;
	private ByteBufferOutputStream out;
	
	public RtmpHandShake(RtmpConnection rtmp) {
		this.conn = rtmp.getConnector();
		this.in = conn.getInputStream();
		this.out = conn.getOutputStream();
	}
	
	private void readVersion() throws IOException, RtmpException {
		if( (in.readByte() & 0xFF) != 3 )	//version
			throw new RtmpException("Invalid Welcome");
	}

	private void writeVersion() throws IOException {
		out.writeByte(3);
	}

	private void writeHandshake() throws IOException {
		out.write32Bit(0);
		out.write32Bit(0);
		handShake = new byte[HANDSHAKE_SIZE - 8];
		Random rnd = new Random();
		rnd.nextBytes(handShake);
		out.write(handShake, 0, handShake.length);
	}
	
	private byte[] readHandshake() throws IOException {
		handShakeTime = in.read32Bit();
		handShakeTime2 = in.read32Bit();
		byte[] b = new byte[HANDSHAKE_SIZE - 8];
		in.read(b, 0, b.length);
		return b;
	}
	
	private void writeHandshake(byte[] b) throws IOException {
		out.write32Bit(handShakeTime); // TODO, time
		out.write32Bit(handShakeTime2); // TODO, time2
		out.write(b, 0, b.length);
	}
	
	public boolean doClientHandshake() throws IOException, RtmpException {
		boolean stateChanged = false;
		long available = conn.available();

		switch( state ) {
		case STATE_UNINIT:
			writeVersion();					//write C0 message
			writeHandshake();				//write c1 message
			state = STATE_VERSION_SENT;
			stateChanged = true;
			break;

		case STATE_VERSION_SENT:
			if( available < 1 + HANDSHAKE_SIZE ) break;
			readVersion();					//read S0 message
			byte[] hs1 = readHandshake();	//read S1 message
			writeHandshake(hs1);			//write C2 message

			state = STATE_ACK_SENT;
			stateChanged = true;
			break;
			
		case STATE_ACK_SENT:
			if(available < HANDSHAKE_SIZE)	break;
			byte[] hs2 = readHandshake();	//read S2 message
			if(!Arrays.equals(handShake, hs2)) {
				throw new RtmpException("Invalid Handshake");
			}
			
			state = STATE_HANDSHAKE_DONE;
			stateChanged = true;
			break;
		}
		return stateChanged;
	}
	
	public void doServerHandshake() throws IOException, RtmpException {
		long available = conn.available();

		switch( state ) {
		case STATE_UNINIT:
			if( available < 1 ) break;
			readVersion();					//read C0 message
			writeVersion();					//write S0 message
			writeHandshake();				//write S1 message
			state = STATE_VERSION_SENT;
			break;

		case STATE_VERSION_SENT:
			if( available < HANDSHAKE_SIZE ) break;

			byte[] hs1 = readHandshake();	//read C1 message
			writeHandshake(hs1);			//write S2 message
			
			state = STATE_ACK_SENT;
			break;
			
		case STATE_ACK_SENT:
			if(available < HANDSHAKE_SIZE)	break;

			byte[] hs2 = readHandshake();	//read C2 message
			if(!Arrays.equals(handShake, hs2)) {
				throw new RtmpException("Invalid Handshake");
			}
			
			state = STATE_HANDSHAKE_DONE;
			break;
		}
	}

	public boolean isHandshakeDone() {
		return (state == STATE_HANDSHAKE_DONE);
	}
}
