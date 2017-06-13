package savemgo.nomad;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.packet.PacketDecoder;
import savemgo.nomad.packet.PacketEncoder;

public class ServerHandler extends ChannelInitializer<SocketChannel> {

	private static final PacketEncoder HANDLER_ENCODER = new PacketEncoder();
	private static final PacketDecoder HANDLER_DECODER = new PacketDecoder();

	private final NomadLobby lobby;
	private final EventExecutorGroup executorGroup;

	public ServerHandler(NomadLobby lobby, EventExecutorGroup executorGroup) {
		this.lobby = lobby;
		this.executorGroup = executorGroup;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("encoder", HANDLER_ENCODER);
		pipeline.addLast("decoder", HANDLER_DECODER);
		pipeline.addLast("timeout", new ReadTimeoutHandler(60 * 2));
		pipeline.addLast(executorGroup, "lobby", lobby);
	}

}