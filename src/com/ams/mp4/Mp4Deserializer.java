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
import com.ams.flv.ISampleDeserializer;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;
import com.ams.mp4.STSD.AudioSampleDescription;
import com.ams.mp4.STSD.VideoSampleDescription;
import com.ams.server.ByteBufferFactory;

public class Mp4Deserializer implements ISampleDeserializer {
	private RandomAccessFileReader reader;
	private TRAK videoTrak = null;
	private TRAK audioTrak = null;
	private Mp4Sample[] videoSamples;
	private Mp4Sample[] audioSamples;
	private ArrayList<Mp4Sample> samples = new ArrayList<Mp4Sample>();
	private int sampleIndex = 0;
	private long moovPosition = 0;
	
	private class SampleTimestampComparator implements java.util.Comparator<Mp4Sample> {
		public int compare(Mp4Sample s, Mp4Sample t) {
			return (int)(s.getTimestamp() - t.getTimestamp());
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
				BOX.Header header = BOX.readHeader(new ByteBufferInputStream(reader));
				long payloadSize = header.payloadSize;
				if ("moov".equalsIgnoreCase(header.type)) {
					moovPosition = reader.getPosition();
					byte[] b = new byte[(int) payloadSize];
					reader.read(b, 0, b.length);
					DataInputStream bin = new DataInputStream(new ByteArrayInputStream(b));
					moov = new MOOV();
					moov.read(bin);
					break;
				} else {
					reader.skip(payloadSize);
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

	public ByteBufferArray videoHeaderData() {
		if (videoTrak == null) return null;
		byte[] data = videoTrak.getVideoDecoderConfigData();
		int dataSize = (data != null) ? data.length : 0;
		ByteBuffer[] buf = new ByteBuffer[1];
		buf[0] = ByteBufferFactory.allocate(5 + dataSize);
		buf[0].put(new byte[]{0x17, 0x00, 0x00, 0x00, 0x00});
		if (data != null) {
			buf[0].put(data);
		}
		buf[0].flip();
		return new ByteBufferArray(buf);
	}
	
	public ByteBufferArray audioHeaderData() {
		if (audioTrak == null) return null;
		byte[] data = audioTrak.getAudioDecoderConfigData();
		int dataSize = (data != null) ? data.length : 0;
		ByteBuffer[] buf = new ByteBuffer[1];
		buf[0] = ByteBufferFactory.allocate(2 + dataSize);
		buf[0].put(new byte[]{(byte)0xaf, 0x00});
		if (data != null) {
			buf[0].put(data);
		}
		buf[0].flip();
		return new ByteBufferArray(buf);
	}
	
	private ByteBufferArray createVideoTag(Mp4Sample sample) throws IOException {
		ByteBuffer[] data = readSampleData(sample);
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(5);

		byte type = (byte) (sample.isKeyframe() ? 0x17 : 0x27);
		buf[0].put(new byte[]{type, 0x01, 0, 0, 0});
		buf[0].flip();
		return new ByteBufferArray(buf);
	}

	private ByteBufferArray createAudioTag(Mp4Sample sample) throws IOException {
		ByteBuffer[] data = readSampleData(sample);
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(2);
		buf[0].put(new byte[]{(byte)0xaf, 0x01});
		buf[0].flip();
		return new ByteBufferArray(buf);
	}
	
	public Sample seek(long seekTime) {
		Mp4Sample seekSample = videoSamples[0];
		int idx = Collections.binarySearch(samples, new Mp4Sample(Sample.SAMPLE_VIDEO, seekTime, true, 0, 0, 0) , new SampleTimestampComparator());
		int i = (idx >= 0) ? idx : -(idx + 1);
		while(i > 0) {
			seekSample = samples.get(i);
			if (seekSample.isVideoSample() && seekSample.isKeyframe()) {
				break;
			}
			i--;
		}
		sampleIndex = i; 
		return seekSample;
	}
	
	public Sample readNext() throws IOException {
		if (sampleIndex < samples.size()) {
			Mp4Sample sample = samples.get(sampleIndex ++);
			if (sample.isVideoSample()) {
				return new Sample(Sample.SAMPLE_VIDEO, sample.getTimestamp(), createVideoTag(sample));
			}
			if (sample.isAudioSample()) {
				return new Sample(Sample.SAMPLE_AUDIO, sample.getTimestamp(), createAudioTag(sample));
			}
		}
		return null;
	}

	public AmfValue metaData() {
		AmfValue track1 = null;
		if (videoTrak != null) {
			track1 = AmfValue.newEcmaArray();
			track1.put("length", videoTrak.getDuration())
				  .put("timescale", videoTrak.getTimeScale())				
				  .put("language", videoTrak.getLanguage())
				  .put("sampledescription", AmfValue.newArray(AmfValue.newEcmaArray().put("sampletype", videoTrak.getType())));
			
		}
		
		AmfValue track2 = null;
		if (audioTrak != null) {
			track2 = AmfValue.newEcmaArray();
			track2.put("length", audioTrak.getDuration())
				  .put("timescale", audioTrak.getTimeScale())				
				  .put("language", audioTrak.getLanguage())
				  .put("sampledescription", AmfValue.newArray(AmfValue.newEcmaArray().put("sampletype", audioTrak.getType())));
		}
		
		AmfValue value = AmfValue.newEcmaArray();
		if (videoTrak != null) {
			VideoSampleDescription videoSd = videoTrak.getVideoSampleDescription();
			value.put("duration", videoTrak.getDurationBySecond())
			 .put("moovPosition", moovPosition)
			 .put("width", videoSd.width)
			 .put("height", videoSd.height)
			 .put("canSeekToEnd", videoSamples[videoSamples.length - 1].isKeyframe())
			 .put("videocodecid", videoTrak.getType())
			 .put("avcprofile", videoSd.getAvcProfile())
			 .put("avclevel", videoSd.getAvcLevel())
			 .put("videoframerate", (float)videoSamples.length / videoTrak.getDurationBySecond());
		}
		if (audioTrak != null) {
			AudioSampleDescription audioSd = audioTrak.getAudioSampleDescription();
			value.put("audiocodecid", audioTrak.getType())
				 .put("aacaot", audioSd.getAudioCodecType())
				 .put("audiosamplerate", audioSd.sampleRate)
				 .put("audiochannels", audioSd.channelCount);
		}
		value.put("trackinfo", AmfValue.newArray(track1, track2));

		return value;
	}

	public void close() throws IOException {
		reader.close();
	}

}
