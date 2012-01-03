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
import com.ams.mp4.STSD.AudioSampleDescription;
import com.ams.mp4.STSD.VideoSampleDescription;
import com.ams.server.ByteBufferFactory;

public class Mp4Deserializer implements SampleDeserializer {
	private RandomAccessFileReader reader;
	private TRAK videoTrak = null;
	private TRAK audioTrak = null;
	private Mp4Sample[] videoSamples;
	private Mp4Sample[] audioSamples;
	private ArrayList<Mp4Sample> samples = new ArrayList<Mp4Sample>();
	private int sampleIndex = 0;
	private long moovPosition = 0;
	
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
			videoSamples = trak.getAllSamples(Sample.SAMPLE_VIDEO);
			samples.addAll(Arrays.asList(videoSamples));
		}
		trak = moov.getAudioTrak();
		if (trak != null) {
			audioTrak = trak;
			audioSamples = trak.getAllSamples(Sample.SAMPLE_AUDIO);
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
					moovPosition = reader.getPosition() - 4;
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
		ByteBuffer[] data = videoTrak.getVideoDecoderConfigData();
		if (data == null) return null;
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(5);
		buf[0].put(new byte[]{0x17, 0x00, 0x00, 0x00, 0x00});
		buf[0].flip();
		return buf;
	}
	
	public ByteBuffer[] audioHeaderData() {
		ByteBuffer[] data = audioTrak.getVideoDecoderConfigData();
		if (data == null) return null;
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
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

		long time = 1000 * sample.getTimestamp() / videoTrak.getTimeScale();
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
		Mp4Sample seekSample = videoSamples[0];
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

	public AmfValue metaData() {
		AmfValue track1 = AmfValue.newObject();
		track1.put("length", videoTrak.getDuration())
			  .put("timescale", videoTrak.getTimeScale())				
			  .put("language", videoTrak.getLanguage())
			  .put("sampledescription", AmfValue.newArray(AmfValue.newObject().put("sampletype", videoTrak.getType())));

		AmfValue track2 = AmfValue.newObject();
		track2.put("length", audioTrak.getDuration())
			  .put("timescale", audioTrak.getTimeScale())				
			  .put("language", audioTrak.getLanguage())
			  .put("sampledescription", AmfValue.newArray(AmfValue.newObject().put("sampletype", audioTrak.getType())));
		
		AmfValue value = AmfValue.newObject();
		value.setEcmaArray(true);
		VideoSampleDescription videoSd = videoTrak.getVideoSampleDescription();
		AudioSampleDescription audioSd = audioTrak.getAudioSampleDescription();
		value.put("duration", (float)videoTrak.getDuration() / 1000)
			 .put("moovPosition", moovPosition)
			 .put("width", videoSd.width)
			 .put("height", videoSd.height)
			 .put("framewidth", videoSd.width)
			 .put("frameheight", videoSd.height)
			 .put("displaywidth", videoSd.width)
			 .put("displayheight", videoSd.height)
			 .put("videocodecid", videoTrak.getType())
			 .put("audiocodecid", audioTrak.getType())
//			 .put("avcprofile", 66)
//			 .put("avclevel", 30)
//			 .put("aacaot", 2)
			 .put("videoframerate", (float)videoSamples.length / videoTrak.getDuration() * 1000)
			 .put("audiosamplerate", audioSd.sampleRate)
			 .put("audiochannels", audioSd.channelCount)
			 .put("trackinfo", AmfValue.newArray(track1, track2));
		return value;
	}

	public void close() throws IOException {
		reader.close();
	}

}
