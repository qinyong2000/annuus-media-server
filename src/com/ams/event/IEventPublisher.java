package com.ams.event;

import java.io.IOException;

public interface IEventPublisher {
	public void publish(Event event) throws IOException;
	public void addSubscriber(IEventSubscriber subscriber);
	public void removeSubscriber(IEventSubscriber subscriber);
}
