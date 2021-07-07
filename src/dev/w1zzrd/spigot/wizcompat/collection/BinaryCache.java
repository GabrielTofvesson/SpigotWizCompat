package dev.w1zzrd.spigot.wizcompat.collection;

import java.util.Comparator;
import java.util.Objects;

public final class BinaryCache<K, V> {
    private static final boolean DEFAULT_NULLABILITY = false;

    private final Object[] keys;
    private final Object[] values;
    private final Object[] ages;

    int entryCount = 0;
    int oldest = 0;

    private final Comparator<K> comparator;
    private final CacheMissHandler<K, V> cacheMiss;
    private final boolean nullableValues;


    public static <K, V> BinaryCache<K, V> makeCache(final int cacheSize, final Comparator<K> comparator, final CacheMissHandler<K, V> cacheMiss, final boolean nullableValues) {
        return new BinaryCache<>(cacheSize, comparator, cacheMiss, nullableValues);
    }

    public static <K, V> BinaryCache<K, V> makeCache(final int cacheSize, final Comparator<K> comparator, final CacheMissHandler<K, V> cacheMiss) {
        return makeCache(cacheSize, comparator, cacheMiss, DEFAULT_NULLABILITY);
    }

    public static <K extends Comparable<K>, V> BinaryCache<K, V> makeCache(final int cacheSize, final CacheMissHandler<K, V> cacheMiss, final boolean nullableValues) {
        return makeCache(cacheSize, K::compareTo, cacheMiss, nullableValues);
    }

    public static <K extends Comparable<K>, V> BinaryCache<K, V> makeCache(final int cacheSize, final CacheMissHandler<K, V> cacheMiss) {
        return makeCache(cacheSize, K::compareTo, cacheMiss, DEFAULT_NULLABILITY);
    }

    private BinaryCache(final int cacheSize, final Comparator<K> comparator, final CacheMissHandler<K, V> cacheMiss, final boolean nullableValues) {
        keys = new Object[cacheSize];
        values = new Object[cacheSize];
        ages = new Object[cacheSize];
        this.comparator = comparator;
        this.cacheMiss = cacheMiss;
        this.nullableValues = nullableValues;
    }

    public void clearValues(final V value) {
        final Object[] scratch1 = new Object[keys.length];

        int copyIndex = 0;
        for (int i = oldest; i < entryCount; ++i)
            if(!Objects.equals(value, values[indexOf((K)ages[i])]))
                scratch1[copyIndex++] = ages[i];

        if (entryCount == ages.length)
            for (int i = 0; i < oldest; ++i)
                if(!Objects.equals(value, values[indexOf((K)ages[i])]))
                    scratch1[copyIndex++] = ages[i];

        if (copyIndex == entryCount)
            return;


        // Just re-index the queue so that the oldest entry lies at index 0
        System.arraycopy(scratch1, 0, ages, 0, copyIndex);
        oldest = 0;

        copyIndex = 0;

        final Object[] scratch2 = new Object[keys.length];
        for (int i = 0; i < entryCount; ++i)
            if (!Objects.equals(value, values[i])) {
                scratch1[copyIndex] = keys[i];
                scratch2[copyIndex++] = values[i];
            }

        System.arraycopy(scratch1, 0, keys, 0, copyIndex);
        System.arraycopy(scratch2, 0, values, 0, copyIndex);

        entryCount -= copyIndex;
    }

    public V get(final K key) {
        int index = indexOf(key);

        if (index >= 0)
            return (V) values[index];

        index = -(index + 1);

        final V value = cacheMiss.loadValue(key);
        if ((value == null) && !nullableValues)
            return null;

        if (entryCount < keys.length) {
            System.arraycopy(keys, index, keys, index + 1, entryCount - index);
            System.arraycopy(values, index, values, index + 1, entryCount - index);

            int ageIndex = oldest + entryCount;
            if (ageIndex >= ages.length)
                ageIndex -= ages.length;

            ages[ageIndex] = key;

            ++entryCount;
        } else {
            if (index > 0)
                --index;

            final int oldestIndex = indexOf((K)ages[oldest]);

            if (oldestIndex > index) {
                System.arraycopy(keys, index, keys, index + 1, oldestIndex - index);
                System.arraycopy(values, index, values, index + 1, oldestIndex - index);
            } else if (oldestIndex < index) {
                System.arraycopy(keys, oldestIndex + 1, keys, oldestIndex, index - oldestIndex);
                System.arraycopy(values, oldestIndex + 1, values, oldestIndex, index - oldestIndex);
            }

            // Overwrite oldest entry with new entry
            ages[oldest] = key;

            // Re-index age list so that current oldest entry becomes youngest
            ++oldest;

            if (oldest >= ages.length)
                oldest -= ages.length;
        }

        keys[index] = key;
        values[index] = value;

        return value;
    }

    private int indexOf(final K key) {
        return binarySearch(keys, comparator, key, entryCount - 1);
    }

    private static <A> int binarySearch(Object[] array, Comparator<A> comp, A key, int maxIndex) {
        int low = 0;
        int high = maxIndex;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final A midVal = (A)array[mid];
            int cmp = comp.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return -(low + 1);
    }

    @FunctionalInterface
    public interface CacheMissHandler<K, V> {
        V loadValue(K key);
    }
}
