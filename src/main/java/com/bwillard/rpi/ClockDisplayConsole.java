package com.bwillard.rpi;

class ClockDisplayConsole implements ClockDisplay {
	private boolean currentState = false;
	
	@Override
	public void setState(boolean okToGetUp) {
		System.out.println("Clock display, ok to get up: " + okToGetUp);
		currentState = okToGetUp;
	}
	
	@Override
	public void shutdown() {
		System.out.println("Cutting down clock diplay");
	}

	@Override
	public boolean getState() {
		return currentState;
	}
}
