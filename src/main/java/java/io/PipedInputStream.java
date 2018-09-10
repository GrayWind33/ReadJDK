/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A piped input stream should be connected
 * to a piped output stream; the piped  input
 * stream then provides whatever data bytes
 * are written to the piped output  stream.
 * Typically, data is read from a <code>PipedInputStream</code>
 * object by one thread  and data is written
 * to the corresponding <code>PipedOutputStream</code>
 * by some  other thread. Attempting to use
 * both objects from a single thread is not
 * recommended, as it may deadlock the thread.
 * The piped input stream contains a buffer,
 * decoupling read operations from write operations,
 * within limits.
 * A pipe is said to be <a name="BROKEN"> <i>broken</i> </a> if a
 * thread that was providing data bytes to the connected
 * piped output stream is no longer alive.
 * 一个管道输入流需要连接一个管道输出流，管道输入流会提供任何数据字节写入管道输出流。典型地，数据被一个线程从一个PipedInputStream对象中读取，然后数据被另一个线程写到对应的PipedOutputStream。
 * 试图由一个线程使用所有对象是不推荐的，这可能导致线程死锁。管道输入流含有一个缓冲区来解耦一个读操作和写操作。一个管道如果提供数据给相连接的管道输出流的线程不再活动，这个管道状态被称为BROKEN
 *
 * @author  James Gosling
 * @see     java.io.PipedOutputStream
 * @since   JDK1.0
 */
public class PipedInputStream extends InputStream {
    /**
     * 输出流关闭，仅PipedOutputStream.close()调用receivedLast()方法会将它置为true
     */
	boolean closedByWriter = false;
	/**
	 * 输入流关闭，仅PipedInputStream.close()会将它置为true
	 */
    volatile boolean closedByReader = false;
    /**
     * 是否有一对输入输出流相互连接
     */
    boolean connected = false;

        /* REMIND: identification of the read and write sides needs to be
           more sophisticated.  Either using thread groups (but what about
           pipes within a thread?) or using finalization (but it may be a
           long time until the next GC).
           识别读和写两边需要更加复杂。使用线程组（但是管道在一个线程中怎么办）或者使用final化（但是可能到下一次GC的时间更长） */
    Thread readSide;//input流的线程
    Thread writeSide;//output流的线程

    private static final int DEFAULT_PIPE_SIZE = 1024;

    /**
     * The default size of the pipe's circular input buffer.
     * 管道循环输入缓冲区默认大小1K
     * @since   JDK1.1
     */
    // This used to be a constant before the pipe size was allowed
    // to change. This field will continue to be maintained
    // for backward compatibility.这个值在管道大小允许改变前被用作常数。这个值会为了向下兼容性被持续保持
    protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;

    /**
     * The circular buffer into which incoming data is placed.
     * 循环缓冲区，接下来的数据会被放入其中
     * @since   JDK1.1
     */
    protected byte buffer[];

    /**
     * The index of the position in the circular buffer at which the
     * next byte of data will be stored when received from the connected
     * piped output stream. <code>in&lt;0</code> implies the buffer is empty,
     * <code>in==out</code> implies the buffer is full
     * 循环缓冲区中下一个从连接的管道输出流接收的字节数据将会被存储的位置下标。in<0说明缓冲区是空的，in==out说明缓冲区满了
     * @since   JDK1.1
     */
    protected int in = -1;

    /**
     * The index of the position in the circular buffer at which the next
     * byte of data will be read by this piped input stream.
     * 循环缓冲区中下一个将会被这个管道输入流读取的字节下标位置。
     * @since   JDK1.1
     */
    protected int out = 0;

    /**
     * Creates a <code>PipedInputStream</code> so
     * that it is connected to the piped output
     * stream <code>src</code>. Data bytes written
     * to <code>src</code> will then be  available
     * as input from this stream.
     * 创建一个PipedInputStream连接到管道输出流src。数据字节写入到src中将会成为这个流的输入。
     *
     * @param      src   the stream to connect to.
     * @exception  IOException  if an I/O error occurs.
     */
    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Creates a <code>PipedInputStream</code> so that it is
     * connected to the piped output stream
     * <code>src</code> and uses the specified pipe size for
     * the pipe's buffer.
     * Data bytes written to <code>src</code> will then
     * be available as input from this stream.
     * 跟上面相比这个管道缓冲区的大小是指定的，其他相同
     *
     * @param      src   the stream to connect to.
     * @param      pipeSize the size of the pipe's buffer.
     * @exception  IOException  if an I/O error occurs.
     * @exception  IllegalArgumentException if {@code pipeSize <= 0}.
     * @since      1.6
     */
    public PipedInputStream(PipedOutputStream src, int pipeSize)
            throws IOException {
         initPipe(pipeSize);
         connect(src);
    }

