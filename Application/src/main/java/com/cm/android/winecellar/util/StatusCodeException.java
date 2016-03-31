package com.cm.android.winecellar.util;

public class StatusCodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int statusCode;
	public String encodedResponse;

	public StatusCodeException(int statusCode, java.lang.String encodedResponse) {
		this.statusCode = statusCode;
		this.encodedResponse = encodedResponse;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getEncodedResponse() {
		return encodedResponse;
	}

}
