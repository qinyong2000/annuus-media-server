package com.ams.mp4;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.ams.amf.AmfValue;
import com.ams.flv.Sample;
import com.ams.flv.SampleDeserializer;
import com.ams.io.RandomAccessFileReader;
import com.ams.server.ByteBufferFactory;

public class Mp4Deserializer implements SampleDeserializer {
	private RandomAccessFileReader reader;
	private TRAK videoTrak = null;
	private TRAK audioTrak = null;
	private ArrayList<Mp4Sample> samples = new ArrayList<Mp4Sample>();
	private int sampleIndex = 0;
	
	private class SampleTimestampComparator implements java.util.Comparator {
		public int compare(Object s, Object t) {
			return (int)((Mp4Sample) s).getTimestamp() - (int)((Mp4Sample) t).getTimestamp();
		}
	};
	
	public Mp4Deserializer(RandomAccessFileReader reader) {
		this.reader = reader;
		MOOV moov = readMoov(reader);
		TRAK trak = moov.getVideoTrak();
		if (trak != null) {
			videoTrak = trak;
			Mp4Sample[] videoSamples = trak.getAllSamples(Sample.SAMPLE_VIDEO);
			samples.addAll(Arrays.asList(videoSamples));
		}
		trak = moov.getAudioTrak();
		if (trak != null) {
			audioTrak = trak;
			Mp4Sample[] audioSamples = trak.getAllSamples(Sample.SAMPLE_AUDIO);
			samples.addAll(Arrays.asList(audioSamples));
		}
		
		Collections.sort(samples, new SampleTimestampComparator());
	}
	
	private MOOV readMoov(RandomAccessFileReader reader) {
		MOOV moov = null;
		try {
			reader.seek(0);
			for(;;) {
				// find moov box
				byte[] b = new byte[4];
				reader.read(b, 0, 4);
				int size = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
							| ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
				b = new byte[4];
				reader.read(b, 0, 4);
				String box = new String(b);
				if ("moov".equalsIgnoreCase(box)) {
					b = new byte[size - 8];
					reader.read(b, 0, size - 8);
					DataInputStream bin = new DataInputStream(new ByteArrayInputStream(b));
					moov = new MOOV();
					moov.read(bin);
					break;
				} else {
					reader.skip(size - 8);
				}
			}
		} catch(IOException e) {
			moov = null;
		}	
		return moov;
	}
	
	private ByteBuffer[] readSampleData(Mp4Sample sample) throws IOException {
		reader.seek(sample.getOffset());
		return reader.read(sample.getSize());
	}

	public ByteBuffer[] videoHeaderData() {
		ByteBuffer[] data;
		try {
			data = videoTrak.getExtraData();
		} catch (IOException e) {
			return null;
		}
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(5);
		buf[0].put(new byte[]{0x17, 0x00, 0x00, 0x00, 0x00});
		buf[0].flip();
		return buf;
	}
	
	public ByteBuffer[] audioHeaderData() {
		ByteBuffer[] buf = new ByteBuffer[1]; 
		buf[0] = ByteBufferFactory.allocate(2);
		buf[0].put(new byte[]{(byte)0xaf, 0x00});
		buf[0].flip();
		return buf;
	}
	
	private ByteBuffer[] createVideoTag(Mp4Sample sample) throws IOException {
		ByteBuffer[] data = readSampleData(sample);
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(5);

		long time = 1000 * sample.getTimestamp() / getVideoTimeScale();
		byte type = (byte) (sample.isKeyframe() ? 0x17 : 0x27);
		//buf[0].put(new byte[]{type, 0x01, (byte) (time & 0xFF), (byte) ((time & 0xFF00) >>> 8), (byte) ((time & 0xFF0000) >>> 16)});
		buf[0].put(new byte[]{type, 0x01, 0, 0, 0});

		buf[0].flip();
		return buf;
	}

	private ByteBuffer[] createAudioTag(Mp4Sample sample) throws IOException {
		ByteBuffer[] data = readSampleData(sample);
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(2);
		buf[0].put(new byte[]{(byte)0xaf, 0x01});
		buf[0].flip();
		return buf;
	}
	
	public Sample seek(long seekTime) {
		Mp4Sample seekSample = null;
		int i = 0;
		for(Mp4Sample sample : samples) {
			if (sample.isVideoTag() && sample.isKeyframe()) {
				if( sample.getTimestamp() >= seekTime ) {
					break;
				}
				seekSample = sample;
			}
			i++;
		}
		sampleIndex = i; 
		return seekSample;
	}
	
	public Sample readNext() throws IOException {
		if (sampleIndex < samples.size()) {
			Mp4Sample sample = samples.get(sampleIndex ++);
			if (sample.isVideoTag()) {
				return new Sample(Sample.SAMPLE_VIDEO, createVideoTag(sample), sample.getTimestamp());
			}
			if (sample.isAudioTag()) {
				return new Sample(Sample.SAMPLE_AUDIO, createAudioTag(sample), sample.getTimestamp());
			}
		}
		return null;
	}

	public int getVideoTimeScale() {
		return videoTrak.getTimeScale();
	}

	public int getAudioTimeScale() {
		return audioTrak.getTimeScale();
	}

	public AmfValue metaData() {
		AmfValue track1 = AmfValue.newObject();
		track1.put("length", 233935)
			  .put("timescale", 1000)				
			  .put("language", "eng")
			  .put("sampledescription", AmfValue.newArray(AmfValue.newObject().put("sampletype", "avc1")));

		AmfValue track2 = AmfValue.newObject();
		track2.put("length", 10314725)
			  .put("timescale", 44100)				
			  .put("language", "eng")
			  .put("sampledescription", AmfValue.newArray(AmfValue.newObject().put("sampletype", "mp4a")));
		
		AmfValue value = AmfValue.newObject();
		value.setEcmaArray(true);
		value.put("duration", 233.935)
			 .put("moovPosition", 32)
			.put("width", 640)
			.put("height", 360)
			.put("framewidth", 640)
			.put("frameheight", 360)
			.put("displaywidth", 640)
			.put("displayheight", 360)
			.put("videocodecid", "avc1")
//			.put("audiocodecid", "mp4a")
			.put("avcprofile", 66)
			.put("avclevel", 30)
			.put("aacaot", 2)
			.put("videoframerate", 30.3030303030303)
//			.put("audiosamplerate", 44100)
//			.put("audiochannels", 2)
			.put("trackinfo", AmfValue.newArray(track1));
		return value;
	}

	public void close() throws IOException {
		reader.close();
	}

}
