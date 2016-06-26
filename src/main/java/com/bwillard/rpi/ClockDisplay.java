package com.bwillard.rpi;


public interface ClockDisplay {
	void setState(boolean okToGetUp);
	boolean getState();
	
	void shutdown();
}
