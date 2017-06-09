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
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.instances.NLobbies;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;

public class Nomad {

	private static final Logger logger = LogManager.getLogger(Nomad.class);

	private EventLoopGroup bossGroup, workerGroup;

	public static boolean bindOnAllIPs = false;
	
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

		Session session = null;
		try {
			Lobby lobbyGateNa = null, lobbyAccount = null, lobbyGame = null;

			session = DB.getSession();
			session.beginTransaction();
			
			lobbyGateNa = session.get(Lobby.class, 1);
			lobbyAccount = session.get(Lobby.class, 2);
			lobbyGame = session.get(Lobby.class, 3);
			
			session.getTransaction().commit();
			DB.closeSession(session);
			
			NLobbies.add(lobbyGateNa);
			NLobbies.add(lobbyAccount);
			NLobbies.add(lobbyGame);
			
			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();

			NomadServer serverGateNa = new NomadServer(new GateLobby(lobbyGateNa), bossGroup, workerGroup, 64);
			NomadServer serverAccount = new NomadServer(new AccountLobby(lobbyAccount), bossGroup, workerGroup, 64);
			NomadServer serverGame = new NomadServer(new GameLobby(lobbyGame), bossGroup, workerGroup, 64);

			serverGateNa.start();
			serverAccount.start();
			serverGame.start();

			logger.info("Started server.");

			ChannelFuture future = serverGame.getFuture();

			try {
				future.sync();

				future.channel().closeFuture().sync();
			} catch (Exception e) {
				logger.error(e);
			}
		} catch (Exception e) {
			logger.error("Exception while starting server.", e);
			DB.rollbackAndClose(session);
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