    /**
     * Creates a <code>PipedInputStream</code> so
     * that it is not yet {@linkplain #connect(java.io.PipedOutputStream)
     * connected}.
     * It must be {@linkplain java.io.PipedOutputStream#connect(
     * java.io.PipedInputStream) connected} to a
     * <code>PipedOutputStream</code> before being used.
     * 创建一个还没有连接的PipedInputStream，在使用前必须连接到一个PipedOutputStream
     */
    public PipedInputStream() {
        initPipe(DEFAULT_PIPE_SIZE);
    }

    /**
     * Creates a <code>PipedInputStream</code> so that it is not yet
     * {@linkplain #connect(java.io.PipedOutputStream) connected} and
     * uses the specified pipe size for the pipe's buffer.
     * It must be {@linkplain java.io.PipedOutputStream#connect(
     * java.io.PipedInputStream)
     * connected} to a <code>PipedOutputStream</code> before being used.
     * 创建一个还没有连接的PipedInputStream指定它的缓冲区大小，在使用前必须连接到一个PipedOutputStream
     *
     * @param      pipeSize the size of the pipe's buffer.
     * @exception  IllegalArgumentException if {@code pipeSize <= 0}.
     * @since      1.6
     */
    public PipedInputStream(int pipeSize) {
        initPipe(pipeSize);
    }

