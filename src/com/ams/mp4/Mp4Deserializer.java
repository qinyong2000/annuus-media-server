package com.ams.mp4;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.ams.io.RandomAccessFileReader;
import com.ams.server.ByteBufferFactory;

public class Mp4Deserializer {
	private RandomAccessFileReader reader;
	private TRAK videoTrak = null;
	private TRAK audioTrak = null;
	private Mp4Sample[] videoSamples = null;
	private Mp4Sample[] audioSamples = null;
	private int videoSampleIndex = 0;
	private int audioSampleIndex = 0;	
	
	public Mp4Deserializer(RandomAccessFileReader reader) {
		this.reader = reader;
		MOOV moov = readMoov(reader);
		TRAK trak = moov.getVideoTrak();
		if (trak != null) {
			videoTrak = trak;
			videoSamples = trak.getAllSamples();
		}
		trak = moov.getAudioTrak();
		if (trak != null) {
			audioTrak = trak;
			audioSamples = trak.getAllSamples();
		}
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

	public ByteBuffer[] createVideoHeaderTag() throws IOException {
		ByteBuffer[] data = videoTrak.getExtraData();
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(5);
		buf[0].put(new byte[]{0x17, 0x00, 0x00, 0x00, 0x00});
		buf[0].flip();
		return buf;
	}

	public ByteBuffer[] createVideoHeaderTag1() throws IOException {
		ByteBuffer[] buf = new ByteBuffer[1];
		buf[0] = ByteBufferFactory.allocate(5);
		buf[0].put(new byte[]{0x52, 0x00});
		buf[0].flip();
		return buf;
	}
	
	public ByteBuffer[] createVideoHeaderTag2() throws IOException {
		ByteBuffer[] buf = new ByteBuffer[1];
		buf[0] = ByteBufferFactory.allocate(5);
		buf[0].put(new byte[]{0x17, 0x02, 0x00, 0x00, 0x00});
		buf[0].flip();
		return buf;
	}

	public ByteBuffer[] createVideoHeaderTag3() throws IOException {
		ByteBuffer[] buf = new ByteBuffer[1];
		buf[0] = ByteBufferFactory.allocate(5);
		buf[0].put(new byte[]{0x52, 0x01});
		buf[0].flip();
		return buf;
	}
	
	public ByteBuffer[] createAudioHeaderTag() {
		//TODO
		ByteBuffer[] buf = new ByteBuffer[1]; 
		buf[0] = ByteBufferFactory.allocate(2);
		buf[0].put(new byte[]{(byte)0xaf, 0x00});
		buf[0].flip();
		return buf;
	}
	
	public ByteBuffer[] createVideoTag(Mp4Sample sample) throws IOException {
		ByteBuffer[] data = readSampleData(sample);
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(5);

		long time = 1000 * sample.getTimeStamp() / getVideoTimeScale();
		byte type = (byte) (sample.isKeyframe() ? 0x17 : 0x27);
		//buf[0].put(new byte[]{type, 0x01, (byte) (time & 0xFF), (byte) ((time & 0xFF00) >>> 8), (byte) ((time & 0xFF0000) >>> 16)});
		buf[0].put(new byte[]{type, 0x01, 0, 0, 0});

		buf[0].flip();
		return buf;
	}

	public ByteBuffer[] createAudioTag(Mp4Sample sample) throws IOException {
		ByteBuffer[] data = readSampleData(sample);
		ByteBuffer[] buf = new ByteBuffer[data.length + 1];
		System.arraycopy(data, 0, buf, 1, data.length);
		buf[0] = ByteBufferFactory.allocate(2);
		buf[0].put(new byte[]{(byte)0xaf, 0x01});
		buf[0].flip();
		return buf;
	}
	
	public Mp4Sample[] seek(long seekTime) {
		if (videoTrak != null) {
			videoSampleIndex = videoTrak.getSampleIndex(seekTime);
		}
		if (audioTrak != null) {
			audioSampleIndex = audioTrak.getSampleIndex(seekTime);
		}
		return readNext();
	}
	
	public Mp4Sample[] readNext() {
		Mp4Sample[] samples = {null, null};
		if (videoSamples != null && videoSampleIndex < videoSamples.length) {
			samples[0] = videoSamples[videoSampleIndex++];
		}
		if (audioSamples != null && audioSampleIndex < audioSamples.length) {
			samples[1] = audioSamples[audioSampleIndex++];
		}
		return samples;
	}

	public int getVideoTimeScale() {
		return videoTrak.getTimeScale();
	}

	public int getAudioTimeScale() {
		return audioTrak.getTimeScale();
	}

}
