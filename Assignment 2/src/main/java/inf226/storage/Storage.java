package inf226.storage;

import java.util.UUID;

/**
 * This provides an interface for an object storage
 * which implements transactional updating of objects.
 *
 * The main limitation of this interface is that you
 * cannot combine updates of several objects into one
 * transaction.
 **/
public interface Storage<T,E extends Exception> {

   /**
    * Save a new object into the storage.
    *
    * Use this when an object is created.
    **/
   Stored<T> save(T value) throws E;

   /**
    * Update an already stored object with a new value.
    *
    * Use this when you want to save changes to an object.
    * If an UpdatedException is thrown, redo changes with
    * the new version and call update() again.
    **/
   Stored<T> update(Stored<T> object, T new_object) throws UpdatedException,DeletedException,E;
   
   /**
    * Delete an object from the store.
    **/
   void delete(Stored<T> object) throws UpdatedException,DeletedException,E;

   /**
    * Get a stored object based on UUID.
    *
    * Use this if you believe your object might be stale, or
    * to retrieve an object from a serialised reference.
    **/
   Stored<T> get(UUID id) throws DeletedException,E;

}
