package com.ams.message;

import java.io.IOException;

public interface IMsgPublisher<T> {
	public void publish(MediaMessage<T> msg) throws IOException;
	public void addSubscriber(IMsgSubscriber<T> subscriber);
	public void removeSubscriber(IMsgSubscriber<T> subscriber);
}
