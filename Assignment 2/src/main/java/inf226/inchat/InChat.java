package inf226.inchat;

import inf226.inchat.Account.Role;
import inf226.storage.DeletedException;
import inf226.storage.Stored;
import inf226.util.Maybe;
import inf226.util.Maybe.NothingException;
import inf226.util.Util;
import inf226.util.immutable.List;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * This class models the chat logic.
 *
 * It provides an abstract interface to
 * usual chat server actions.
 *
 **/

public class InChat {
    private final Connection connection;
    private final UserStorage userStore;
    private final ChannelStorage channelStore;
    private final AccountStorage accountStore;
    private final SessionStorage sessionStore;

    public InChat(UserStorage userStore,
                  ChannelStorage channelStore,
                  AccountStorage accountStore,
                  SessionStorage sessionStore,
                  Connection     connection) {
        this.userStore=userStore;
        this.channelStore=channelStore;
        this.accountStore=accountStore;
        this.sessionStore=sessionStore;
        this.connection=connection;
    }

    /**
     * An atomic operation in Inchat.
     * An operation has a function run(), which returns its
     * result through a consumer.
     */
    @FunctionalInterface
    private interface Operation<T,E extends Throwable> {
        void run(final Consumer<T> result) throws E,DeletedException;
    }
    /**
     * Execute an operation atomically in SQL.
     * Wrapper method for commit() and rollback().
     */
    private<T> Maybe<T> atomic(Operation<T,SQLException> op) {
        synchronized (connection) {
            try {
                Maybe.Builder<T> result = Maybe.builder();
                op.run(result);
                connection.commit();
                return result.getMaybe();
            }catch (SQLException | DeletedException e) {
                System.err.println(e);
            }
            try {
                connection.rollback();
            } catch (SQLException e) {
                System.err.println(e);
            }
            return Maybe.nothing();
        }
    }

    /**
     * Log in a user to the chat.
     */
    public Maybe<Stored<Session>> login(final String username,
                                        final String password) {
        return atomic(result -> {
            final Stored<Account> account = accountStore.lookup(username);
            final Stored<Session> session =
                sessionStore.save(new Session(account, Instant.now().plusSeconds(60*60*24)));
            // Check that password is not incorrect and not too long.
            if (account.value.checkPassword(password)) {
                result.accept(session);
            }
        });
    }
    
    /**
     * Register a new user.
     */
    public Maybe<Stored<Session>> register(final String username,
                                           final String password, final String password_repeat) {
        return atomic(result -> {
            if(password.equals(password_repeat) && Password.verify (password )) {
                final Stored<User> user =
                    userStore.save(User.create(username));
                final Stored<Account> account =
                    accountStore.save(Account.create(user, password));
                final Stored<Session> session =
                    sessionStore.save(new Session(account, Instant.now().plusSeconds(60*60*24)));
                result.accept(session); 
            }
        });
    }
    
    /**
     * Restore a previous session.
     */
    public Maybe<Stored<Session>> restoreSession(UUID sessionId) {
        return atomic(result ->
            result.accept(sessionStore.get(sessionId))
        );
    }
    
    /**
     * Log out and invalidate the session.
     */
    public void logout(Stored<Session> session) {
        atomic(result ->
            Util.deleteSingle(session,sessionStore));
    }
    
    /**
     * Create a new channel.
     */
    public Maybe<Stored<Channel>> createChannel(Stored<Account> account,
                                                String name) {
        try {
            if(!channelStore.channelExist(name)) {
                Stored<Channel> channel = channelStore.save(new Channel(name,List.empty()));

                return joinChannel(account,channel.identity);

            }
        } catch (SQLException e) {
            System.err.println("When trying to create channel " + name +":\n" + e);
        }
        return Maybe.nothing();
    }
    
