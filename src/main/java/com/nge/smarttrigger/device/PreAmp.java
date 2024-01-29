package com.nge.smarttrigger.device;

public interface PreAmp<I extends PreAmpInput, V extends PreAmpVolume> extends Device {

	public I getSelectedInput();
	
	public void setSelectedInput(I input);
	
	public V getVolume();
	
	public void setVolume(V volume);
	
	public boolean isMuted();
	
	public void toggleMute();
	
	public void setMute(boolean mute);
}