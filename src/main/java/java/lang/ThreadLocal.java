/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;
import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 * 这个类提供线程局部变量。这些变量在每一个线程中的正常副本都不相同，每一个线程访问一个副本（通过其 get或 set法），副本有自己独立的变量初始化复制。
 * ThreadLocal实例通常是类中私有的静态字段希望关联状态和线程（例如，一个用户ID或交易ID）。
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * 例如，下面的类为每个线程生成唯一的标识符。一个线程的ID在第一次调用ThreadId.get()时分配并且在后续调用中保持不变。
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 * 每一个线程在活着的时候就持有一个暗示对它线程本地变量拷贝的引用并且ThreadLocal实例可以访问；
 * 线程死去后它的线程本地实例拷贝受垃圾回收管辖（除非存在其他对这些副本引用）。
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     * ThreadLocal依赖每个线程线性探测hash表关联到每个线程(Thread.threadLocals和inheritableThreadLocals)。
     * ThreadLocal对象作为key，通过threadLocalHashCode来查找。
     * 这是一个典型的hash值(只在ThreadLocalMaps内有用)消除当同样的线程连续创建ThreadLocal实例常见状况下的冲突，同时不太常见的状况也是良性的。
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     * 下一个要给出的hash值，自动更新，从0开始。
     */
    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     * 连续产生的hash值之间的差值-使得连续的线程本地ID变得隐式连续，接近最佳地扩展到大小是2的指数次表的乘法倍数的hash值上。
     */
    private static final int HASH_INCREMENT = 0x61c88647;//‭0110 0001 1100 1000 1000 0110 0100 0111‬

    /**
     * Returns the next hash code.
     * 返回下一个hash值
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     * 返回此线程局部变量的“初始值”。该方法将在一个线程第一次用get方法访问变量时被调用，
     * 除非线程之前调用了 set方法，在这种情况下， initialValue方法不会被这个线程调用。
     * 通常情况下，这种方法是每个线程调用一次，但它可能在后续调用get随后调用remove而再次调用。 
     *
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     * 这种实现简单的返回null；如果程序员渴望线程局部变量有一个初始值而不是null，ThreadLocal必须有子类，
     * 并重写这个方法。通常情况下，将使用一个匿名内部类。
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     * 创建一个线程局部变量。变量初始值由调用以Supplier为参数的get方法决定
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     * 返回这个线程本地变量在当前线程中的副本值。如果这个变量在当前线程中没有值，第一次通过调用initialValue初始化这个值并返回
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();//map不存在或map中没有这个对象时调用setInitialValue
    }

    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     * set方法的变体，用于创建初始化值。如果用户已经重写了set方法，可以用作set方法的替代
     *
     * @return the initial value
     */
    private T setInitialValue() {
        T value = initialValue();//未重写直接返回null
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     * 设置当前线程的线程本地变量副本为指定的值。大部分子类不需要重写这个方法，只依靠initialValue来设置线程本地变量值
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     * 移除当前线程的线程本地变量值。如果这个线程本地变量随后被当前线程用get方法读取，它的值会被initialValue重新初始化，
     * 除非这之间当前线程调用了set方法。这可能会导致当前线程中多次调用initialValue方法。
     *
     * @since 1.5
     */
     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)//map未初始化时什么都不做
             m.remove(this);
     }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     * 获取关联ThreadLocal的map。在InheritableThreadLocal中重写。
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     * 创建一个关联ThreadLocal的map。在InheritableThreadLocal中重写。
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     * ThreadLocalMap是一个典型的hash表，只适合存储线程本地变量。没有操作暴露到ThreadLocal类外部。
     * 这个类是包私有的，允许在Thread中声明字段。为了帮助解决非常大并且长期存活的使用，hash表条目对key使用WeakReference。
     * 然而，因为没有使用引用队列，旧的条目只在表用完空间时才保证移除。
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         * 这个hash表的条目扩展了WeakReference，使用它的只要引用字段作为k(总是ThreadLocal对象)。
         * 注意null的key值(比如entry.get() == null)意味着key不再被引用，因此条目可以从表擦除。
         * 这样的条目作为“旧条目”在下方代码中被引用。
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal.关联这个ThreadLocal的值 */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);//将ThreadLocal作为key用于构造WeakReference
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         * 初始容量，必需是2的指数次
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         * 表，有必要的话需要resize。table.length必须总是2的指数次
         */
        private Entry[] table;

        /**
         * The number of entries in the table.
         * 表中条目的数量
         */
        private int size = 0;

        /**
         * The next size value at which to resize.
         * 到达后要进行resize的大小
         */
        private int threshold; // Default to 0

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         * 设置resize的门槛保证最差有2/3的负载因子
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
         * 增加i取模len
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         * 减少i取模len
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         * 创建一个新的map初始化包含firstKey, firstValue)。
         * ThreadLocalMap是懒汉式构建，因此我们只有当有至少一个条目要放入时再创建一个
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         * 创建一个新的map包含所有来自父map的可继承的ThreadLocal。只会由createInheritedMap调用。
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];//e是父亲map中的条目
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);//根据hash值计算位置
                        while (table[h] != null)
                            h = nextIndex(h, len);//线性探测处理冲突
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         * 获取和key关联的条目。这个方法本身只处理快速通道：已有key直接命中，否则会转移到getEntryAfterMiss。
         * 这种设计是为了最大化直接命中的效率，部分通过使这个方法容易非线性读取来实现。
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);//根据hash值计算出直接命中的位置
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;//命中
            else
                return getEntryAfterMiss(key, i, e);//未命中
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         * getEntry方法用于key在直接hash位置没有找到时的版本
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                if (k == null)
                    expungeStaleEntry(i);//不再有引用了，需要擦除
                else
                    i = nextIndex(i, len);//向后移动一位
                e = tab[i];
            }
            return null;
        }

        /**
         * Set the value associated with key.
         * 设置和key关联的value值
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.
        	//没有像get那样使用快速路径是因为使用set创建一个新的条目至少和替换一个已有的是一样频率，这样快速路径会经常失败

            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;//替换已有值
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);//替换过时的值
                    return;
                }
            }

            tab[i] = new Entry(key, value);//hash值对应的位置没有插入过元素
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         * 移除key对应的条目
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {//线性查找hash值相等的条目
                if (e.get() == key) {
                    e.clear();//清除引用
                    expungeStaleEntry(i);//删掉过期条目
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         * 用一个有指定key的条目替换在set操作中遇到的过时条目。value参数传递的值存储在条目中，无论有这个key的条目是否已经存在。
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         * 作为一个副作用，这个方法擦除了一趟中所有过时的条目(一趟指两个null位置间的序列)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            //返回检查当前趟中之前的过期条目。我们一次清除整个趟避免因为垃圾回收释放串中的引用而频繁增加的rehash
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))//从staleSlot开始向前直到entry为null的位置为止最靠前的引用为null的条目
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            //寻找key或者趟里后面null位置两者中先发生的
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {//循环从staleSlot向后倒entry为null的位置
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                //如果找到key，我们需要交换它和过期条目来保持hash表顺序。新的过期位置或者在它之前任何其他过期位置，
                //可以被发送给expungeStaleEntry来移除或者rehash趟中的其他所有条目
                if (k == key) {//找到了set操作中要插入的key
                    e.value = value;

                    tab[i] = tab[staleSlot];//将key相同的结点与staleSlot位置的结点交换
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists如果有的话开始擦除先前的过期结点
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);//第一个参数是从slotToExpunge到下一个null的位置，第二个参数是table长度
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                //向前查找没有找到过期条目，在查找key时第一个发现的过期条目还是趟中的第一个
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot如果没有找到key，将新的条目放在过期的位置
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them如果这一趟中还有其他过期条目，擦除它们
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         * 通过rehash任何在staleSlot与下一个null位置之间可能冲突条目来擦除过期条目。
         * 这也擦除了在随后的null之前遇到的所有其他过期条目
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).staleSlot之后下一个null的位置(在staleSlot和这个位置之间的都被检查是否要擦除)
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot擦除在staleSlot的条目
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null直到遇到null之前rehash
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                if (k == null) {//key为null说明已经被remove，删除其他数据
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {//key存在，移动到接近hash直接定位的地方
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         * 启发式地扫描一些存储格寻找过期条目。这个方法在增加一个新元素或者另一个过期元素被擦除时被调用。
         * 它执行一个对数检查，来平衡不检查(快速但是留下垃圾)和检查与元素数量成比例的数(可以找到所有垃圾但是引起更多插入花费O(n)的时间)
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         * 确定没有过期条目的位置，检查从i之后的位置开始
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         * 检查log2(n)个位置，除非找到了一个过期条目，额外检查log2(table.length)-1个位置。
         * 插入时调用这个参数是元素个数，replaceStaleEntry调用时这个参数是table的大小。
         *
         * @return true if any stale entries have been removed.
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {//找到过期的条目需要清除
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);//n=n/2
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4)
                resize();//size达到了resize的大小，扩大数组
        }

        /**
         * Double the capacity of the table.
         * 两倍扩大表
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         * 清除表中稳定所有过期条目
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
