package com.ams.message;

import com.ams.message.MediaMessage;

public interface IMsgSubscriber {
	public void messageNotify(MediaMessage event);
}
