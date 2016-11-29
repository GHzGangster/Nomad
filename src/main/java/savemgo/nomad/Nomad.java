package savemgo.nomad;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.server.Lobby;
import savemgo.nomad.server.NomadServer;

public class Nomad {

	private static final Logger logger = LogManager.getLogger(Nomad.class.getSimpleName());

	private EventLoopGroup bossGroup, workerGroup;

	public Nomad() {
		Campbell campbell = Campbell.instance();
		campbell.setBaseUrl("http://api.savemgo.com/campbell/");
//		campbell.setApiKey("ASecretKey");

		logger.info("Starting server...");

		try {
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();

			Lobby gateLobby = new GateLobby(1);
			Lobby accountLobby = new AccountLobby(2);

			NomadServer serverGate = new NomadServer(gateLobby, "0.0.0.0", 5731, bossGroup, workerGroup);
			NomadServer serverLobby = new NomadServer(accountLobby, "0.0.0.0", 5732, bossGroup, workerGroup);

			serverGate.start();
			serverLobby.start();

			logger.info("Started server.");

			ChannelFuture future = serverGate.getFuture();

			try {
				future.sync();
				future.channel().closeFuture().sync();
			} catch (Exception e) {
				logger.error(e);
			}
		} finally {
			logger.info("Shutting down server...");

			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();

			logger.info("Shut down server.");
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