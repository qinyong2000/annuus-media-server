package com.ams.rtmp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.ByteBufferOutputStream;
import com.ams.server.Connector;

public class RtmpHandShake {
	private final static int HANDSHAKE_SIZE = 0x600;
	private final static int STATE_UNINIT = 0;
	private final static int STATE_VERSION_SENT = 1;
	private final static int STATE_ACK_SENT = 2;
	private final static int STATE_HANDSHAKE_DONE = 3;
	
	private final static byte[] SECRET_KEY = { (byte) 0x47, (byte) 0x65,
        (byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65,
        (byte) 0x20, (byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62,
        (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c, (byte) 0x61,
        (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65,
        (byte) 0x64, (byte) 0x69, (byte) 0x61, (byte) 0x20, (byte) 0x53,
        (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
        (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0xf0,
        (byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68,
        (byte) 0xbe, (byte) 0xe8, (byte) 0x2e, (byte) 0x00, (byte) 0xd0,
        (byte) 0xd1, (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57,
        (byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29,
        (byte) 0x80, (byte) 0x6f, (byte) 0xab, (byte) 0x93, (byte) 0xb8,
        (byte) 0xe6, (byte) 0x36, (byte) 0xcf, (byte) 0xeb, (byte) 0x31,
        (byte) 0xae };
	private int state = STATE_UNINIT;
	private long handShakeTime;
	private long handShakeTime2;
	private byte[] handShake;
	
	private Mac hmacSHA256;
	private Connector conn;
	private ByteBufferInputStream in;
	private ByteBufferOutputStream out;
	
	public RtmpHandShake(RtmpConnection rtmp) {
		this.conn = rtmp.getConnector();
		this.in = conn.getInputStream();
		this.out = conn.getOutputStream();
        try {
            hmacSHA256 = Mac.getInstance("HmacSHA256");
	    } catch (Exception e) {
		}
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

    private byte[] calculateHmacSHA256(byte[] in, byte[] key) {
        byte[] out = null;
        try {
        	hmacSHA256.init(new SecretKeySpec(key, "HmacSHA256"));
            out = hmacSHA256.doFinal(in);
        } catch (Exception e) {
        }
        return out;
    }
    
	private byte[] getHandshake(byte[] b) {
		int index = ((b[0]&0x0ff) + (b[1]&0x0ff) + (b[2]&0x0ff) + (b[3]&0x0ff)) % 728 + 12;
		byte[] part = new byte[32];
		System.arraycopy(b, index, part, 0, 32);
		byte[] newKey = calculateHmacSHA256(part, SECRET_KEY);
		
		byte[] newHandShake = new byte[HANDSHAKE_SIZE - 8];
		Random rnd = new Random();
		rnd.nextBytes(newHandShake);
		byte[] newPart = calculateHmacSHA256(newHandShake, newKey);
		System.arraycopy(newPart, 0, newHandShake, newHandShake.length - 32, 32);
		return newHandShake;
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
			if (handShakeTime2 == 0) {
				writeHandshake(hs1);		//write S2 message
			} else {
				writeHandshake(getHandshake(hs1));		//write S2 message
			}
			
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
