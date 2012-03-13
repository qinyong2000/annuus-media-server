package com.ams.flv;

import java.io.IOException;
import com.ams.io.ByteBufferArray;
import com.ams.io.ByteBufferInputStream;
import com.ams.message.MediaSample;

public class VideoTag extends MediaSample {
	private int codecId = -1;
	private int width = 0, height = 0;
	
	public VideoTag(long timestamp, ByteBufferArray data) {
		super(MediaSample.SAMPLE_VIDEO, timestamp, data);
	}

	public VideoTag(long timestamp, boolean keyframe, long offset, int size) {
		super(MediaSample.SAMPLE_VIDEO, timestamp, keyframe, offset, size);
	}
	
	public void getParameters() throws IOException {
		ByteBufferInputStream bi = new ByteBufferInputStream(data.duplicate());
		byte b = bi.readByte();
		keyframe = (b >>> 4) == 1;
		codecId = b & 0x0F;
	    byte[] videoData = new byte[9];
	    bi.read(videoData);
	    if (codecId == 2) {		//H263VIDEOPACKET
		    int pictureSize = (videoData[3] & 0x03) << 2 + (videoData[4] & 0x80) >>> 7; 
		    switch(pictureSize) {
		    case 0:
		    	width = (videoData[4] & 0x7F) << 1 + (videoData[5] & 0x80) >>> 7; 
		    	height = (videoData[5] & 0x7F) << 1 + (videoData[6] & 0x80) >>> 7;
		    	break;
		    case 1:
		    	width = (videoData[4] & 0x7F) << 9 + (videoData[5] & 0x80) << 1 + (videoData[6] & 0x80) >>> 7; 
		    	height = (videoData[6] & 0x7F) << 9 + (videoData[7] & 0x80) << 1 + (videoData[8] & 0x80) >>> 7;
		    	break;
		    case 2:
		    	width = 352; height = 288;
		    	break;
		    case 3:
		    	width = 176; height = 144;
		    	break;
		    case 4:
		    	width = 128; height = 96;
		    	break;
		    case 5:
		    	width = 320; height = 240;
		    	break;
		    case 6:
		    	width = 160; height = 120;
		    	break;
		    	
		    }
	    } else if (codecId == 3) {
	    	width = (videoData[0] & 0x0F) << 4 + videoData[1]; 
	    	height = videoData[2]<< 4 + (videoData[3] & 0xF0) >>> 4;
	    }
	}
	
	public int getCodecId() {
		return codecId;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	
}
