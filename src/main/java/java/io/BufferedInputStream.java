/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.io;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A <code>BufferedInputStream</code> adds
 * functionality to another input stream-namely,
 * the ability to buffer the input and to
 * support the <code>mark</code> and <code>reset</code>
 * methods. When  the <code>BufferedInputStream</code>
 * is created, an internal buffer array is
 * created. As bytes  from the stream are read
 * or skipped, the internal buffer is refilled
 * as necessary  from the contained input stream,
 * many bytes at a time. The <code>mark</code>
 * operation  remembers a point in the input
 * stream and the <code>reset</code> operation
 * causes all the  bytes read since the most
 * recent <code>mark</code> operation to be
 * reread before new bytes are  taken from
 * the contained input stream.
 * BufferedInputStream对另一个输出流增加了功能，支持缓存输入和mark/reset操作。
 * 当创建BufferedInputStream时，一个内部缓冲数组被建立，随着字节从流中被读取或者跳过，内部缓冲区根据需要从包含的输入流中重新装填许多字节。
 * mark操作记录了输入流中的一个位置，reset操作引起在最近一个mark操作前的所有读取的字节在流中新的字节被获取之前被重新读取
 * 
 *
 * @author  Arthur van Hoff
 * @since   JDK1.0
 */
public
class BufferedInputStream extends FilterInputStream {

    private static int DEFAULT_BUFFER_SIZE = 8192;//默认大小8K

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 分配的最大数组大小。一些虚拟机保留了数组中一些头部字。
     * 试图分配更大的数组可能导致OutOfMemoryError：请求的数组大小超过了虚拟机限制
     */
    private static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The internal buffer array where the data is stored. When necessary,
     * it may be replaced by another array of
     * a different size.
     * 内部缓冲数组存储数据，必要时，它会被替换为另一个不同大小的数组
     */
    protected volatile byte buf[];

    /**
     * Atomic updater to provide compareAndSet for buf. This is
     * necessary because closes can be asynchronous. We use nullness
     * of buf[] as primary indicator that this stream is closed. (The
     * "in" field is also nulled out on close.)
     * 原子更新操作为缓冲区提供CAS操作。这是必要的，因为关闭是异步的。
     * 我们使用null的buf[]作为流关闭的主要指示器，输入关闭时输入区域也是null
     */
    private static final
        AtomicReferenceFieldUpdater<BufferedInputStream, byte[]> bufUpdater =
        AtomicReferenceFieldUpdater.newUpdater
        (BufferedInputStream.class,  byte[].class, "buf");

    /**
     * The index one greater than the index of the last valid byte in
     * the buffer.
     * This value is always
     * in the range <code>0</code> through <code>buf.length</code>;
     * elements <code>buf[0]</code>  through <code>buf[count-1]
     * </code>contain buffered input data obtained
     * from the underlying  input stream.
     * 比缓冲区最后一个有效字节的下标大1.这个值得范围总是在0和buf.length之间
     * 元素buf[0]到buf[count-1]含有从下层的输入流中缓冲的输入数据
     */
    protected int count;

    /**
     * The current position in the buffer. This is the index of the next
     * character to be read from the <code>buf</code> array.
     * 缓冲区的当前位置，下一个要从buf数组中被读取的字符的下标
     * <p>
     * This value is always in the range <code>0</code>
     * through <code>count</code>. If it is less
     * than <code>count</code>, then  <code>buf[pos]</code>
     * is the next byte to be supplied as input;
     * if it is equal to <code>count</code>, then
     * the  next <code>read</code> or <code>skip</code>
     * operation will require more bytes to be
     * read from the contained  input stream.
     * 这个值的范围总是在0到count。如果它小于count，buf[pos]是下一个要作为输入提供的字节，
     * 如果它等于count，下一个read或者skip操作需要从输入流中读取更多的字节
     *
     * @see     java.io.BufferedInputStream#buf
     */
    protected int pos;

