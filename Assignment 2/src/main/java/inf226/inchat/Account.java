package inf226.inchat;
import com.lambdaworks.crypto.SCryptUtil;
import inf226.util.immutable.List;
import inf226.util.Pair;
import inf226.storage.Stored;
import inf226.util.Maybe;
import inf226.util.Util;
import inf226.storage.*;

/**
 * The Account class holds all information private to
 * a specific user.
 **/
public final class Account {
    /*
     * A channel consists of a User object of public account info,
     * and a list of channels which the user can post to.
     */
    enum Role {
        Banned,
        Observer,
        Participant,
        Moderator,
        Owner
    }
    public final Stored<User> user;
    public final List<Pair<String,Stored<Channel>>> channels;
    public final List<Pair<String, Role>> roles;
    public final Password pwd;
    
    public Account(final Stored<User> user,
                   final List<Pair<String,Stored<Channel>>> channels,List<Pair<String, Role>> roles,
                   final Password pwd) {
        this.user = user;
        this.channels = channels;
        this.pwd = pwd;
        this.roles = roles;
    }

    /**
     * Create a new Account.
     *
     * @param user The public User profile for this user.
     * @param password The login password for this account.
     **/
    public static Account create(final Stored<User> user,
                                 final String password) {
        return new Account(user,List.empty(), List.empty(), Password.create ( password ));
    }
    
    /**
     * Join a channel with this account.
     *
     * @return A new account object with the cannnel added.
     */
    public Account joinChannel(final String alias, final Stored<Channel> channel,final Role role) {
        Pair<String,Stored<Channel>> entry
                = new Pair<>(alias, channel);
        Pair<String, Role> entryRole = new Pair<>(alias, role);
        return new Account
                (user, List.cons(entry, channels), List.cons(entryRole, roles), pwd);
    }



    /**
     * Check weather if a string is a correct password for
     * this account.
     *
     * @return true if password matches.
     */
    public boolean checkPassword(String password) {
        boolean comparePwd = SCryptUtil.check ( password, pwd.toString () );
        return comparePwd;
    }
   

    /**
     * @param alias of the channel
     * @param role the new role of this account
     * @return a new account that is identical to this one, except it has a new list of channels with its new role.
     */
    public Account setRole(String alias, Role role) {
        final List.Builder<Pair<String,Role>> new_roles = List.builder();
        roles.forEach(element ->{
            //System.err.println("Channel: " + element.first + " Role: " + element.second + " Alias: " + alias);
            if(element.first.equals(alias)) {
                //System.err.println("Changing a role");
                new_roles.accept(new Pair<>(alias, role));
            } else {
                new_roles.accept(element);
            }
        });
        return new Account(user,channels, new_roles.getList(),pwd);
    }


    /**
     * @param alias of the channel
     * @return the role this account has in the given channel.
     */
    public Maybe<Role> getRole(String alias) {
        return Util.lookup(roles, alias);

    }

    public String getName() {
        return user.value.name;
    }
}