package inf226.storage;

import java.util.UUID;

/**
 * This class represents an object stored in a Storage.
 */
public class Stored<T> {
  public final T value;
  public final UUID identity;
  public final UUID version;


  /**
   *  The default constructor for creating a new Stored value.
   **/
  public Stored(T value) {
    this.value = value;
    this.identity = UUID.randomUUID();
    this.version = UUID.randomUUID();
  }

  /**
   * Construct a new version of this stored object.
   **/
  public Stored<T> newVersion(T newValue) {
     return new Stored<T>(newValue , identity, UUID.randomUUID());
  }

  /**
   * The constructor for recreating a stored object
   * from a serialised version.
   **/
  public Stored(T value, UUID identity, UUID version) {
    this.value = value;
    this.identity = identity;
    this.version = version;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Stored<T> stored_other = (Stored<T>) other;
    return this.identity.equals(stored_other.identity)
        && this.version.equals(stored_other.version)
        && this.value.equals(stored_other.value);

  }
}

