package savemgo.nomad;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Clan;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.MessageClanApplication;
import savemgo.nomad.helper.Games;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.instances.NLobbies;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;
import savemgo.nomad.plugin.PluginHandler;

public class Nomad {

	private static final Logger logger = LogManager.getLogger();

	private EventLoopGroup bossGroup, workerGroup;

	public static boolean BIND_ON_ALL = true;
	public static int DB_WORKERS = 10;
	public static int SERVER_WORKERS = 10;

	public void test() {
		Session session = null;
		try {
			int clanId = 1;
			boolean isLeader = true;
			
			session = DB.getSession();
			session.beginTransaction();

			Query<Clan> query = session.createQuery(
					"from Clan c join fetch c.members m join fetch m.character where c.id = :clan", Clan.class);
			query.setParameter("clan", clanId);

			Clan clan = query.uniqueResult();

			if (clan != null && isLeader) {
//				Hibernate.initialize(clan.getApplications());
				
				Query<MessageClanApplication> queryM = session
						.createQuery("from MessageClanApplication m join fetch m.character where m.clan = :clan", MessageClanApplication.class);
				queryM.setParameter("clan", clan);
				clan.setApplications(queryM.list());
			}

			if (isLeader) {
				logger.debug("{} applications (2).", clan.getApplications().size());
			}
			
			session.getTransaction().commit();
			DB.closeSession(session);
		} catch (Exception e) {
			logger.error("Exception occurred!", e);
			DB.rollbackAndClose(session);
		}
		
		logger.debug("DONE!");
	}
	
	public Nomad() {
		Properties properties = new Properties();
		String key = "";
		String dbUrl = null, dbUser = null, dbPassword = null;
		int dbPoolMin = 0, dbPoolMax = 0, dbPoolIncrement = 0;
		String plugin = null;
		ArrayList<Integer> lobbyIds = new ArrayList<>();
		try {
			properties.load(new FileInputStream(new File("nomad.properties")));
			key = properties.getProperty("apikey");
			dbUrl = properties.getProperty("dbUrl");
			dbUser = properties.getProperty("dbUser");
			dbPassword = properties.getProperty("dbPassword");
			plugin = properties.getProperty("plugin");
			DB_WORKERS = Integer.parseInt(properties.getProperty("dbWorkers"));
			dbPoolMin = Integer.parseInt(properties.getProperty("dbPoolMin"));
			dbPoolMax = Integer.parseInt(properties.getProperty("dbPoolMax"));
			dbPoolIncrement = Integer.parseInt(properties.getProperty("dbPoolIncrement"));
			SERVER_WORKERS = Integer.parseInt(properties.getProperty("serverWorkers"));

			String strLobbies = properties.getProperty("lobbies");
			String[] strsLobbies = strLobbies.split(",");
			for (String lobStr : strsLobbies) {
				int id = Integer.parseInt(lobStr);
				lobbyIds.add(id);
			}
		} catch (Exception e) {
			logger.error("Error while reading properties file.", e);
			return;
		}

		if (plugin != null) {
			try {
				PluginHandler.get().loadPlugin(plugin);
			} catch (Exception e) {
				logger.error("Error while loading plugin.");
			}
		}

		PluginHandler.get().getPlugin().initialize();

		DB.initialize(dbUrl, dbUser, dbPassword, dbPoolMin, dbPoolMax, dbPoolIncrement);

//		if (Math.sqrt(1) == 1) {
//			test();
//			return;
//		}
		
		PluginHandler.get().getPlugin().onStart();

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

			PluginHandler.get().getPlugin().onStart();

			NomadService service = new NomadService(() -> {
				Hub.updateLobbies();
				Games.cleanup();
				return true;
			}, 60);
			service.start();

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

			if (bossGroup != null && workerGroup != null) {
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}

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
		try {
			new Nomad();
		} catch (Exception e) {
			logger.error("Failed to start Nomad.", e);
		}
	}

}