    private void initPipe(int pipeSize) {
         if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
         }
         buffer = new byte[pipeSize];
    }

    /**
     * Causes this piped input stream to be connected
     * to the piped  output stream <code>src</code>.
     * If this object is already connected to some
     * other piped output  stream, an <code>IOException</code>
     * is thrown.
     * 引起这个管道输入流连接到管道输出流src。如果这个对象已经连接到某个其他的管道输出流会抛出IOException。
     * <p>
     * If <code>src</code> is an
     * unconnected piped output stream and <code>snk</code>
     * is an unconnected piped input stream, they
     * may be connected by either the call:
     *
     * <pre><code>snk.connect(src)</code> </pre>
     * <p>
     * or the call:
     *
     * <pre><code>src.connect(snk)</code> </pre>
     * <p>
     * The two calls have the same effect.
     * 如果src是一个未连接的管道输出流，snk是一个未连接的管道输入流，它们可以通过snk.connect(src)或者src.connect(snk)连接，两者效果相同。
     *
     * @param      src   The piped output stream to connect to.
     * @exception  IOException  if an I/O error occurs.
     */
    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }

    /**
     * Receives a byte of data.  This method will block if no input is
     * available.
     * 接收一字节数据，这个方法如果没有有效输入时会阻塞。
     * @param b the byte being received
     * @exception IOException If the pipe is <a href="#BROKEN"> <code>broken</code></a>,
     *          {@link #connect(java.io.PipedOutputStream) unconnected},
     *          closed, or if an I/O error occurs.
     * @since     JDK1.1
     */
    protected synchronized void receive(int b) throws IOException {
        checkStateForReceive();//检查管道状态
        writeSide = Thread.currentThread();//写线程设为当前线程
        if (in == out)
            awaitSpace();//缓冲区满了，通知所有线程使得读取端读取字节给当前流缓冲区空出位置来接收
        if (in < 0) {//in<0表示当前缓冲区为空
            in = 0;
            out = 0;
        }
        buffer[in++] = (byte)(b & 0xFF);//将b存储到缓冲区中
        if (in >= buffer.length) {
            in = 0;//因为是循环缓冲区，所以in超过buffer.length时重新回到0的位置
        }
    }

    /**
     * Receives data into an array of bytes.  This method will
     * block until some input is available.
     * 将数据接收到一个字节数组中，这个方法会阻塞到一些输入变得有效。
     * @param b the buffer into which the data is received
     * @param off the start offset of the data
     * @param len the maximum number of bytes received
     * @exception IOException If the pipe is <a href="#BROKEN"> broken</a>,
     *           {@link #connect(java.io.PipedOutputStream) unconnected},
     *           closed,or if an I/O error occurs.
     */
    synchronized void receive(byte b[], int off, int len)  throws IOException {
        checkStateForReceive();//检查管道状态
        writeSide = Thread.currentThread();//写线程设为当前线程
        int bytesToTransfer = len;
        while (bytesToTransfer > 0) {//还有需要写入的字节
            if (in == out)
                awaitSpace();//缓冲区满了，通知其他线程读取字节空出空间
            int nextTransferAmount = 0;
            if (out < in) {//out<in说明out到in这段是未读取的数据，所以空余空间是buffer.length-in
                nextTransferAmount = buffer.length - in;
            } else if (in < out) {
                if (in == -1) {
                	//当前缓冲区为空，可用空间为buffer.length
                    in = out = 0;
                    nextTransferAmount = buffer.length - in;
                } else {
                	//in已经到达一次右边界重置为0之后in<out，out到右边界是未读取内容，in不能超过out的值，所以空余空间为out-in
                    nextTransferAmount = out - in;
                }
            }
            if (nextTransferAmount > bytesToTransfer)
                nextTransferAmount = bytesToTransfer;//要读取的字节数是剩余空间和参数指定长度间的较小值
            assert(nextTransferAmount > 0);
            System.arraycopy(b, off, buffer, in, nextTransferAmount);//将接收的字节复制到缓冲区in开始的位置
            bytesToTransfer -= nextTransferAmount;
            off += nextTransferAmount;
            in += nextTransferAmount;//in增加读取的字节数
            if (in >= buffer.length) {
                in = 0;//in到达缓冲区边界时重置为0
            }
        }
    }

    /**
     * 确定当前有连接，管道两端都没有被关闭，有活动的读取端线程
     * @throws IOException
     */
    private void checkStateForReceive() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByWriter || closedByReader) {
            throw new IOException("Pipe closed");
        } else if (readSide != null && !readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }

    private void awaitSpace() throws IOException {
        while (in == out) {
            checkStateForReceive();

            /* full: kick any waiting readers 满了的时候唤醒所有的等待读取线程*/
            notifyAll();//唤醒所有线程，使得有reader读取字节将当前流缓冲区空出来
            try {
                wait(1000);//当前线程等待1s
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
    }

    /**
     * Notifies all waiting threads that the last byte of data has been
     * received.
     * 通知所有等待的线程最后一个字节数据已经被接收
     */
    synchronized void receivedLast() {
        closedByWriter = true;
        notifyAll();
    }

    /**
     * Reads the next byte of data from this piped input stream. The
     * value byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>.
     * This method blocks until input data is available, the end of the
     * stream is detected, or an exception is thrown.
     * 从这个管道输入流读取下一个字节。返回值作为一个整数范围在0-255之间。这个方法一直阻塞到输入数据变得有效，或者探知到流结束或者抛出异常。
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if the pipe is
     *           {@link #connect(java.io.PipedOutputStream) unconnected},
     *           <a href="#BROKEN"> <code>broken</code></a>, closed,
     *           or if an I/O error occurs.
     */
    public synchronized int read()  throws IOException {
        //存在连接，读取端没有关闭，缓存区为空时写入端线程必须活动
    	if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByReader) {
            throw new IOException("Pipe closed");
        } else if (writeSide != null && !writeSide.isAlive()
                   && !closedByWriter && (in < 0)) {
            throw new IOException("Write end dead");
        }

        readSide = Thread.currentThread();//读取端线程设为当前线程
        int trials = 2;//加起来等待2s，第三次循环缓冲区依然为空没有写入数据则认为管道BROKEN
        while (in < 0) {
            if (closedByWriter) {
                /* 输出管道被writer关闭返回-1 */
                return -1;
            }
            if ((writeSide != null) && (!writeSide.isAlive()) && (--trials < 0)) {
                throw new IOException("Pipe broken");
            }
            /* 可能有一个writer在等待 */
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
        int ret = buffer[out++] & 0xFF;//读取out位置的字节并增加out
        if (out >= buffer.length) {
            out = 0;//如果out到达buffer右边界，将它重置为0
        }
        if (in == out) {
            /* in==out说明缓冲区空了 */
            in = -1;
        }

        return ret;
    }

    /**
     * Reads up to <code>len</code> bytes of data from this piped input
     * stream into an array of bytes. Less than <code>len</code> bytes
     * will be read if the end of the data stream is reached or if
     * <code>len</code> exceeds the pipe's buffer size.
     * If <code>len </code> is zero, then no bytes are read and 0 is returned;
     * otherwise, the method blocks until at least 1 byte of input is
     * available, end of the stream has been detected, or an exception is
     * thrown.
     * 从这个管道输入流读取最大len程度的字节数据到字节数组中。如果到达了数据量末端或者len超过了管道缓冲区大小，少于len字节的数据会被读取。
     * 如果len是0，没有数据会被读取返回0；否则这个方法会阻塞直到至少1字节输入是有效的，或者到达了流末端或者抛出异常。
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in the destination array <code>b</code>
     * @param      len   the maximum number of bytes read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     * @exception  NullPointerException If <code>b</code> is <code>null</code>.
     * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
     * <code>len</code> is negative, or <code>len</code> is greater than
     * <code>b.length - off</code>
     * @exception  IOException if the pipe is <a href="#BROKEN"> <code>broken</code></a>,
     *           {@link #connect(java.io.PipedOutputStream) unconnected},
     *           closed, or if an I/O error occurs.
     */
    public synchronized int read(byte b[], int off, int len)  throws IOException {
    	//存在连接，读取端没有关闭，缓存区为空时写入端线程必须活动
    	if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        /* 可能等待第一个字节 */
        int c = read();//尝试读取一个字节
        if (c < 0) {
            return -1;//没有读取到直接返回-1
        }
        b[off] = (byte) c;//将读取到的字节存入数组
        int rlen = 1;
        while ((in >= 0) && (len > 1)) {

            int available;

            if (in > out) {
                available = Math.min((buffer.length - out), (in - out));//in>out则可读取的数据是in-out，正常情况下in不能超过buffer.length
            } else {
                available = buffer.length - out;//in<out则可读取数据是out到buffer右边界，一次循环只能读取到右边界，从头开始的部分要下一次循环读取
            }

            // 在循环外事先读取的字节
            if (available > (len - 1)) {
                available = len - 1;//读取的字节不能超过参数指定的数量-1因为循环外已经读取了一个字节
            }
            System.arraycopy(buffer, out, b, off + rlen, available);//复制可读取字节
            out += available;//out位置移动读取的字节数
            rlen += available;//目标位置移动
            len -= available;//待读取长度减少

            if (out >= buffer.length) {
                out = 0;
            }
            if (in == out) {
                /* in==out缓冲区为空，将in置为-1，所以循环会跳出 */
                in = -1;
            }
        }
        return rlen;
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     * 返回从这个输入流可以读取多少字节不用阻塞
     *
     * @return the number of bytes that can be read from this input stream
     *         without blocking, or {@code 0} if this input stream has been
     *         closed by invoking its {@link #close()} method, or if the pipe
     *         is {@link #connect(java.io.PipedOutputStream) unconnected}, or
     *          <a href="#BROKEN"> <code>broken</code></a>.
     *
     * @exception  IOException  if an I/O error occurs.
     * @since   JDK1.0.2
     */
    public synchronized int available() throws IOException {
        if(in < 0)
            return 0;
        else if(in == out)
            return buffer.length;//因为在接收和读取时没有可读取的字节会把in置为-1，所以in==out是缓冲区满了的状态
        else if (in > out)
            return in - out;//in>out时可读取内容是in到out之间
        else
            return in + buffer.length - out;//in<out时是out到buffer右边界再加上buffer左边界到in的内容
    }

    /**
     * Closes this piped input stream and releases any system resources
     * associated with the stream.
     * 关闭管道输入流，释放任何和这个流关联的资源
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close()  throws IOException {
        closedByReader = true;//输入流关闭
        synchronized (this) {
            in = -1;//缓冲区所有数据失效
        }
    }
}
