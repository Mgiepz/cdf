package org.pentaho.cdf;

public class InvalidTemplateException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidTemplateException(Exception ex) {
		super(ex);
	}

	public InvalidTemplateException(String string) {
		super(string);
	}

}
