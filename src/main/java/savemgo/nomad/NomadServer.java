package savemgo.nomad;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.packet.Packet;

public class NomadServer {

	private ServerBootstrap sb;
	private ChannelFuture future;

	public NomadServer(NomadLobby nLobby, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
			EventExecutorGroup executorGroup) {
		sb = new ServerBootstrap();
		sb.group(bossGroup, workerGroup);
		sb.channel(NioServerSocketChannel.class);

		final int BUF_PER_CLIENT = Packet.MAX_PACKET_LENGTH * 4;
		final int MAX_CLIENTS = 2000;

		sb.option(ChannelOption.SO_BACKLOG, MAX_CLIENTS);
		sb.option(ChannelOption.SO_REUSEADDR, true);
		sb.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		sb.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(BUF_PER_CLIENT));
		sb.childOption(ChannelOption.SO_SNDBUF, 65535);
		sb.childOption(ChannelOption.SO_RCVBUF, 65535);

		sb.childHandler(new ServerHandler(nLobby, executorGroup));
		String ip = Nomad.BIND_ON_ALL ? "0.0.0.0" : nLobby.getLobby().getIp();
		sb.localAddress(ip, nLobby.getLobby().getPort());
	}

	public boolean start() {
		future = sb.bind();
		return true;
	}

	public void stop() {
		future.cancel(true);
		try {
			future.sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ChannelFuture getFuture() {
		return future;
	}

}
