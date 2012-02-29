package com.ams.message;

import com.ams.message.MediaMessage;

public interface IMsgSubscriber<T> {
	public void messageNotify(MediaMessage<T> msg);
}
