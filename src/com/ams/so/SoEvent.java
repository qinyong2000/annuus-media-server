package com.ams.so;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ams.amf.Amf0Deserializer;
import com.ams.amf.Amf0Serializer;
import com.ams.amf.AmfException;
import com.ams.amf.AmfValue;

public class SoEvent {
	public static final int SO_EVT_USE = 1;
	public static final int SO_EVT_RELEASE = 2;
	public static final int SO_EVT_REQUEST_CHANGE = 3;
	public static final int SO_EVT_CHANGE = 4;
	public static final int SO_EVT_SUCCESS = 5;
	public static final int SO_EVT_SEND_MESSAGE = 6;
	public static final int SO_EVT_STATUS = 7;
	public static final int SO_EVT_CLEAR = 8;
	public static final int SO_EVT_REMOVE = 9;
	public static final int SO_EVT_REQUEST_REMOVE = 10;
	public static final int SO_EVT_USE_SUCCESS = 11;
	
	private int kind = 0;

	public SoEvent(int kind) {
		this.kind = kind;
	}

	public int getKind() {
		return kind;
	}

	public static SoEvent read(DataInputStream in, int kind, int size) throws IOException, AmfException {
		Amf0Deserializer deserializer = new Amf0Deserializer(in); 
		SoEvent event = null;
		switch(kind) {
		case SoEvent.SO_EVT_USE:
			event = new SoEvent(SoEvent.SO_EVT_USE);
			break;
		case SoEvent.SO_EVT_RELEASE:
			event = new SoEvent(SoEvent.SO_EVT_RELEASE);
			break;
		case SoEvent.SO_EVT_REQUEST_CHANGE:
			event = new SoEventRequestChange(in.readUTF(), deserializer.read());
			break;
		case SoEvent.SO_EVT_CHANGE:
			Map<String, AmfValue> hash = new LinkedHashMap<String, AmfValue>();
			while( true ) {
				String key = null;
				try {
					key = in.readUTF();
				}catch(EOFException e) {
					break;
				}
				hash.put(key, deserializer.read());
			}
			event = new SoEventChange(hash);
			break;
		case SoEvent.SO_EVT_SUCCESS:
			event = new SoEventSuceess(in.readUTF());
			break;
		case SoEvent.SO_EVT_SEND_MESSAGE:
			event = new SoEventSendMessage(deserializer.read());
			break;
		case SoEvent.SO_EVT_STATUS:
			String msg = in.readUTF();
			String type = in.readUTF();
			event = new SoEventStatus(msg, type);
			break;
		case SoEvent.SO_EVT_CLEAR:
			event = new SoEvent(SoEvent.SO_EVT_CLEAR);
			break;
		case SoEvent.SO_EVT_REMOVE:
			event = new SoEvent(SoEvent.SO_EVT_REMOVE);
			break;	
		case SoEvent.SO_EVT_REQUEST_REMOVE:
			event = new SoEventRequestRemove(in.readUTF());
			break;
		case SoEvent.SO_EVT_USE_SUCCESS:
			event = new SoEvent(SoEvent.SO_EVT_USE_SUCCESS);
		}
		return event;
	}

	public static void write(DataOutputStream out, SoEvent event) throws IOException {
		Amf0Serializer serializer = new Amf0Serializer(out); 
		switch(event.getKind()) {
		case SoEvent.SO_EVT_USE:
		case SoEvent.SO_EVT_RELEASE:
		case SoEvent.SO_EVT_CLEAR:
		case SoEvent.SO_EVT_REMOVE:
		case SoEvent.SO_EVT_USE_SUCCESS:
			// nothing
			break;
		case SoEvent.SO_EVT_REQUEST_CHANGE:
			out.writeUTF(((SoEventRequestChange)event).getName());
			serializer.write(((SoEventRequestChange)event).getValue());
			break;
		case SoEvent.SO_EVT_CHANGE:
			Map<String, AmfValue> data = ((SoEventChange)event).getData();
			Iterator<String> it = data.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				out.writeUTF(key);
				serializer.write(data.get(key));
			}
			break;
		case SoEvent.SO_EVT_SUCCESS:
			out.writeUTF(((SoEventSuceess)event).getName());
			break;
		case SoEvent.SO_EVT_SEND_MESSAGE:	
			serializer.write(((SoEventSendMessage)event).getMsg());
			break;
		case SoEvent.SO_EVT_STATUS:
			out.writeUTF(((SoEventStatus)event).getMsg());
			out.writeUTF(((SoEventStatus)event).getMsgType());
			break;
		case SoEvent.SO_EVT_REQUEST_REMOVE:
			out.writeUTF(((SoEventRequestRemove)event).getName());
			break;
		}
	}
	
}
