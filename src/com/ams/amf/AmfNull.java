package com.ams.amf;

public class AmfNull extends AmfValue {
	public AmfNull() {
		this.kind = AMF_NULL;
		this.value = null;
	}
}
