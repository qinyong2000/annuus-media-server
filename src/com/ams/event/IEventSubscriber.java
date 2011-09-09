package com.ams.event;

import com.ams.event.Event;

public interface IEventSubscriber {
	public void messageNotify(Event event);
}
