/**
 * 
 */
package com.ams.http;

public class Cookie {
	public String value="";
	public long expires=0;
	public String domain="";
	public String path="";
	public boolean secure=false;

	public Cookie(String value, long expires, String domain, String path, boolean secure) {
		this.value = value;
		this.expires = expires;
		this.domain = domain;
		this.path = path;
		this.secure = secure;
	}
}