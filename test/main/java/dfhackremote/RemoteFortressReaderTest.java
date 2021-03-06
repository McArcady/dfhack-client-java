// (c) 2021- McArcady@gmail.com
// This code is licensed under MIT license (see LICENSE.txt for details)

/* This example connects to the local DFHack RPC server, 
 * and retrieves version info from the RemoteFortressReader plugin.
 */ 
package main.java.dfhackremote;

import java.io.IOException;

import dfproto.CoreProtocol.EmptyMessage;
import main.java.dfhackclient.DFHackRPCClient;

public class RemoteFortressReaderTest { 
	
	public static void main(String[] argv) throws IOException {
		DFHackRPCClient cli = new DFHackRPCClient("RemoteFortressReader");
		cli.connect();
		var lrb = EmptyMessage.newBuilder();
		var buf = cli.call("GetVersionInfo", lrb.build(), "VersionInfo");
		System.out.println(remotefortressreader.RemoteFortressReader.VersionInfo.parser().parseFrom(buf).toString());
		cli.disconnect();
	}
}