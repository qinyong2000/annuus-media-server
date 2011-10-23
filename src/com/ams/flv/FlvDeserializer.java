package com.ams.flv;

import java.io.IOException;
import com.ams.io.ByteBufferInputStream;
import com.ams.io.RandomAccessFileReader;

public class FlvDeserializer {
	private RandomAccessFileReader reader;
	private ByteBufferInputStream in;
	
	public FlvDeserializer(RandomAccessFileReader reader) {
		this.reader = reader;
	}

	public FlvTag seek(long seekTime) throws IOException, FlvException {
		reader.seek(0);
		in = new ByteBufferInputStream(reader);
		FlvHeader flvHeader = FlvHeader.read(in);
		
		boolean hasAudio = flvHeader.isHasAudio();
		boolean hasVideo = flvHeader.isHasVideo();
		FlvTag flvTag = null;
		while (hasAudio || hasVideo) {
			flvTag = FlvTag.read(in);
			if( flvTag == null ) {
				return null;
			}
			long time = flvTag.getTimestamp();
			if( time < seekTime ) {
				continue;
			}

			switch( flvTag.getTagType() ) {
			case FlvTag.FLV_AUDIO:
				hasAudio = false;
				break;
			case FlvTag.FLV_VIDEO:
				hasVideo = false;
				break;
			case FlvTag.FLV_META:
				break;
			}
		}
		return flvTag;
	}
	
	public FlvTag readNext() throws IOException, FlvException {
		return FlvTag.read(in);
	}

}
