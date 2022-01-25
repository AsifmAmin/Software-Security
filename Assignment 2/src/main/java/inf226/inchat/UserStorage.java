package inf226.inchat;

import inf226.storage.DeletedException;
import inf226.storage.Storage;
import inf226.storage.Stored;
import inf226.storage.UpdatedException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;


/**
 * The UserStore stores User objects in a SQL database.
 */
public final class UserStorage
    implements Storage<User,SQLException> {
    
    final Connection connection;
    
    public UserStorage(Connection connection) 
      throws SQLException {
        this.connection = connection;
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS User (id TEXT PRIMARY KEY, version TEXT, name TEXT, joined TEXT)");
    }
    
    @Override
    public Stored<User> save(User user)
      throws SQLException {
        final Stored<User> stored = new Stored<User>(user);

        String sql = "INSERT INTO User VALUES(?, ?, ?, ?);";

        PreparedStatement saveData = connection.prepareStatement(sql);

        saveData.setString(1, stored.identity.toString());
        saveData.setString(2, stored.version.toString());
        saveData.setString(3, user.name.toString());
        saveData.setString(4, user.joined.toString());

        saveData.executeUpdate();

        return stored;
    }
    
    @Override
    public synchronized Stored<User> update(Stored<User> user,
                                            User new_user)
        throws UpdatedException,
            DeletedException,
            SQLException {
        final Stored<User> current = get(user.identity);
        final Stored<User> updated = current.newVersion(new_user);
        if(current.version.equals(user.version)) {

            String sql = "UPDATE User SET (version, name, joined) = (?, ?, ?) WHERE id= ?,;";

            PreparedStatement updateData = connection.prepareStatement(sql);

            updateData.setString(1, updated.version.toString());
            updateData.setString(2, new_user.name.toString());
            updateData.setString(3, new_user.joined.toString());
            updateData.setString(4, updated.identity.toString());

            updateData.executeUpdate();

        } else {
            throw new UpdatedException(current);
        }
        return updated;
    }
   
    @Override
    public synchronized void delete(Stored<User> user)
       throws UpdatedException,
              DeletedException,
              SQLException {
        final Stored<User> current = get(user.identity);
        if(current.version.equals(user.version)) {
            String sql =  "DELETE FROM User WHERE id ='" + user.identity + "'";
            connection.createStatement().executeUpdate(sql);
        } else {
        throw new UpdatedException(current);
        }
    }
    @Override
    public Stored<User> get(UUID id)
      throws DeletedException,
             SQLException {
        final String sql = "SELECT version,name,joined FROM User WHERE id = '" + id.toString() + "'";
        final Statement statement = connection.createStatement();
        final ResultSet rs = statement.executeQuery(sql);

        if(rs.next()) {
            final UUID version = 
                UUID.fromString(rs.getString("version"));
            final String name = rs.getString("name");
            final Instant joined = Instant.parse(rs.getString("joined"));
            return ( new Stored<>
                    (new User(name, joined), id, version));
        } else {
            throw new DeletedException();
        }
    }

}


