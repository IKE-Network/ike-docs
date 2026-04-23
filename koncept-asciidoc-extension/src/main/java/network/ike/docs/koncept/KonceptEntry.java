package network.ike.docs.koncept;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks references to a single Koncept within a document.
 * Thread-safe for concurrent macro processing.
 */
public class KonceptEntry {

    private final String identifier;
    private final AtomicInteger refCount = new AtomicInteger(0);

    /**
     * Creates an entry for the given koncept identifier with a reference count of zero.
     *
     * @param identifier the CamelCase koncept identifier
     */
    public KonceptEntry(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the koncept identifier.
     *
     * @return the CamelCase identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Atomically increments and returns the reference count.
     *
     * @return the new reference count after incrementing
     */
    public int incrementRefCount() {
        return refCount.incrementAndGet();
    }

    /**
     * Returns the current reference count.
     *
     * @return the number of times this koncept has been referenced
     */
    public int getRefCount() {
        return refCount.get();
    }

    @Override
    public String toString() {
        return "KonceptEntry{id='%s', refs=%d}".formatted(identifier, refCount.get());
    }
}
