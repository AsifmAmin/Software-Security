package inf226.inchat;

import inf226.storage.DeletedException;
import inf226.storage.Storage;
import inf226.storage.Stored;
import inf226.storage.UpdatedException;
import inf226.util.Maybe;
import inf226.util.Pair;
import inf226.util.Util;
import inf226.util.immutable.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * This class stores Channels in a SQL database.
 */
public final class ChannelStorage
    implements Storage<Channel,SQLException> {
    
    final Connection connection;
    /* The waiters object represent the callbacks to
     * make when the channel is updated.
     */
    private final Map<UUID,List<Consumer<Stored<Channel>>>> waiters
        = new TreeMap<>();
    public final EventStorage eventStore;
    
    public ChannelStorage(Connection connection) 
      throws SQLException {
        this.connection = connection;
        this.eventStore = new EventStorage(connection);
        
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS Channel (id TEXT PRIMARY KEY, version TEXT, name TEXT)");
    }
    
    @Override
    public Stored<Channel> save(Channel channel)
      throws SQLException {
        
        final Stored<Channel> stored = new Stored<Channel>(channel);

        String sql = "INSERT INTO Channel VALUES(?, ?, ?);";

        PreparedStatement saveData = connection.prepareStatement(sql);

        saveData.setString(1, stored.identity.toString());
        saveData.setString(2, stored.version.toString());
        saveData.setString(3, channel.name.toString());

        saveData.executeUpdate();

        return stored;
    }
    
    @Override
    public synchronized Stored<Channel> update(Stored<Channel> channel,
                                            Channel new_channel)
        throws UpdatedException,
            DeletedException,
            SQLException {
        final Stored<Channel> current = get(channel.identity);
        final Stored<Channel> updated = current.newVersion(new_channel);
        if(current.version.equals(channel.version)) {
            String sql = "UPDATE Channel SET (version, name) = (?, ?) WHERE id= ?;";

            PreparedStatement updateData = connection.prepareStatement(sql);

            updateData.setString(1, updated.version.toString());
            updateData.setString(2, new_channel.name.toString());
            updateData.setString(3, updated.identity.toString());

            updateData.executeUpdate();
        } else {
            throw new UpdatedException(current);
        }
        giveNextVersion(updated);
        return updated;
    }
   
    @Override
    public synchronized void delete(Stored<Channel> channel)
       throws UpdatedException,
              DeletedException,
              SQLException {
        final Stored<Channel> current = get(channel.identity);
        if(current.version.equals(channel.version)) {
        String sql =  "DELETE FROM Channel WHERE id ='" + channel.identity + "'";
        connection.createStatement().executeUpdate(sql);
        } else {
        throw new UpdatedException(current);
        }
    }
    @Override
    public Stored<Channel> get(UUID id)
      throws DeletedException,
             SQLException {

        final String channelsql = "SELECT version,name FROM Channel WHERE id = '" + id.toString() + "'";
        final String eventsql = "SELECT id,rowid FROM Event WHERE channel = '" + id + "' ORDER BY rowid ASC";

        final Statement channelStatement = connection.createStatement();
        final Statement eventStatement = connection.createStatement();

        final ResultSet channelResult = channelStatement.executeQuery(channelsql);
        final ResultSet eventResult = eventStatement.executeQuery(eventsql);

        if(channelResult.next()) {
            final UUID version = 
                UUID.fromString(channelResult.getString("version"));
            final String name =
                channelResult.getString("name");
            // Get all the events associated with this channel
            final List.Builder<Stored<Channel.Event>> events = List.builder();
            while(eventResult.next()) {
                final UUID eventId = UUID.fromString(eventResult.getString("id"));
                events.accept(eventStore.get(eventId));
            }
            return ( new Stored<>(new Channel(name, events.getList()), id, version));
        } else {
            throw new DeletedException();
        }
    }
    
    /**
     * This function creates a "dummy" update.
     * This function should be called when events are changed or
     * deleted from the channel.
     */
    public Stored<Channel> noChangeUpdate(UUID channelId)
        throws SQLException, DeletedException {
        String sql = "UPDATE Channel SET" +
                " (version) =('" + UUID.randomUUID() + "') WHERE id='"+ channelId + "'";
        connection.createStatement().executeUpdate(sql);
        Stored<Channel> channel = get(channelId);
        giveNextVersion(channel);
        return channel;
    }
    
    /**
     * Get the current version UUID for the specified channel.
     * @param id UUID for the channel.
     */
    public UUID getCurrentVersion(UUID id)
      throws DeletedException,
             SQLException {

        final String channelsql = "SELECT version FROM Channel WHERE id = '" + id.toString() + "'";
        final Statement channelStatement = connection.createStatement();

        final ResultSet channelResult = channelStatement.executeQuery(channelsql);
        if(channelResult.next()) {
            return UUID.fromString(
                    channelResult.getString("version"));
        }
        throw new DeletedException();
    }
    
    /**
     * Wait for a new version of a channel.
     * This is a blocking call to get the next version of a channel.
     * @param identity The identity of the channel.
     * @param version  The previous version accessed.
     * @return The newest version after the specified one.
     */
    public Stored<Channel> waitNextVersion(UUID identity, UUID version)
      throws DeletedException,
             SQLException {
        var result
            = Maybe.<Stored<Channel>>builder();
        // Insert our result consumer
        synchronized(waiters) {
            var channelWaiters 
                = Maybe.just(waiters.get(identity));
            waiters.put(identity
                       ,List.cons(result
                                 ,channelWaiters.defaultValue(List.empty())));
        }
        // Test if there already is a new version avaiable
        if(!getCurrentVersion(identity).equals(version)) {
            return get(identity);
        }
        // Wait
        synchronized(result) {
            while(true) {
                try {
                    result.wait();
                    return result.getMaybe().get();
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted.");
                } catch (Maybe.NothingException e) {
                    // Still no result, looping
                }
            }
        }
    }
    
    /**
     * Notify all waiters of a new version
     */
    private void giveNextVersion(Stored<Channel> channel) {
        synchronized(waiters) {
            Maybe<List<Consumer<Stored<Channel>>>> channelWaiters 
                = Maybe.just(waiters.get(channel.identity));
            try {
                channelWaiters.get().forEach(w -> {
                    w.accept(channel);
                    synchronized(w) {
                        w.notifyAll();
                    }
                });
            } catch (Maybe.NothingException e) {
                // No were waiting for us :'(
            }
            waiters.put(channel.identity,List.empty());
        }
    }

    public List<Pair<String, UUID>> getChannels(){
        try {
            //                .executeUpdate("CREATE TABLE IF NOT EXISTS Channel (id TEXT PRIMARY KEY, version TEXT, name TEXT)");
            final String sql = "SELECT id, name FROM Channel";

            final Statement stmt = connection.createStatement();

            final ResultSet rs = stmt.executeQuery(sql);

            final List.Builder<Pair<String, UUID>> channels = List.builder();
            while(rs.next()) {
                String alias = rs.getString("name");
                UUID id = UUID.fromString(rs.getString("id"));
                channels.accept(new Pair<>(alias, id));
            }

            return channels.getList();

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return List.empty();
    }
    public boolean channelExist(String alias) {
        List<Pair<String, UUID>> channels = getChannels();
        return !Util.lookup(channels, alias).isNothing();
    }
}
 
 
