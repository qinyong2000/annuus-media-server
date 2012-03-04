package com.ams.message;

public interface IMsgSubscriber<T> {
	public void messageNotify(T msg);
}
