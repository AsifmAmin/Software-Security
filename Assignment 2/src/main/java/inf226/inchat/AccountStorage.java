package inf226.inchat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import inf226.inchat.Account.Role;
import inf226.storage.DeletedException;
import inf226.storage.Storage;
import inf226.storage.Stored;
import inf226.storage.UpdatedException;
import inf226.util.Maybe;
import inf226.util.Maybe.NothingException;
import inf226.util.Mutable;
import inf226.util.Pair;
import inf226.util.Util;
import inf226.util.immutable.List;

import java.sql.*;
import java.util.UUID;




/**
 * This class stores accounts in the database.
 */
public final class AccountStorage
    implements Storage<Account,SQLException> {
    
    final Connection connection;
    final Storage<User,SQLException> userStore;
    final Storage<Channel,SQLException> channelStore;
   
    /**
     * Create a new account storage.
     *
     * @param  connection   The connection to the SQL database.
     * @param  userStore    The storage for User data.
     * @param  channelStore The storage for channels.
     */
    public AccountStorage(Connection connection,
                          Storage<User,SQLException> userStore,
                          Storage<Channel,SQLException> channelStore) 
      throws SQLException {
        this.connection = connection;
        this.userStore = userStore;
        this.channelStore = channelStore;
        
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS Account (id TEXT PRIMARY KEY, version TEXT, user TEXT, pwd TEXT, FOREIGN KEY(user) REFERENCES User(id) ON DELETE CASCADE)");
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS AccountChannel (account TEXT, channel TEXT, alias TEXT, ordinal INTEGER,role TEXT, PRIMARY KEY(account,channel), FOREIGN KEY(account) REFERENCES Account(id) ON DELETE CASCADE, FOREIGN KEY(channel) REFERENCES Channel(id) ON DELETE CASCADE)");
    }
    
    @Override
    public Stored<Account> save(Account account)
      throws SQLException {
        
        final Stored<Account> stored = new Stored<Account>(account);
        String sql = "INSERT INTO Account VALUES(?, ?, ?, ?);";

        PreparedStatement saveData = connection.prepareStatement(sql);

        saveData.setString(1, stored.identity.toString());
        saveData.setString(2, stored.version.toString());
        saveData.setString(3, account.user.identity.toString());
        saveData.setString(4, account.pwd.toString());

        saveData.executeUpdate();
        
        // Write the list of channels
        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<>(0);
        account.channels.forEach(element -> {
            String alias = element.first;
            Stored<Channel> channel = element.second;
            try {
                Role role = Util.lookup(account.roles, alias).get();
            final String msql
              = "INSERT INTO AccountChannel VALUES('" + stored.identity + "','"
                                                      + channel.identity + "','"
                                                      + alias + "','"
                                                      + ordinal.get().toString() +"','"
                                                      + role + "')";
            connection.createStatement().executeUpdate(msql); }
            catch (SQLException e) { exception.accept(e) ; } catch (NothingException e) {
                System.err.println("Has no role for this channel!");
                e.printStackTrace();
            }
            ordinal.accept(ordinal.get() + 1);
        });

        Util.throwMaybe(exception.getMaybe());
        return stored;
    }
    
    @Override
    public synchronized Stored<Account> update(Stored<Account> account,
                                            Account new_account)
        throws UpdatedException,
            DeletedException,
            SQLException {
    final Stored<Account> current = get(account.identity);
    final Stored<Account> updated = current.newVersion(new_account);
    if(current.version.equals(account.version)) {

        String sql = "UPDATE Account SET (version, user, pwd) = (?, ?,?) WHERE id= ?;";

        PreparedStatement updateData = connection.prepareStatement(sql);

        updateData.setString(1, updated.version.toString());
        updateData.setString(2, new_account.user.identity.toString());
        updateData.setString ( 3,new_account.pwd.toString () );
        updateData.setString(4, updated.identity.toString());

        updateData.executeUpdate();
        
        // Rewrite the list of channels
        connection.createStatement().executeUpdate("DELETE FROM AccountChannel WHERE account='" + account.identity + "'");
        
        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<>(0);
        new_account.channels.forEach(element -> {
            String alias = element.first;
            Stored<Channel> channel = element.second;
        try {
            Role role = Util.lookup(new_account.roles, alias).get();
            final String msql
                = "INSERT INTO AccountChannel VALUES('" + account.identity + "','"
                                                        + channel.identity + "','"
                                                        + alias + "','"
                                                        + ordinal.get().toString() +"','"
                                                        + role + "')";
             connection.createStatement().executeUpdate(msql); }
            catch  (SQLException e) { exception.accept(e) ; } catch (NothingException e) {
            System.err.println("No role for that channel!");
            e.printStackTrace();
        }
            ordinal.accept(ordinal.get() + 1);
        });

        Util.throwMaybe(exception.getMaybe());
    } else {
        throw new UpdatedException(current);
    }
    return updated;
    }
   
    @Override
    public synchronized void delete(Stored<Account> account)
       throws UpdatedException,
              DeletedException,
              SQLException {
        final Stored<Account> current = get(account.identity);
        if(current.version.equals(account.version)) {
        String sql =  "DELETE FROM Account WHERE id ='" + account.identity + "'";
        connection.createStatement().executeUpdate(sql);
        } else {
        throw new UpdatedException(current);
        }
    }
    @Override
    public Stored<Account> get(UUID id)
      throws DeletedException,
             SQLException {

        final String accountsql = "SELECT version,user,pwd FROM Account WHERE id = '" + id.toString() + "'";
        final String channelsql = "SELECT channel,alias,ordinal,role FROM AccountChannel WHERE account = '" + id + "' ORDER BY ordinal DESC";

        final Statement accountStatement = connection.createStatement();
        final Statement channelStatement = connection.createStatement();

        final ResultSet accountResult = accountStatement.executeQuery(accountsql);
        final ResultSet channelResult = channelStatement.executeQuery(channelsql);

        if(accountResult.next()) {
            final UUID version = UUID.fromString(accountResult.getString("version"));
            final UUID userid =
            UUID.fromString(accountResult.getString("user"));
            final Password pwd = new Password ( accountResult.getString ( "pwd" ) );
            final Stored<User> user = userStore.get(userid);
            // Get all the channels associated with this account
            final List.Builder<Pair<String,Stored<Channel>>> channels = List.builder();
            final List.Builder<Pair<String, Role>> roles = List.builder();
            while(channelResult.next()) {
                final UUID channelId = 
                    UUID.fromString(channelResult.getString("channel"));
                final String alias = channelResult.getString("alias");
                final Role role = Role.valueOf(channelResult.getString("role"));
                channels.accept(
                        new Pair<String,Stored<Channel>>(alias, channelStore.get(channelId))); //try to add this
                roles.accept(new Pair<>(alias, role));
            }
            return (new Stored<Account>(new Account(user,channels.getList(), roles.getList(),pwd),id,version));
        } else {
            throw new DeletedException();
        }
    }
    
    /**
     * Look up an account based on their username.
     */
    public Stored<Account> lookup(String username)
      throws DeletedException,
             SQLException {
        final String sql = "SELECT Account.id from Account INNER JOIN User ON user=User.id where User.name='" + username + "'";
        System.err.println("lookup: " + sql);

        System.err.println(sql);
        final Statement statement = connection.createStatement();
        
        final ResultSet rs = statement.executeQuery(sql);
        if(rs.next()) {
            final UUID identity = 
                    UUID.fromString(rs.getString("id"));
            return get(identity);
        }
        throw new DeletedException();
    }
    /**
     * @param channelid a channels ID
     * @return a list of the members in the given channel
     * Should throw instead?
     */
    public List<Pair<String, Role>> memberlist(UUID channelid) {
        try {
            final PreparedStatement stmt = connection.prepareStatement("SELECT User.name, Account.role FROM AccountChannel INNER JOIN User ON Account.user = User.id   where id=?");
            stmt.setObject(1, channelid);
            final ResultSet rs = stmt.executeQuery();

            final List.Builder<Pair<String, Role>> channels = List.builder();
            while(rs.next()) {
                String name = rs.getString("name");
                Role  role = Role.valueOf(rs.getString("role"));
                channels.accept(new Pair<>(name, role));
            }

            return channels.getList();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return List.empty();
    }
    /**
     * @param id of channel
     * @param role the role of the members you want to get
     * @return a list of the members with the @role in channel @id
     */
    public List<String> getMembersWithRole(UUID id,Role role) {
        List<Pair<String, Role>> members =  memberlist(id);
        final List.Builder<String> result = new List.Builder<>();
        members.forEach(pair -> {
            if(pair.second.equals(role))
                result.accept(pair.first);
        });
        return result.getList();
    }

    /**
     * @return the amount of owners in the given channel @id
     */
    public int numOfOwners(UUID id) {
        List<String> owners =  getMembersWithRole(id, Role.Owner);
        return owners.length;

}
}
 
