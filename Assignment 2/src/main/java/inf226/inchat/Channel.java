package inf226.inchat;

import inf226.storage.Stored;
import inf226.util.immutable.List;

import java.time.Instant;
import java.util.UUID;
/**
 * The Channel class represents a channel.
 */
public final class Channel {
    public final String name;
    public final List<Stored<Event>> events;
    
    /**
     * Construct a Channel object from name and events.
     */
    public Channel(String name, List<Stored<Event>> events) {
        this.name=name;
        this.events=events;
    }
    
    /**
     * Post a new event to the channel.
     */
    public Channel postEvent(Stored<Event> event) {
        return new Channel(name, List.cons(event,events));
    }
    
    /**
     * The Event class represents different kinds of events
     * in a channel, such as "join events" and "message events".
     */
    public static class Event {
        public enum Type {
            message(0),join(1);
            public final Integer code;
            Type(Integer code){this.code=code;}
            public static Type fromInteger(Integer i) {
                if (i.equals(0))
                    return message;
                else if (i.equals(1))
                    return join;
                else
                    throw new IllegalArgumentException("Invalid Channel.Event.Type code:" + i);
            }
        }

        public final UUID channel;
        public final Type type;
        public final Instant time;
        public final String sender;
        public final String message;
        
        /**
         * Copy constructor
         */
        public Event(UUID channel, Instant time, String sender, Type type, String message) {
            if (time == null) {
                throw new IllegalArgumentException("Event time cannot be null");
            }
            if (type.equals(message) && message == null) {
                throw new IllegalArgumentException("null in Event creation");
            }
            this.channel=channel;
            this.time   =time;
            this.sender =sender;
            this.type   =type;
            this.message=message;
        }
        /**
        * Create a message event, which represents a user writing to the channel.
        */
        public static Event createMessageEvent(UUID channel, Instant time, String sender, String message) {
            return new Event(   channel,
                                time,
                                sender,
                                Event.Type.message,
                                message);
        }
        /**
        * Create a message event, which represents a user joining the channel.
        */
        public static Event createJoinEvent(UUID channel,Instant time, String user) {
            return new Event(   channel,
                                time,
                                user,
                                Event.Type.join,
                                null);
        }

        /**
         * Create a new event with a different message.
         */        
        public Event setMessage(String message) {
            return new Event(channel,time,sender,type,message);
        }
    }
}
