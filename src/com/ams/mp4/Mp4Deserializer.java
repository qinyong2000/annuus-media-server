package com.ams.mp4;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;

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
					ByteBufferInputStream in = new ByteBufferInputStream(reader);
					moov = new MOOV();
					moov.read(new DataInputStream(in));
					break;
				} else {
					reader.skip(size);
				}
			}
		} catch(IOException e) {
			moov = null;
		}	
		return moov;
	}
	
	public ByteBuffer[] readSampleData(Mp4Sample sample) throws IOException {
		reader.seek(sample.getOffset());
		return reader.read(sample.getSize());
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

	public TRAK getVideoTrak() {
		return videoTrak;
	}

	public TRAK getAudioTrak() {
		return audioTrak;
	}
}
