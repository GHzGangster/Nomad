package savemgo.nomad.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

public class NomadConnectionProvider implements ConnectionProvider {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean isUnwrappableAs(Class arg0) {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) {
		return null;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		DB.close(conn);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return DB.get();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

}
