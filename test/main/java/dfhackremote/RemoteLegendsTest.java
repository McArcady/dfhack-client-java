package main.java.dfhackremote;

import java.io.IOException;

import remotelegends.RemoteLegends;

public class RemoteLegendsTest { 
	
	public static void main(String[] argv) throws IOException {
		DFHackRPCClient cli = new DFHackRPCClient("RemoteLegends");
		cli.connect();
		var lrb = RemoteLegends.ListRequest.newBuilder();
		var buf = cli.call("GetWorldLandmassList", lrb.build(), "WorldLandmassList");
		System.out.println(RemoteLegends.WorldLandmassList.parseFrom(buf).getList(0).toString());
	}
}