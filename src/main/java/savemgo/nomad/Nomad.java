package savemgo.nomad;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Game;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;

public class Nomad {

	private static final Logger logger = LogManager.getLogger(Nomad.class);

	private EventLoopGroup bossGroup, workerGroup;

	private NomadLobby gateLobby, accountLobby, gameLobby;

	public static void testHibernate() {
		Session session = DB.getSession();
		session.beginTransaction();

		Query<Game> query = session.createQuery("from Game", Game.class);
		List<Game> games = query.list();

		games.size();
		Game game = games.get(0);
		
		Hibernate.initialize(game.getPlayers());

		session.getTransaction().commit();
		DB.closeSession(session);
	}

	public Nomad() {
		Properties properties = new Properties();
		String key = "";
		String dbUrl = null, dbUser = null, dbPassword = null;
		try {
			properties.load(new FileInputStream(new File("nomad.properties")));
			key = properties.getProperty("apikey");
			dbUrl = properties.getProperty("dbUrl");
			dbUser = properties.getProperty("dbUser");
			dbPassword = properties.getProperty("dbPassword");
		} catch (Exception e) {
			logger.error("Error while reading properties file.", e);
		}

		DB.initialize(dbUrl, dbUser, dbPassword);

		// testHibernate();
		// if (Math.sqrt(1) == 1) {
		// return;
		// }

		Campbell campbell = Campbell.instance();
		campbell.setBaseUrl("https://api.savemgo.com/campbell/");
		campbell.setApiKey(key);

		logger.info("Starting server...");

		try {
			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();

			int workersPerServer = 64;

			gateLobby = new GateLobby(1);
			accountLobby = new AccountLobby(2);
			gameLobby = new GameLobby(36);

			// NomadServer serverGate = new NomadServer(gateLobby, "0.0.0.0",
			// 15731, bossGroup, workerGroup,
			// workersPerServer);
			NomadServer serverGateNa = new NomadServer(gateLobby, "0.0.0.0", 15731, bossGroup, workerGroup,
					workersPerServer);
			NomadServer serverAccount = new NomadServer(accountLobby, "0.0.0.0", 5732, bossGroup, workerGroup,
					workersPerServer);
			NomadServer serverGame = new NomadServer(gameLobby, "0.0.0.0", 5733, bossGroup, workerGroup);

			// serverGate.start();
			serverGateNa.start();
			serverAccount.start();
			serverGame.start();

			logger.info("Started server.");

			ChannelFuture future = serverGateNa.getFuture();

			try {
				future.sync();

				// Campbell.instance().getResponse("nomad", "onStart");

				NomadService service = new NomadService();
				service.start(() -> {
					// Campbell.instance().getResponse("nomad",
					// "updateLobbyCounts");
					return true;
				}, 60);

				future.channel().closeFuture().sync();

				// Campbell.instance().getResponse("nomad", "onStop");
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