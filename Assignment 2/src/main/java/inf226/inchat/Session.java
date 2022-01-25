package inf226.inchat;
import java.time.Instant;
import inf226.storage.*;


/**
 * The Session class represents a session.
 * A session is created each time the user logs in.
 */
public final class Session {
    final Stored<Account> account;
    final Instant expiry;

    public Session( Stored<Account> account, Instant expiry) {
        this.account = account;
        this.expiry = expiry;
    }
}