    /**
     * The value of the <code>pos</code> field at the time the last
     * <code>mark</code> method was called.
     * 上一次mark操作时pos的值
     * <p>
     * This value is always
     * in the range <code>-1</code> through <code>pos</code>.
     * If there is no marked position in  the input
     * stream, this field is <code>-1</code>. If
     * there is a marked position in the input
     * stream,  then <code>buf[markpos]</code>
     * is the first byte to be supplied as input
     * after a <code>reset</code> operation. If
     * <code>markpos</code> is not <code>-1</code>,
     * then all bytes from positions <code>buf[markpos]</code>
     * through  <code>buf[pos-1]</code> must remain
     * in the buffer array (though they may be
     * moved to  another place in the buffer array,
     * with suitable adjustments to the values
     * of <code>count</code>,  <code>pos</code>,
     * and <code>markpos</code>); they may not
     * be discarded unless and until the difference
     * between <code>pos</code> and <code>markpos</code>
     * exceeds <code>marklimit</code>.
     * 这个值总是在-1到pos的范围之间。如果在输入流中没有标记的位置，这个值是-1。如果在输入流中有标记的位置，buf[markpos]在reset操作之后会是第一个作为输入提供的字节。
     * 如果markpos不是-1，从buf[markpos]到buf[pos-1]之间的所有字节需要保留在缓冲区数组中（尽管可能被移动到缓冲区数组的另一个位置使得对count、pos和markpos的值有合适的调整）
     * 它们直到pos和markpos之间的差值超过marklimit前不能被丢弃
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#pos
     */
    protected int markpos = -1;

    /**
     * The maximum read ahead allowed after a call to the
     * <code>mark</code> method before subsequent calls to the
     * <code>reset</code> method fail.
     * Whenever the difference between <code>pos</code>
     * and <code>markpos</code> exceeds <code>marklimit</code>,
     * then the  mark may be dropped by setting
     * <code>markpos</code> to <code>-1</code>.
     * 在调用mark之后到随后调用reset之前能够读取的最大字节数
     * 无论何时pos和markpos之间的差值超过marklimit，标记会被丢弃，markpos被设为-1
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#reset()
     */
    protected int marklimit;

