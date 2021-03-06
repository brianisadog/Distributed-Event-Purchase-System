package Concurrency;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe ServiceList to maintain membership
 *
 * @param <T>
 */
public class ServiceList<T> {
    private final ReentrantReadWriteLock lock;
    private final Set<T> list;
    private T primary;
    private final String service;

    /**
     * ServiceList constructor.
     * Use TreeSet for storing service list to prevent duplicates,
     * and to sort for Bully Election.
     */
    public ServiceList(String service) {
        this.lock = new ReentrantReadWriteLock();
        this.list = new TreeSet<>();
        this.service = service;
    }

    /**
     * Check if the list contains with the service.
     *
     * @param service
     * @return boolean
     */
    public boolean contains(T service) {
        boolean result;

        this.lock.readLock().lock();
        result = this.list.contains(service);
        this.lock.readLock().unlock();

        return result;
    }

    /**
     * Add the service into the list and return result.
     *
     * @param service
     * @return boolean
     */
    public boolean addService(T service) {
        boolean result;

        this.lock.writeLock().lock();
        result = this.list.add(service);
        this.lock.writeLock().unlock();

        return result;
    }

    /**
     * Remove the service from the list and return result.
     *
     * @param service
     * @return boolean
     */
    public boolean removeService(T service) {
        boolean result;

        this.lock.writeLock().lock();
        result = this.list.remove(service);
        this.lock.writeLock().unlock();

        return result;
    }

    /**
     * Return the snapshot of current service list.
     *
     * @return List
     */
    public List<T> getList() {
        List<T> list = new ArrayList<>();

        this.lock.readLock().lock();
        list.addAll(this.list);
        this.lock.readLock().unlock();

        return list;
    }

    /**
     * Primary Setter.
     *
     * @param primary
     */
    public void setPrimary(T primary) {
        this.lock.writeLock().lock();
        this.primary = primary;
        this.lock.writeLock().unlock();
    }

    /**
     * Return the address of current primary service.
     *
     * @return T
     */
    public T getPrimary() {
        T primary;

        this.lock.readLock().lock();
        primary = this.primary;
        this.lock.readLock().unlock();

        return primary;
    }

    /**
     * Return the details of service in the list.
     *
     * @return JsonArray
     */
    public JsonArray getData() {
        JsonArray array = new JsonArray();

        this.lock.readLock().lock();
        for (T address : this.list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("service", this.service);
            obj.addProperty("address", (String) address);
            obj.addProperty("primary", address.equals(this.primary));
            array.add(obj);
        }
        this.lock.readLock().unlock();

        return array;
    }
}
