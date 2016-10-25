package savemgo.nomad;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.server.NomadServer;

public class Nomad {

	private NomadServer serverGate;

	private EventLoopGroup bossGroup, workerGroup;

	public Nomad() {
		Campbell campbell = Campbell.instance();
		campbell.setBaseUrl("http://api.savemgo.com/campbell/");
		campbell.setApiKey("ASecretKey");

		try {
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();

			serverGate = new NomadServer(new GateLobby(), "0.0.0.0", 5731, bossGroup, workerGroup);

			System.out.println("Starting...");

			serverGate.start();

			System.out.println("Started.");
			
			ChannelFuture future = serverGate.getFuture();

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