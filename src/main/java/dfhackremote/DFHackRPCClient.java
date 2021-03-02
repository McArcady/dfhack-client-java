package main.java.dfhackremote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.cache.annotation.Cacheable;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import dfproto.CoreProtocol.CoreBindReply;
import dfproto.CoreProtocol.CoreBindRequest;
import dfproto.CoreProtocol.CoreBindRequest.Builder;

/**
 * DFHack RPC client.
 */
public class DFHackRPCClient {

	public static final int DFHACK_RPC_PORT_DEFAULT = 5000;
	private AsynchronousSocketChannel socket = null;
	private String plugin = null;
	
	enum ReplyCode {
	    RPC_REPLY_RESULT (-1),
	    RPC_REPLY_FAIL   (-2),
	    RPC_REPLY_TEXT   (-3),
	    RPC_REQUEST_QUIT (-4);
	    private final int id;
		ReplyCode(int id) { this.id = id; }
	}

	public DFHackRPCClient(String plugin) throws IOException {
		this.plugin = plugin;
		socket = AsynchronousSocketChannel.open();
	}
	
	private ByteBuffer writeAndRead(ByteBuffer request, int exp_sz) throws DFHackRPCException {
		assert socket != null;
		try {
			Future<Integer> wres = socket.write(request);
			wres.get();
			var result = ByteBuffer.allocate(exp_sz).order(ByteOrder.LITTLE_ENDIAN);
			Future<Integer> rres = socket.read(result);
			if (rres.get() == -1) {
				throw new DFHackRPCException("end-of-stream error while reading result from RPC server");
			}
			return result.rewind();
		} catch (InterruptedException | ExecutionException e) {
			throw new DFHackRPCException("failed to exchange with RPC server", e);
		}		
	}	
	
	private ByteBuffer createHandshake() {
		var handshake = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
		handshake.put("DFHack?\n".getBytes(StandardCharsets.US_ASCII));
		handshake.putInt(1);
		return handshake.rewind();
	}

	/**
	 * Open connection to DFHack RPC server.
	 * @param tcpPort DFHack server port
	 * @throws DFHackRPCException
	 */
	public void connect(int tcpPort) throws DFHackRPCException {
		assert 0 < tcpPort && tcpPort < 0x10000;
		assert socket != null;
		try {
			Future<Void> cres = socket.connect(new InetSocketAddress("127.0.0.1", tcpPort));
			cres.get();
			ByteBuffer response = writeAndRead(createHandshake(), 12);
			byte[] magic = new byte[8];
			response.rewind().get(magic);
			if (! new String(magic).contentEquals("DFHack!\n")) {
				throw new DFHackRPCException("invalid handshake reply magic value: " + new String(magic));
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new DFHackRPCException("failed to connect to RPC server", e);
		}
	}

	/**
	 * Open connection to DFHack RPC server using default port.
	 * @throws DFHackRPCException
	 */
	public void connect() throws DFHackRPCException {
		connect(DFHACK_RPC_PORT_DEFAULT);
	}
	
	/**
	 * Disconnect from server.
	 * @throws DFHackRPCException
	 */
	public void disconnect() throws DFHackRPCException {
		assert socket != null;
		var header = createHeader(ReplyCode.RPC_REQUEST_QUIT.id, 0).rewind();
		Future<Integer> wres = socket.write(header);
		try {
			wres.get();
			socket.close();
			socket = null;
		} catch (InterruptedException | ExecutionException | IOException e) {
			throw new DFHackRPCException("error while closing RPC session", e);
		}
	}
	
	private ByteBuffer createHeader(int id, int size) {
		assert id < 0x10000;
		var header = ByteBuffer.allocate(8+size).order(ByteOrder.LITTLE_ENDIAN);
		header.putShort((short) id);
		header.putShort((short) 0);
		header.putInt(size);
		return header;
	}
	
	private ByteBuffer sendRequest(ByteBuffer request) throws DFHackRPCException {
		ByteBuffer result = writeAndRead(request, 8);
		short id = result.getShort();
		if (id != ReplyCode.RPC_REPLY_RESULT.id) {
			throw new DFHackRPCException("unexpected result for bind request: id="+id);
		}
		result.getShort();   // padding
		int size = result.getInt();
		result = ByteBuffer.allocate(size);
		Future<Integer> rres = socket.read(result);
		try {
			if (rres.get() == -1) {
				throw new DFHackRPCException("end-of-stream error while reading result from RPC server");
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new DFHackRPCException("failed to read result from RPC server", e);
		}
		return result.rewind();
	}
	
	private ByteBuffer createRequest(int id, Message msg) {
		assert id < 0x10000;
		ByteBuffer request = createHeader(id, msg.getSerializedSize());
		request.put(msg.toByteArray());
		return request.rewind();
	}
	
	@Cacheable("method_ids")
	private int bindMethod(String method, String imsg, String omsg) throws DFHackRPCException {
		assert plugin != null;
		Builder brb = CoreBindRequest.newBuilder();
		brb.setMethod(method).setInputMsg(plugin+"."+imsg).setOutputMsg(plugin+"."+omsg).setPlugin(plugin);
		ByteBuffer request = createRequest(0, brb.build());
		ByteBuffer result = sendRequest(request);
		CoreBindReply reply;
		try {
			reply = CoreBindReply.parseFrom(result);
		} catch (InvalidProtocolBufferException e) {
			throw new DFHackRPCException("failed to parse reply to bind request", e);
		}
		return reply.getAssignedId();
	}

	/**
	 * Perform RPC call.
	 * @param method RPC method name
	 * @param input protobuf input data
	 * @param outputType name of output type
	 * @return buffer of protobuf-encoded response
	 * @throws DFHackRPCException
	 */
	public ByteBuffer call(String method, Message input, String outputType) throws DFHackRPCException {
		int id = bindMethod(method, input.getClass().getSimpleName(), outputType);
		ByteBuffer request = createRequest(id, input);
		return sendRequest(request).rewind();
	}	
}
