package inf226.inchat;
import java.time.Instant;

/**
 * The User class holds the public information
 * about a user.
 **/
public final class User {
    public final String name;
    public final Instant joined;

    public User(String name,
                Instant joined) {
        this.name = name;
        this.joined = joined;
    }
    
    /**
     * Create a new user.
     */
    public static User create(String name) {
        return new User(name, Instant.now());
    }
}