    /**
     * Check to make sure that underlying input stream has not been
     * nulled out due to close; if not return it;
     * 检查确认下层输入流还没有因为关闭变成null，不是null的话返回输入流
     */
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if (input == null)
            throw new IOException("Stream closed");
        return input;
    }

    /**
     * Check to make sure that buffer has not been nulled out due to
     * close; if not return it;
     * 检查确认缓冲区还没有因为关闭变成null，非null的话返回它
     */
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null)
            throw new IOException("Stream closed");
        return buffer;
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use. An internal
     * buffer array is created and  stored in <code>buf</code>.
     * 创建一个BufferedInputStream并存储它的参数供之后使用，输入流是in。一个大小为8K内部的缓冲数组被建立存储在buf
     *
     * @param   in   the underlying input stream.
     */
    public BufferedInputStream(InputStream in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a <code>BufferedInputStream</code>
     * with the specified buffer size,
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.  An internal
     * buffer array of length  <code>size</code>
     * is created and stored in <code>buf</code>.
     * 创建一个指定缓冲区大小的BufferedInputStream并存储它的参数供之后使用，输入流是in。一个大小为size的内部的缓冲数组被建立存储在buf
     *
     * @param   in     the underlying input stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if {@code size <= 0}.
     */
    public BufferedInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    /**
     * Fills the buffer with more data, taking into account
     * shuffling and other tricks for dealing with marks.
     * Assumes that it is being called by a synchronized method.
     * This method also assumes that all data has already been read in,
     * hence pos > count.
     * 用更多的数据填充缓冲区，考虑到慢慢移动和其他处理标记的花招。假设这个方法被一个同步的方法调用，同时假设所有数据都被读入所以pos>count
     */
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if (markpos < 0)
            pos = 0;            /* no mark: throw away the buffer 没有标记丢弃缓冲区*/
        else if (pos >= buffer.length)  /* no room left in buffer 缓冲区没有空间剩余*/
            if (markpos > 0) {  /* can throw away early part of the buffer 丢弃缓冲区早期部分*/
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);//被丢弃的是从0到markpos之间的数据
                pos = sz;
                markpos = 0;//标记位置设为0
            } else if (buffer.length >= marklimit) {
                markpos = -1;   /* buffer got too big, invalidate mark buffer太大，无效标记缓冲区*/
                pos = 0;        /* drop buffer contents 丢弃缓冲区内容*/
            } else if (buffer.length >= MAX_BUFFER_SIZE) {
                throw new OutOfMemoryError("Required array size too large");
            } else {            /* grow buffer buffer增长*/
                int nsz = (pos <= MAX_BUFFER_SIZE - pos) ?
                        pos * 2 : MAX_BUFFER_SIZE;//pos大小乘以2除非超过最大上限
                if (nsz > marklimit)
                    nsz = marklimit;//增长后的大小不能超过marklimit
                byte nbuf[] = new byte[nsz];
                System.arraycopy(buffer, 0, nbuf, 0, pos);//将原buffer中的内容复制到新的数组中
                if (!bufUpdater.compareAndSet(this, buffer, nbuf)) {
                    // Can't replace buf if there was an async close.如果有一个异步关闭，不能替换
                    // Note: This would need to be changed if fill()
                    // is ever made accessible to multiple threads.如果fill()曾被设置为多线程可进入，这里需要改变
                    // But for now, the only way CAS can fail is via close.
                    // assert buf == null;但是现在，CAS唯一失败的原因是关闭，断言buf==null
                    throw new IOException("Stream closed");
                }
                buffer = nbuf;
            }
        count = pos;//从开始pos到count都是有效字符
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos);//从输入流中读取数据填满缓冲区
        if (n > 0)
            count = n + pos;//读取到数据则增加count
    }

    /**
     * See
     * the general contract of the <code>read</code>
     * method of <code>InputStream</code>.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     * @see        java.io.FilterInputStream#in
     */
    public synchronized int read() throws IOException {
        if (pos >= count) {//pos>=count说明缓冲区内没有可读取的数据，需要从流中读取数据到缓冲区
            fill();
            if (pos >= count)//流中也没有数据可读取了
                return -1;
        }
        return getBufIfOpen()[pos++] & 0xff;
    }

    /**
     * Read characters into a portion of an array, reading from the underlying
     * stream at most once if necessary.
     * 将字符读入到数组的一部分，如果需要的话最多从下层输入流读取一次
     */
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;//缓冲区内有效的字节数
        if (avail <= 0) {//缓冲区内没有数据了
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, do not bother to copy the
               bytes into the local buffer.  In this way buffered streams will
               cascade harmlessly. 如果需要的长度至少跟buffer一样大，并且没有mark/reset活动，不会打扰复制字节到本地缓冲区。这样缓冲流是级联无害的*/
            if (len >= getBufIfOpen().length && markpos < 0) {//流超过了缓冲区的数组长度且不存在标记
                return getInIfOpen().read(b, off, len);//从输入流中直接读取字节复制到数组b
            }
            fill();//从输入流中读取数据到缓冲区，再缓冲区数据被全部读取到b中且未超过marklimit的时候，会扩大缓冲区
            avail = count - pos;
            if (avail <= 0) return -1;//输入流中也没有数据了，返回-1
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);//将缓冲区内的字节复制到数组b，数据量为len和缓冲区内剩余字节数的较小值
        pos += cnt;
        return cnt;//返回读取到数组b的字节数
    }

    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     * 从这个字节输入流读取字节到指定的字节数组中，从给出的偏移量开始
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
     * the <code>{@link InputStream}</code> class.  As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the <code>read</code> method of the underlying stream.  This
     * iterated <code>read</code> continues until one of the following
     * conditions becomes true: <ul>
     *
     *   <li> The specified number of bytes have been read,
     *
     *   <li> The <code>read</code> method of the underlying stream returns
     *   <code>-1</code>, indicating end-of-file, or
     *
     *   <li> The <code>available</code> method of the underlying stream
     *   returns zero, indicating that further input requests would block.
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of bytes
     * actually read.
     * 这个方法实现了InputStream.read(byte[], int, int)抽象方法。作为额外的便利，这个方法尝试读取尽可能多的字节，通过重复调用下层输入流的read方法。
     * 这个重复的read会一直持续直到以下几种情况：读取到了指定数量的字节；下层输入流read方法返回-1说明到达文件末尾；下层输入流的available方法返回0说明后续的读取请求会被阻塞。
     * 如果下层输入流的第一次read方法返回-1说明到达文件末尾，这个方法会返回-1，否则返回实际读取的字节数。
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many bytes as possible in the same fashion.
     * 这个类的子类是被鼓励的，但是不是必须的去尝试一同样的类型读取尽可能多的字节
     *
     * @param      b     destination buffer.
     * @param      off   offset at which to start storing bytes.
     * @param      len   maximum number of bytes to read.
     * @return     the number of bytes read, or <code>-1</code> if the end of
     *             the stream has been reached.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     */
    public synchronized int read(byte b[], int off, int len)
        throws IOException
    {
        getBufIfOpen(); // 检查流有没有关闭
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {//这里面有一个负数则或计算后得到负数
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (;;) {
            int nread = read1(b, off + n, len - n);
            if (nread <= 0)
                return (n == 0) ? nread : n;//什么都没读取到时返回-1
            n += nread;
            if (n >= len)
                return n;//达到需要读取的字节数，返回实际读取到的字节，正常情况下等于len
            // 如果没有关闭但是没有可读取的字节，返回
            InputStream input = in;
            if (input != null && input.available() <= 0)//下层输入流没有可以读取的字节
                return n;//返回读取到的字节数，该值小于len
        }
    }

    /**
     * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     *
     * @exception  IOException  if the stream does not support seek,
     *                          or if this input stream has been closed by
     *                          invoking its {@link #close()} method, or an
     *                          I/O error occurs.
     */
    public synchronized long skip(long n) throws IOException {
        getBufIfOpen(); // 检查流是否关闭
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {
            // 如果没有标记则不再保留buffer
            if (markpos <0)
                return getInIfOpen().skip(n);//调用了下层输入流的skip方法

            // 填充缓冲区，保存用于reset的字节
            fill();
            avail = count - pos;
            if (avail <= 0)
                return 0;
        }

        long skipped = (avail < n) ? avail : n;//跳过的字节数最大不能超过缓冲区的可填充字节数
        pos += skipped;//通过读取到缓冲区并设置pos的位置来skip
        return skipped;//返回实际跳过的字节
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * 返回从输入流中不需要因为下一个操作阻塞的可以读取或者跳过的字节数的估计值。
     * 下一个操作可能由这个线程或者其他线程来调用。一个单位的读取或者跳过操作不会阻塞，但是可能读取或跳过更少的字节。
     * <p>
     * This method returns the sum of the number of bytes remaining to be read in
     * the buffer (<code>count&nbsp;- pos</code>) and the result of calling the
     * {@link java.io.FilterInputStream#in in}.available().
     * 这个方法返回缓冲区内剩余可读取的字节数count-pos加上FilterInputStream.available()
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     */
    public synchronized int available() throws IOException {
        int n = count - pos;
        int avail = getInIfOpen().available();
        return n > (Integer.MAX_VALUE - avail)
                    ? Integer.MAX_VALUE
                    : n + avail;//不超过Interger范围时，返回值是缓冲区的有效数据加上输入流中的有效数据量
    }

    /**
     * See the general contract of the <code>mark</code>
     * method of <code>InputStream</code>.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.BufferedInputStream#reset()
     */
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;//marklimit设为给出的参数
        markpos = pos;//标记当前位置
    }

    /**
     * See the general contract of the <code>reset</code>
     * method of <code>InputStream</code>.
     * <p>
     * If <code>markpos</code> is <code>-1</code>
     * (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code>
     * is thrown. Otherwise, <code>pos</code> is
     * set equal to <code>markpos</code>.
     * markpos是-1时没有标记，抛出IOException。否则pos=markpos
     *
     * @exception  IOException  if this stream has not been marked or,
     *                  if the mark has been invalidated, or the stream
     *                  has been closed by invoking its {@link #close()}
     *                  method, or an I/O error occurs.
     * @see        java.io.BufferedInputStream#mark(int)
     */
    public synchronized void reset() throws IOException {
        getBufIfOpen(); // 如果关闭了会引起异常
        if (markpos < 0)
            throw new IOException("Resetting to invalid mark");
        pos = markpos;
    }

    /**
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods. The <code>markSupported</code>
     * method of <code>BufferedInputStream</code> returns
     * <code>true</code>.
     * 检查这个输入流是否支持mark/reset操作，BufferedInputStream总是返回true
     *
     * @return  a <code>boolean</code> indicating if this stream type supports
     *          the <code>mark</code> and <code>reset</code> methods.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * Once the stream has been closed, further read(), available(), reset(),
     * or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     * 关闭这个输入流并释放任何关联的系统资源。一旦流被关闭，后面的read(), available(), reset(), skip()
     * 调用都会抛出IOException。关闭一个已经关闭的流没有作用
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        byte[] buffer;
        while ( (buffer = buf) != null) {
            if (bufUpdater.compareAndSet(this, buffer, null)) {//将buf设为null
                InputStream input = in;
                in = null;//将in设为null
                if (input != null)
                    input.close();//关闭下层输入流
                return;
            }
            // Else retry in case a new buf was CASed in fill()或者一个新的buf在fill()中更新时重试
        }
    }
}
