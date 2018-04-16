package EventService.EventConcurrency;

import com.google.gson.JsonObject;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe Event class.
 */
public class Event {
    private final ReentrantReadWriteLock lock;
    private final int eventId;
    private final String eventName;
    private final int createUserId;
    private final int numtickets;
    private int avail;
    private int purchased;

    /**
     * Builder pattern to create an Event class with part of immutable data.
     */
    static class EventBuilder {
        private int eventId;
        private String eventName;
        private int createUserId;
        private int numtickets;
        private int purchased = 0;

        /**
         * EventId setter.
         *
         * @param eventId
         * @return EventBuilder
         */
        EventBuilder setEventId(int eventId) {
            this.eventId = eventId;
            return this;
        }

        /**
         * EventName setter.
         *
         * @param eventName
         * @return EventBuilder
         */
        EventBuilder setEventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        /**
         * CreateUserId setter.
         *
         * @param createUserId
         * @return EventBuilder
         */
        EventBuilder setCreateUserId(int createUserId) {
            this.createUserId = createUserId;
            return this;
        }

        /**
         * Total number of tickets setter.
         *
         * @param numtickets
         * @return EventBuilder
         */
        EventBuilder setNumtickets(int numtickets) {
            this.numtickets = numtickets;
            return this;
        }

        /**
         * Puchased number setter for backup usage.
         *
         * @param purchased
         * @return EventBuilder
         */
        EventBuilder setPurchased(int purchased) {
            this.purchased = purchased;
            return this;
        }

        /**
         * Build method.
         *
         * @return Event
         */
        Event build() {
            return new Event(this);
        }
    }

    /**
     * Constructor of Event.
     *
     * @param eb
     */
    private Event(EventBuilder eb) {
        this.lock = new ReentrantReadWriteLock();
        this.eventId = eb.eventId;
        this.eventName = eb.eventName;
        this.createUserId = eb.createUserId;
        this.numtickets = eb.numtickets;
        this.purchased = eb.purchased;
        this.avail = this.numtickets - this.purchased;
    }

    /**
     * Synchronized purchase method to check availability and purchase.
     *
     * @param tickets
     * @return boolean
     *      - true for success, false for fail
     */
    public boolean purchase(int tickets) {
        boolean result = false;

        this.lock.writeLock().lock();
        if (this.avail - tickets >= 0 && this.purchased + tickets >= 0 &&
                (this.avail - tickets) + (this.purchased + tickets) == this.numtickets) {
            this.avail -= tickets;
            this.purchased += tickets;
            result = true;
        }
        this.lock.writeLock().unlock();

        return result;
    }

    /**
     * Synchronized toJsonObject method to get the detail of Event with JSON format.
     *
     * @return JsonObject
     *      - event detail
     */
    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();

        this.lock.readLock().lock();
        obj.addProperty("eventid", this.eventId);
        obj.addProperty("eventname", this.eventName);
        obj.addProperty("userid", this.createUserId);
        obj.addProperty("avail", this.avail);
        obj.addProperty("purchased", this.purchased);
        this.lock.readLock().unlock();

        return obj;
    }

    void lockForBackup() {
        this.lock.readLock().lock();
    }

    void unlockFromBackup() {
        this.lock.readLock().unlock();
    }
}
