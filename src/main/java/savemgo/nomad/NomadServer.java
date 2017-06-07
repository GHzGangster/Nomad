package savemgo.nomad;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import savemgo.nomad.packet.Packet;

public class NomadServer {

	private ServerBootstrap sb;
	private ChannelFuture future;

	public NomadServer(NomadLobby lobby, String ip, int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		this(lobby, ip, port, bossGroup, workerGroup, 16);
	}

	public NomadServer(NomadLobby lobby, String ip, int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
			int executorThreads) {
		sb = new ServerBootstrap();
		sb.group(bossGroup, workerGroup);
		sb.channel(NioServerSocketChannel.class);

		final int BUF_PER_CLIENT = Packet.MAX_PACKET_LENGTH * 4;
		final int MAX_CLIENTS = 2000;
		
		sb.option(ChannelOption.SO_BACKLOG, MAX_CLIENTS);
		sb.option(ChannelOption.SO_REUSEADDR, true);
		sb.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		sb.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(BUF_PER_CLIENT));
		sb.childOption(ChannelOption.SO_SNDBUF, BUF_PER_CLIENT);
		sb.childOption(ChannelOption.SO_RCVBUF, BUF_PER_CLIENT);
		
		sb.childHandler(new ServerHandler(lobby, executorThreads));
		sb.localAddress(ip, port);
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
