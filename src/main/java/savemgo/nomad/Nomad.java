package savemgo.nomad;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.helper.Games;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.instances.NLobbies;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;

public class Nomad {

	private static final Logger logger = LogManager.getLogger();

	private EventLoopGroup bossGroup, workerGroup;

	public static boolean BIND_ON_ALL = true;
	public static int DB_WORKERS = 10;
	public static int SERVER_WORKERS = 10;

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
		ArrayList<Integer> lobbyIds = new ArrayList<>();
		try {
			properties.load(new FileInputStream(new File("nomad.properties")));
			key = properties.getProperty("apikey");
			dbUrl = properties.getProperty("dbUrl");
			dbUser = properties.getProperty("dbUser");
			dbPassword = properties.getProperty("dbPassword");
			DB_WORKERS = Integer.parseInt(properties.getProperty("dbWorkers"));
			SERVER_WORKERS = Integer.parseInt(properties.getProperty("serverWorkers"));

			String strLobbies = properties.getProperty("lobbies");
			String[] strsLobbies = strLobbies.split(",");
			for (String lobStr : strsLobbies) {
				int id = Integer.parseInt(lobStr);
				lobbyIds.add(id);
			}
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
			ArrayList<Lobby> lobbies = new ArrayList<>();

			session = DB.getSession();
			session.beginTransaction();

			for (Integer lobbyId : lobbyIds) {
				Lobby lobby = session.get(Lobby.class, lobbyId);
				lobbies.add(lobby);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			for (Lobby lobby : lobbies) {
				if (lobby.getType() < 0 || lobby.getType() > 2) {
					continue;
				}
				NLobbies.add(lobby);
			}
			Hub.initializeLobbies();

			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();

			EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(SERVER_WORKERS, new ThreadFactory() {

				private int counter = 1;

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "Executor " + counter++);
				}
			});

			ArrayList<NomadServer> servers = new ArrayList<>();
			for (Lobby lobby : lobbies) {
				NomadLobby nLobby = null;
				if (lobby.getType() == 0) {
					nLobby = new GateLobby(lobby);
				} else if (lobby.getType() == 1) {
					nLobby = new AccountLobby(lobby);
				} else if (lobby.getType() == 2) {
					nLobby = new GameLobby(lobby);
				}
				NomadServer nServer = new NomadServer(nLobby, bossGroup, workerGroup, executorGroup);
				servers.add(nServer);
			}
			
			for (NomadServer server : servers) {
				server.start();
			}

			logger.info("Started server.");

			NomadService service = new NomadService();
			service.start(() -> {
				Hub.updateLobbies();
				Games.cleanup();
				return true;
			}, 60);

			ChannelFuture future = servers.get(0).getFuture();

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