package main.java.dfhackremote;

import java.io.IOException;

public class DFHackRPCException extends IOException {

	public DFHackRPCException(String msg) {
		super(msg);
	}

	public DFHackRPCException(String msg, Exception e) {
		super(msg, e);
	}

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

}
