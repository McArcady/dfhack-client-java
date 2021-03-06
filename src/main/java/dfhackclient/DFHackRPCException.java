// (c) 2021- McArcady@gmail.com
// This code is licensed under MIT license (see LICENSE.txt for details)

package main.java.dfhackclient;

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
