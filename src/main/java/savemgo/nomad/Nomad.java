package savemgo.nomad;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.instances.NLobbies;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;

public class Nomad {

	private static final Logger logger = LogManager.getLogger(Nomad.class);

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
		try {
			properties.load(new FileInputStream(new File("nomad.properties")));
			key = properties.getProperty("apikey");
			dbUrl = properties.getProperty("dbUrl");
			dbUser = properties.getProperty("dbUser");
			dbPassword = properties.getProperty("dbPassword");
			DB_WORKERS = Integer.parseInt(properties.getProperty("dbWorkers"));
			SERVER_WORKERS = Integer.parseInt(properties.getProperty("serverWorkers"));
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
			Lobby lobbyGateNa = null, lobbyGateJp = null, lobbyGateEu = null, lobbyAccount = null, lobbyGameNa = null,
					lobbyGameEu = null, lobbyGameJp = null;

			session = DB.getSession();
			session.beginTransaction();

			lobbyGateNa = session.get(Lobby.class, 1);
			lobbyAccount = session.get(Lobby.class, 2);
			lobbyGameNa = session.get(Lobby.class, 3);
			lobbyGateJp = session.get(Lobby.class, 4);
			lobbyGateEu = session.get(Lobby.class, 5);
			lobbyGameEu = session.get(Lobby.class, 6);
			lobbyGameJp = session.get(Lobby.class, 7);

			session.getTransaction().commit();
			DB.closeSession(session);

			NLobbies.add(lobbyGateJp);
			NLobbies.add(lobbyGateNa);
			NLobbies.add(lobbyGateEu);
			NLobbies.add(lobbyAccount);
			NLobbies.add(lobbyGameNa);
			NLobbies.add(lobbyGameEu);
			NLobbies.add(lobbyGameJp);

			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();

			EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(SERVER_WORKERS, new ThreadFactory() {

				private int counter = 1;

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "Executor " + counter++);
				}
			});

			NomadServer serverGateJp = new NomadServer(new GateLobby(lobbyGateJp), bossGroup, workerGroup,
					executorGroup);
			NomadServer serverGateNa = new NomadServer(new GateLobby(lobbyGateNa), bossGroup, workerGroup,
					executorGroup);
			NomadServer serverGateEu = new NomadServer(new GateLobby(lobbyGateEu), bossGroup, workerGroup,
					executorGroup);
			NomadServer serverAccount = new NomadServer(new AccountLobby(lobbyAccount), bossGroup, workerGroup,
					executorGroup);
			NomadServer serverGameNa = new NomadServer(new GameLobby(lobbyGameNa), bossGroup, workerGroup, executorGroup);
			NomadServer serverGameEu = new NomadServer(new GameLobby(lobbyGameEu), bossGroup, workerGroup, executorGroup);
			NomadServer serverGameJp = new NomadServer(new GameLobby(lobbyGameJp), bossGroup, workerGroup, executorGroup);

			serverGateJp.start();
			serverGateNa.start();
			serverGateEu.start();
			serverAccount.start();
			serverGameNa.start();
			serverGameEu.start();
			serverGameJp.start();

			logger.info("Started server.");

			NomadService service = new NomadService();
			service.start(() -> {
				Hub.updateLobbyCounts();
				return true;
			}, 30);
			
			ChannelFuture future = serverGameJp.getFuture();

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