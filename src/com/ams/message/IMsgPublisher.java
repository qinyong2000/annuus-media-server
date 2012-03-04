package com.ams.message;

import java.io.IOException;

public interface IMsgPublisher<T1, T2> {
	public void publish(T1 msg) throws IOException;
	public void addSubscriber(IMsgSubscriber<T2> subscriber);
	public void removeSubscriber(IMsgSubscriber<T2> subscriber);
}
