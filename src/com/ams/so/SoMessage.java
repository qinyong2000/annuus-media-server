package com.ams.so;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import com.ams.amf.AmfException;

public class SoMessage {
		private String name;
		private int version;
		private boolean persist;
		private int unknown;
		private ArrayList<SoEvent> events;
		
		public SoMessage(String name, int version, boolean persist, int unknown, ArrayList<SoEvent> events) {
			this.name = name;
			this.version = version;
			this.persist = persist;
			this.unknown = unknown;
			this.events = events;
		}

		public ArrayList<SoEvent> getEvents() {
			return events;
		}

		public String getName() {
			return name;
		}

		public boolean isPersist() {
			return persist;
		}

		public int getVersion() {
			return version;
		}

		public int getUnknown() {
			return unknown;
		}

		public static SoMessage read( DataInputStream in ) throws IOException, AmfException {
			String name = in.readUTF();
			int version = in.readInt();
			boolean persist = (in.readInt() == 2);
			int unknown = in.readInt();
			ArrayList<SoEvent> events = new ArrayList<SoEvent>();
			
			while( true ) {
				int kind;
				try {
					kind = in.readByte() & 0xFF;
				} catch(EOFException e) {
					 break;
				}
				int size = in.readInt();
				SoEvent event = SoEvent.read(in, kind, size);
				if (event != null) {
					events.add(event);
				}
			}
			
			return new SoMessage(name, version, persist, unknown, events);
		}

		public static void write(DataOutputStream out, SoMessage so) throws IOException {
			out.writeUTF(so.getName());
			out.writeInt(so.getVersion());
			out.writeInt(so.isPersist()?2:0);
			out.writeInt(so.getUnknown());
			ArrayList<SoEvent> events = so.getEvents();
			
			for(int i=0; i < events.size(); i++) {
				SoEvent event = events.get(i);
				out.writeByte(event.getKind());
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				SoEvent.write(new DataOutputStream(bos), event);
				byte[] data = bos.toByteArray();
				out.writeInt(data.length);
				out.write(data);
			}
		}
		
}