    /**
     * Join a channel.
     */
    public Maybe<Stored<Channel>> joinChannel(Stored<Account> account, UUID channelID, Role role) {
        return atomic(result -> {
            Stored<Channel> channel = channelStore.get(channelID);
            Util.updateSingle(account,
                              accountStore,
                              a -> a.value.joinChannel(channel.value.name,channel, role));
            Stored<Channel.Event> joinEvent
                = channelStore.eventStore.save(
                    Channel.Event.createJoinEvent(channelID,
                        Instant.now(),
                        account.value.user.value.name));
            result.accept(
                Util.updateSingle(channel,
                                  channelStore,
                                  c -> c.value.postEvent(joinEvent)));
        });
    }
    public Maybe<Stored<Channel>> joinChannel(Stored<Account> account, UUID channelID) {
        try {
            String channelName = channelStore.get(channelID).value.name;
            Role role = Util.lookup(account.value.roles, channelName).get();
            return joinChannel(account, channelID, role);

        } catch (NothingException e) {
            //No role // new to the channel
            return joinChannel(account, channelID,Role.Participant);
        } catch (DeletedException e) {
            // This channel has been deleted.
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("When trying to join " + channelID +":\n" + e);
        }
        return Maybe.nothing();
    }
    /**
     * @param account of the user setting the role
     * @param username the string @username of the user that is getting a new role
     * @param roleString the new role for the user @username
     * @param channel the channel that the accounts are a part of.
     */
    public Stored<Channel> setRole(Stored<Account> account, String username, String roleString, Stored<Channel> channel) {
        String alias = channel.value.name;
        Role role = Role.valueOf(roleString);
        try {
            if(account.value.getRole(alias).get() == Role.Owner) {
                Stored<Account> new_account = accountStore.lookup(username);
                if(!(new_account.equals(account) && accountStore.numOfOwners(channel.identity) == 1)) {
                    Util.updateSingle(new_account, accountStore, a -> a.value.setRole(alias, role));
                    return channel;
                }
            }
        } catch (NothingException | SQLException | DeletedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return channel;
    }


    /**
     * Post a message to a channel.
     */
    public Maybe<Stored<Channel>> postMessage(Stored<Account> account,
                                              Stored<Channel> channel,
                                              String message) {
        try {
            Role role = account.value.getRole(channel.value.name).get();
            if(role.ordinal() > 1) {
                Stored<Channel.Event> event = channelStore.eventStore.save(
                        Channel.Event.createMessageEvent(channel.identity,Instant.now(), account.value.user.value.name, message));
                try {
                    return Maybe.just(
                            Util.updateSingle(channel,
                                    channelStore,
                                    c -> c.value.postEvent(event)));
                } catch (DeletedException e) {
                    // Channel was already deleted.
                    // Let us pretend this never happened
                    Util.deleteSingle(event, channelStore.eventStore);
                }

            }
        } catch (SQLException e) {
            System.err.println("When trying to post message in " + channel.identity +":\n" + e);
        } catch (NothingException e1) {
            System.err.println("Nothingexception");
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return Maybe.nothing();
    }

    /**
     * A blocking call which returns the next state of the channel.
     */
    public Maybe<Stored<Channel>> waitNextChannelVersion(UUID identity, UUID version) {
        try{
            return Maybe.just(channelStore.waitNextVersion(identity, version));
        } catch (DeletedException | SQLException e) {
            return Maybe.nothing();
        }
    }
    
    /**
     * Get an event by its identity.
     */
    public Maybe<Stored<Channel.Event>> getEvent(UUID eventID) {
        return atomic(result ->
            result.accept(channelStore.eventStore.get(eventID))
        );
    }
    
    /**
     * Delete an event.
     */
    public Stored<Channel> deleteEvent(Stored<Account> account, Stored<Channel> channel, Stored<Channel.Event> event) {
        try {
            String username = account.value.getName();
            Role role = account.value.getRole(channel.value.name).get();
            if(role.ordinal() >= 3 || (username.equals(event.value.sender) && role.ordinal() > 1)) {
                Util.deleteSingle(event , channelStore.eventStore);
                return channelStore.noChangeUpdate(channel.identity);
            }
        } catch (SQLException er) {
            System.err.println("While deleting event " + event.identity +":\n" + er);
        } catch (DeletedException ignored) {
        } catch (NothingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return channel;
    }

    /**
     * Edit a message.
     */
    public Stored<Channel> editMessage(Stored<Account> account, Stored<Channel> channel, Stored<Channel.Event> event, String newMessage) {
        try{
            String username = account.value.getName();
            Role role = account.value.getRole(channel.value.name).get();
            if(role.ordinal() >= 3 || (username.equals(event.value.sender) && role.ordinal() > 1)) {
                Util.updateSingle(event,
                        channelStore.eventStore,
                        e -> e.value.setMessage(newMessage));
                return channelStore.noChangeUpdate(channel.identity);
            }
        } catch (SQLException er) {
            System.err.println("While deleting event " + event.identity +":\n" + er);
        } catch (DeletedException er) {
            System.err.println("DeletedException");
        } catch (NothingException e1) {
            // TODO Auto-generated catch block
            System.err.println("NothingExcpetion");
            e1.printStackTrace();
        }
        System.err.println("Success");
        return channel;
    }

}

