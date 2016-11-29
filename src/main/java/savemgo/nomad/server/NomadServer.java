package savemgo.nomad.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NomadServer {

	private ServerBootstrap sb;
	private ChannelFuture future;

	public NomadServer(Lobby lobby, String ip, int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		this(lobby, ip, port, bossGroup, workerGroup, 16);
	}

	public NomadServer(Lobby lobby, String ip, int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
			int executorThreads) {
		sb = new ServerBootstrap();
		sb.group(bossGroup, workerGroup);
		sb.channel(NioServerSocketChannel.class);
		sb.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).option(ChannelOption.SO_BACKLOG, 512)
				.option(ChannelOption.SO_RCVBUF, 1024).option(ChannelOption.SO_SNDBUF, 1024)
				.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1024));
		sb.childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
				new WriteBufferWaterMark(8 * 1024, 32 * 1024));
		sb.childHandler(new ServerInitializer(lobby, executorThreads));
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
