package savemgo.nomad;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.DefaultAttributeMap;
import savemgo.nomad.server.Lobby;
import savemgo.nomad.server.NServer;

public class Nomad {

	private NServer server, server1, server2;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	public Nomad() {
		try {			
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();

			Lobby lobby = new Lobby();
			server = new NServer(lobby, "0.0.0.0", 5731, bossGroup, workerGroup);
			server1 = new NServer(lobby, "0.0.0.0", 5732, bossGroup, workerGroup);
			server2 = new NServer(lobby, "0.0.0.0", 5733, bossGroup, workerGroup);
			
			System.out.println("Starting...");
			
			server.start();
			server1.start();
			server2.start();
			
			ChannelFuture future = server.getFuture();
			
			try {
				future.sync();
				future.channel().closeFuture().sync();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			System.out.println("Shutting down...");
			
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	public static void main(String[] args) {
		new Nomad();
	}

}
