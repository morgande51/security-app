package com.nge.smarttrigger.manager;

import java.io.Serializable;
import java.util.Map.Entry;

public class SimpleKeyValue implements Serializable, Cloneable {

	private String key;
	
	private String value;
	
	public SimpleKeyValue() {}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		SimpleKeyValue kv = new SimpleKeyValue();
		kv.key = key;
		kv.value = value;
		return kv;
	}
	
	public static final SimpleKeyValue buildFrom(Entry<Object, Object> entry) {
		SimpleKeyValue skv = new SimpleKeyValue();
		skv.key = entry.getKey().toString();
		skv.value = entry.getValue().toString();
		return skv;
	}

	private static final long serialVersionUID = 1L;
}