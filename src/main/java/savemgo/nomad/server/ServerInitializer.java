package savemgo.nomad.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.packet.PacketDecoder;
import savemgo.nomad.packet.PacketEncoder;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final PacketEncoder HANDLER_ENCODER = new PacketEncoder();
	private static final PacketDecoder HANDLER_DECODER = new PacketDecoder();

	private final Lobby lobby;
	private final EventExecutorGroup eventExecutor;

	public ServerInitializer(Lobby lobby, int executorThreads) {
		this.lobby = lobby;
		eventExecutor = new DefaultEventExecutorGroup(executorThreads);
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("timeout", new ReadTimeoutHandler(300));
		pipeline.addLast("packetEncoder", HANDLER_ENCODER);
		pipeline.addLast("packetDecoder", HANDLER_DECODER);
		pipeline.addLast(eventExecutor, "packetHandler", lobby);
	}

}