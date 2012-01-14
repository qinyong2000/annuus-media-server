package com.ams.mp4;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class STSD extends BOX {
	private SampleDescription[] descriptions;
	
	private VideoSampleDescription videoSampleDescription = null;
	private AudioSampleDescription audioSampleDescription = null;
	
	public static class SampleDescription {
		public String type;
		public byte[] description;
	}
	
	public static class VideoSampleDescription {
		public short index;
		public short preDefined1;
		public short reserved1;
		public int preDefined2;
		public int preDefined3;
		public int preDefined4;
		public short width;
		public short height;
		public int horizontalResolution;
		public int verticalResolution;
		public int reserved2;
		public short frameCount;
		public String compressorName;
		public short depth;
		public short preDefined5;
		public String configType;
		public byte[] decoderConfig;
		
		public byte getAvcProfile() {
			return decoderConfig[1];
		}

		public byte getAvcLevel() {
			return decoderConfig[3];
		}

		public void read(DataInputStream in) throws IOException {
			in.skipBytes(6); // reserved
			index = in.readShort();
			preDefined1 = in.readShort();
			reserved1 = in.readShort();
			preDefined2 = in.readInt();
			preDefined3 = in.readInt();
			preDefined4 = in.readInt();
			width = in.readShort();
			height = in.readShort();
			horizontalResolution = in.readInt();
			verticalResolution = in.readInt();
			reserved2 = in.readInt();
			frameCount = in.readShort();
			int nameSize = in.readByte();
			byte[] nameBytes = new byte[nameSize];
			in.read(nameBytes);
			compressorName = new String(nameBytes);
			in.skipBytes(31 - nameSize);
			depth = in.readShort();
			preDefined5 = in.readShort();
			int configSize = in.readInt();
			byte[] b = new byte[4];
			in.read(b);
			configType = new String(b);	// avcC
			decoderConfig = new byte[configSize - 8];
			in.read(decoderConfig);		
		}	
	}

	public static class AudioSampleDescription {
		public final static int ES_TAG = 3;
		public final static int DECODER_CONFIG_TAG = 4;
		public final static int DECODER_SPECIFIC_CONFIG_TAG = 5;		
		public short index;
		public short innerVersion;
		public short revisionLevel;
		public int vendor;
		public short channelCount;
		public short sampleSize;
		public short compressionId;
		public short packetSize;
		public int sampleRate;
		public int samplesPerPacket;
		public int bytesPerPacket;
		public int bytesPerFrame;
		public int samplesPerFrame;
		public byte[] decoderSpecificConfig;
		
		public int getAudioCodecType() {
			int audioCodecType;
		    switch(decoderSpecificConfig[0]) {
		    	case 0x12:
		    	default:
		    		//AAC LC - 12 10
		    		audioCodecType = 1;
		    		break;
		    	case 0x0a:
		    		//AAC Main - 0A 10
		    		audioCodecType = 0;
		    		break;
		    	case 0x11:
		    	case 0x13:
		    		//AAC LC SBR - 11 90 & 13 xx
		    		audioCodecType = 2;
		    		break;
		    }
		    return audioCodecType;
		}
		
		private int readDescriptor(DataInputStream in) throws IOException {
			int tag = in.readByte();
			int size = 0;
			int c = 0;
			do {
				c = in.readByte();
				size = (size << 7) | (c & 0x7f);
			} while ((c & 0x80) == 0x80);
			
			if (tag == DECODER_SPECIFIC_CONFIG_TAG) {
				decoderSpecificConfig = new byte[size];
				in.read(decoderSpecificConfig);
			}
			return tag;
		}
		
		public void read(DataInputStream in) throws IOException {
			in.skipBytes(6); // reserved
			index = in.readShort();
			innerVersion = in.readShort();
			revisionLevel = in.readShort();
			vendor = in.readInt();
			channelCount = in.readShort();
			sampleSize = in.readShort();
			compressionId = in.readShort();
			packetSize = in.readShort();
			byte[] b = new byte[4];
			in.read(b);
			sampleRate = ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
			if (innerVersion != 0) {
				samplesPerPacket = in.readInt();
				bytesPerPacket = in.readInt();
				bytesPerFrame = in.readInt();
				samplesPerFrame = in.readInt();
			}
			
			//read MP4Descriptor(in);
			int size = in.readInt();
			in.read(new byte[4]); 	// "esds"
			in.readInt(); 	// version and flags
			int tag = readDescriptor(in);
			if (tag == ES_TAG) {
				in.skipBytes(3);
			} else {
				in.skipBytes(2);
			}
			tag = readDescriptor(in);
			if (tag == DECODER_CONFIG_TAG) {
				in.skipBytes(13);
				// DECODER_SPECIFIC_CONFIG_TAG
				tag = readDescriptor(in);
			}
		}
	}
	
	public STSD(int version) {
		super(version);
	}
	
	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		descriptions = new SampleDescription[count];
		for (int i = 0 ; i < count; i++) {
			int length = in.readInt();
			byte[] b = new byte[4];
			in.read(b);
			String type = new String(b);
			byte[] description = new byte[length];
			in.read(description);
			descriptions[i] = new SampleDescription();
			descriptions[i].type = type;
			descriptions[i].description = description; 
		}
		
		SampleDescription desc = getDescription();
		if (isVideoSampleDescription(desc)) {
			videoSampleDescription = getVideoSampleDescription(desc);
		} else if (isAudioSampleDescription(desc)) {
			audioSampleDescription = getAudioSampleDescription(desc);
		}
		
	}
	
	public boolean isVideoSampleDescription(SampleDescription desc) {
		if ("avc1".equalsIgnoreCase(desc.type)) {
			return true;
		}
		return false;
	}

	public boolean isAudioSampleDescription(SampleDescription desc) {
		if ("mp4a".equalsIgnoreCase(desc.type)) {
			return true;
		}
		return false;
	}

	public SampleDescription getDescription() {
		return descriptions[0];
	}
	
	public static VideoSampleDescription getVideoSampleDescription(SampleDescription desc) throws IOException {
		VideoSampleDescription sd = new VideoSampleDescription();
		DataInputStream bin = new DataInputStream(new ByteArrayInputStream(desc.description));
		sd.read(bin);
		return sd;
	}

	public static AudioSampleDescription getAudioSampleDescription(SampleDescription desc) throws IOException {
		AudioSampleDescription sd = new AudioSampleDescription();
		DataInputStream bin = new DataInputStream(new ByteArrayInputStream(desc.description));
		sd.read(bin);
		return sd;
	}

	public VideoSampleDescription getVideoSampleDescription() {
		return videoSampleDescription;
	}

	public AudioSampleDescription getAudioSampleDescription() {
		return audioSampleDescription;
	}

}
