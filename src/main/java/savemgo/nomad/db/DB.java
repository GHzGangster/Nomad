package savemgo.nomad.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import savemgo.nomad.Nomad;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterAppearance;
import savemgo.nomad.entity.CharacterBlocked;
import savemgo.nomad.entity.CharacterChatMacro;
import savemgo.nomad.entity.CharacterEquippedSkills;
import savemgo.nomad.entity.CharacterFriend;
import savemgo.nomad.entity.CharacterHostSettings;
import savemgo.nomad.entity.CharacterSetGear;
import savemgo.nomad.entity.CharacterSetSkills;
import savemgo.nomad.entity.Clan;
import savemgo.nomad.entity.MessageClanApplication;
import savemgo.nomad.entity.ClanMember;
import savemgo.nomad.entity.ConnectionInfo;
import savemgo.nomad.entity.EventConnectGame;
import savemgo.nomad.entity.EventCreateGame;
import savemgo.nomad.entity.EventDisconnectGame;
import savemgo.nomad.entity.EventEndGame;
import savemgo.nomad.entity.Game;
//github.com/GHzGangster/Nomad.git
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.News;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.plugin.PluginHandler;

public class DB {

	private static final Logger logger = LogManager.getLogger(DB.class);

	private static final Class<?>[] entityClasses = { Character.class, CharacterAppearance.class,
			CharacterBlocked.class, CharacterChatMacro.class, CharacterEquippedSkills.class, CharacterFriend.class,
			CharacterHostSettings.class, CharacterSetGear.class, CharacterSetSkills.class, Clan.class,
			MessageClanApplication.class, ClanMember.class, ConnectionInfo.class, EventCreateGame.class, EventEndGame.class,
			EventConnectGame.class, EventDisconnectGame.class, Game.class, Lobby.class, News.class, Player.class,
			User.class };

	private static ComboPooledDataSource cpds;

	private static SessionFactory sessionFactory;

	public static HashMap<Session, String> sessionCheckouts = new HashMap<>();

	public static boolean initialize(String url, String user, String password, int minPoolSize, int maxPoolSize,
			int poolIncrement) {
		try {
			cpds = new ComboPooledDataSource();
			cpds.setDriverClass("com.mysql.cj.jdbc.Driver");
			cpds.setJdbcUrl(url);
			cpds.setUser(user);
			cpds.setPassword(password);

			cpds.setInitialPoolSize(minPoolSize);
			cpds.setMinPoolSize(minPoolSize);
			cpds.setAcquireIncrement(poolIncrement);
			cpds.setMaxPoolSize(maxPoolSize);

			cpds.setNumHelperThreads(Nomad.DB_WORKERS);

			cpds.setTestConnectionOnCheckout(true);
		} catch (Exception e) {
			logger.error("Failed to initialize DB.", e);
			return false;
		}
		logger.debug("Initialized DB.");
		return true;
	}

	private static SessionFactory buildSessionFactory() {
		try {
			Configuration configuration = new Configuration();

			Properties props = new Properties();
			props.put("hibernate.current_session_context_class", "thread");
			configuration.setProperties(props);

			for (Class<?> clazz : entityClasses) {
				configuration.addAnnotatedClass(clazz);
			}

			PluginHandler.get().getPlugin().addAnnotatedClass(configuration);

			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
					.applySettings(configuration.getProperties())
					.addService(ConnectionProvider.class, new NomadConnectionProvider()).build();

			SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);

			return sessionFactory;
		} catch (Throwable ex) {
			logger.error("Failed to build session factory.", ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		if (sessionFactory == null) {
			sessionFactory = buildSessionFactory();
		}
		return sessionFactory;
	}

	public static Connection get() {
		Connection conn = null;
		try {
			long time = System.currentTimeMillis();
			conn = cpds.getConnection();
			time = System.currentTimeMillis() - time;
			if (time >= 1000L) {
				logger.warn("Took a long time to get a connection: {} ms", time);
			}
		} catch (Exception e) {
			logger.error("Failed to get Connection from DB.", e);
		}
		return conn;
	}

	public static void close(Connection conn) {
		close(conn, (Statement) null, null);
	}

	public static void close(Connection conn, Statement stmt) {
		close(conn, stmt, null);
	}

	public static void close(Connection conn, NamedParameterStatement stmt) {
		close(conn, stmt.getPrepareStatement(), null);
	}

	public static void close(Connection conn, NamedParameterStatement stmt, ResultSet rs) {
		close(conn, stmt.getPrepareStatement(), rs);
	}

	public static void close(Connection conn, Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
				// Ignored
			}
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				// Ignored
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				// Ignored
			}
		}
	}

	public static Session getSession() {
		String callerName = Thread.currentThread().getStackTrace()[2].getMethodName();
		Session session = getSessionFactory().getCurrentSession();
		if (session.getTransaction().isActive()) {
			logger.error("Transaction is active on checkout! Last checked out by: {}", sessionCheckouts.get(session));
		}
		sessionCheckouts.put(session, callerName);
		return session;
	}

	public static void closeSession(Session session) {
		if (session != null && session.isOpen()) {
			if (session.getTransaction().isActive()) {
				logger.error("Transaction is active on close! Last checked out by: {}", sessionCheckouts.get(session));
			}
			try {
				session.close();
			} catch (Exception e) {
				// Ignored
			}
		}
		sessionCheckouts.remove(session);
	}

	public static void rollbackAndClose(Session session) {
		if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
			try {
				session.getTransaction().rollback();
			} catch (Exception e) {
				// Ignored
			}
		}
		closeSession(session);
	}

}
