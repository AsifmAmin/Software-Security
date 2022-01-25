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




public final class EventStorage
    implements Storage<Channel.Event,SQLException> {
    
    private final Connection connection;
    
    public EventStorage(Connection connection) 
      throws SQLException {
        this.connection = connection;
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS Event (id TEXT PRIMARY KEY, version TEXT, channel TEXT, type INTEGER, time TEXT, FOREIGN KEY(channel) REFERENCES Channel(id) ON DELETE CASCADE)");
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS Message (id TEXT PRIMARY KEY, sender TEXT, content Text, FOREIGN KEY(id) REFERENCES Event(id) ON DELETE CASCADE)");
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS Joined (id TEXT PRIMARY KEY, sender TEXT, FOREIGN KEY(id) REFERENCES Event(id) ON DELETE CASCADE)");
    }
    
    @Override
    public Stored<Channel.Event> save(Channel.Event event)
      throws SQLException {
        
        final Stored<Channel.Event> stored = new Stored<Channel.Event>(event);

        String sql = "INSERT INTO Event VALUES(?, ?, ?, ?, ?);";

        PreparedStatement saveData = connection.prepareStatement(sql);

        saveData.setString(1, stored.identity.toString());
        saveData.setString(2, stored.version.toString());
        saveData.setString(3, event.channel.toString());
        saveData.setInt(4, event.type.code);
        saveData.setString(5, event.time.toString());

        saveData.executeUpdate();
        
        switch (event.type) {
            case message:
                sql = "INSERT INTO Message VALUES(?, ?, ?);";

                saveData = connection.prepareStatement(sql);

                saveData.setString(1, stored.identity.toString());
                saveData.setString(2, event.sender.toString());
                saveData.setString(3, event.message.toString());

                break;
            case join:
                sql = "INSERT INTO Joined VALUES(?, ?);";

                saveData = connection.prepareStatement(sql);

                saveData.setString(1, stored.identity.toString());
                saveData.setString(2, stored.identity.toString());

                break;
        }
        saveData.executeUpdate();

        return stored;
    }
    
    @Override
    public synchronized Stored<Channel.Event> update(Stored<Channel.Event> event,
                                            Channel.Event new_event)
        throws UpdatedException,
            DeletedException,
            SQLException {
    final Stored<Channel.Event> current = get(event.identity);
    final Stored<Channel.Event> updated = current.newVersion(new_event);
    if(current.version.equals(event.version)) {
        
        String sql = "UPDATE Event SET (version, channel, time, type) = (?, ?, ?, ?) WHERE id = ?;";

        PreparedStatement updateData = connection.prepareStatement(sql);

        updateData.setString(1, updated.version.toString());
        updateData.setString(2, new_event.channel.toString());
        updateData.setString(3, new_event.time.toString());
        updateData.setInt(4, new_event.type.code);
        updateData.setString(5, updated.identity.toString());

        updateData.executeUpdate();

        switch (new_event.type) {
            case message:
                sql = "UPDATE Message SET (sender,content) = (?, ?) WHERE id = ?;";

                updateData = connection.prepareStatement(sql);

                updateData.setString(1, new_event.sender.toString());
                updateData.setString(2, new_event.message.toString());
                updateData.setString(3, updated.identity.toString());

                break;
            case join:
                sql = "UPDATE Joined SET (sender) = (?) WHERE id = ?;";

                updateData = connection.prepareStatement(sql);

                updateData.setString(1, new_event.sender.toString());
                updateData.setString(2, updated.identity.toString());

                break;
        }
        updateData.executeUpdate();

    } else {
        throw new UpdatedException(current);
    }
        return updated;
    }
   
    @Override
    public synchronized void delete(Stored<Channel.Event> event)
       throws UpdatedException,
              DeletedException,
              SQLException {
        final Stored<Channel.Event> current = get(event.identity);
        if(current.version.equals(event.version)) {
        String sql =  "DELETE FROM Event WHERE id ='" + event.identity + "'";
        connection.createStatement().executeUpdate(sql);
        } else {
        throw new UpdatedException(current);
        }
    }
    @Override
    public Stored<Channel.Event> get(UUID id)
      throws DeletedException,
             SQLException {
        final String sql = "SELECT version,channel,time,type FROM Event WHERE id = '" + id.toString() + "'";
        final Statement statement = connection.createStatement();
        final ResultSet rs = statement.executeQuery(sql);

        if(rs.next()) {
            final UUID version = UUID.fromString(rs.getString("version"));
            final UUID channel = 
                UUID.fromString(rs.getString("channel"));
            final Channel.Event.Type type = 
                Channel.Event.Type.fromInteger(rs.getInt("type"));
            final Instant time = 
                Instant.parse(rs.getString("time"));
            
            final Statement mstatement = connection.createStatement();
            switch(type) {
                case message:
                    final String msql = "SELECT sender,content FROM Message WHERE id = '" + id + "'";
                    final ResultSet mrs = mstatement.executeQuery(msql);
                    mrs.next();
                    return new Stored<Channel.Event>(
                            Channel.Event.createMessageEvent(channel,time,mrs.getString("sender"),mrs.getString("content")),
                            id,
                            version);
                case join:
                    final String asql = "SELECT sender FROM Joined WHERE id = '" + id + "'";
                    final ResultSet ars = mstatement.executeQuery(asql);
                    ars.next();
                    return new Stored<Channel.Event>(
                            Channel.Event.createJoinEvent(channel,time,ars.getString("sender")),
                            id,
                            version);
            }
        }
        throw new DeletedException();
    }
    
}


 
