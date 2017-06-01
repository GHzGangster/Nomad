package savemgo.nomad;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterAppearance;
import savemgo.nomad.lobby.AccountLobby;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.lobby.GateLobby;

public class Nomad {

	private static final Logger logger = LogManager.getLogger(Nomad.class);

	private EventLoopGroup bossGroup, workerGroup;

	private NomadLobby gateLobby, accountLobby, gameLobby;

	public static void testHibernate() {
		// Lobby lobby = new Lobby();
		// lobby.setType(0);
		// lobby.setSubtype(0);
		// lobby.setName("test");
		// lobby.setIp("127.0.0.1");
		// lobby.setPort(1738);
		//
		// Session session =
		// HibernateUtil.getSessionFactory().getCurrentSession();
		// session.beginTransaction();
		// session.save(lobby);
		// session.getTransaction().commit();
		//
		// logger.debug("Lobby ID: {}", lobby.getId());
		//
		// HibernateUtil.getSessionFactory().close();

		// Query query = session.createQuery("from Lobby");
		// List<Lobby> lobbies = query.list();
		// for (Lobby lobby : lobbies) {
		// logger.debug("Lobby {} : {} {} {}", lobby.getId(), lobby.getName(),
		// lobby.getIp(), lobby.getPort());
		// }

		// Query query = session.createQuery("FROM User WHERE id=:id");
		// query.setParameter("id", 1);
		//
		// User user = (User) query.uniqueResult();
		// logger.debug("User {} {} Banned? {}", user.getId(),
		// user.getDisplayName(), user.isBanned());

		// Query<Character> query = session.createQuery("FROM Character WHERE
		// user=:user", Character.class);
		// query.setParameter("user", 1);
		//
		// List<Character> characters = query.list();
		// for (Character chara : characters) {
		// logger.debug("Character {} : {}", chara.getId(), chara.getName());
		// }

		Session session = DB.getSession();
		session.beginTransaction();

		/**
		 * Getting just the character (lazy)
		 * 
		 * FROM Character WHERE user=:user
		 * 
		 * Getting character and appearance in one query
		 * 
		 * FROM Character as c INNER JOIN FETCH c.appearance WHERE user=:user
		 */

		Query<Character> query = session.createQuery("FROM Character AS c INNER JOIN FETCH c.appearances WHERE user=:user", Character.class);
		query.setParameter("user", 1);
		List<Character> characters = query.list();

		for (Character character : characters) {
			logger.debug("Character {} : {}", character.getId(), character.getName());
			
			CharacterAppearance appearance = character.getAppearances().get(0);
			logger.debug("Appearance {} : {}", appearance.getId(), appearance.getGender());
			
			character.setComment("Hibernate test!");
			session.update(character);
			
//			break;
		}

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

//		testHibernate();
//		if (Math.sqrt(1) == 1) {
//			return;
//		}

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
			// NomadServer serverGame = new NomadServer(gameLobby, "0.0.0.0",
			// 5733, bossGroup, workerGroup);

			// serverGate.start();
			serverGateNa.start();
			serverAccount.start();
			// serverGame.start();

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