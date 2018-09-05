/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Writes text to a character-output stream, buffering characters so as to
 * provide for the efficient writing of single characters, arrays, and strings.
 * 将文本写到字符输出流，缓冲字符来提供单个字符、数组、字符串的高效写入
 *
 * <p> The buffer size may be specified, or the default size may be accepted.
 * The default is large enough for most purposes.
 * 缓冲区的大小可能是指定的，或者默认大小8K，默认大小对于大多数情况足够大。
 *
 * <p> A newLine() method is provided, which uses the platform's own notion of
 * line separator as defined by the system property <tt>line.separator</tt>.
 * Not all platforms use the newline character ('\n') to terminate lines.
 * Calling this method to terminate each output line is therefore preferred to
 * writing a newline character directly.
 * newLine()方法使用平台定义的行分隔符line.separator。不是所有平台使用'\n'来表示行结束。
 * 调用这个方法来结束每个输入行比直接写一个'\n'字符更好。
 *
 * <p> In general, a Writer sends its output immediately to the underlying
 * character or byte stream.  Unless prompt output is required, it is advisable
 * to wrap a BufferedWriter around any Writer whose write() operations may be
 * costly, such as FileWriters and OutputStreamWriters.  For example,
 * 一般来说，一个Writer发送它的输出直接到下层的字符流或者字节流。除非要求立刻输出，否则建议用BufferedWriter
 * 来包装一个write()操作花费大的Writer，比如FileWriters和OutputStreamWriters。例如，
 *
 * <pre>
 * PrintWriter out
 *   = new PrintWriter(new BufferedWriter(new FileWriter("foo.out")));
 * </pre>
 *
 * will buffer the PrintWriter's output to the file.  Without buffering, each
 * invocation of a print() method would cause characters to be converted into
 * bytes that would then be written immediately to the file, which can be very
 * inefficient.
 * 会缓冲PrintWriter到一个文件的输出。没有缓冲，每一次调用print()方法会引起字符被转换成字节然后直接写入到文件，这样效率低。
 *
 * @see PrintWriter
 * @see FileWriter
 * @see OutputStreamWriter
 * @see java.nio.file.Files#newBufferedWriter
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public class BufferedWriter extends Writer {
	/**
	 * 下层输出流
	 */
    private Writer out;
    /**
     * 缓冲区
     */
    private char cb[];
    /**
     * 缓冲区大小
     */
    private int nChars;
    /**
     * 下一个有效字符
     */
    private int nextChar;

    private static int defaultCharBufferSize = 8192;

    /**
     * Line separator string.  This is the value of the line.separator
     * property at the moment that the stream was created.
     * 行分隔符，在这个流创建时行分隔符的值
     */
    private String lineSeparator;

    /**
     * Creates a buffered character-output stream that uses a default-sized
     * output buffer.
     * 创建一个使用默认大小8K输出缓冲区的缓冲字符输出流
     *
     * @param  out  A Writer
     */
    public BufferedWriter(Writer out) {
        this(out, defaultCharBufferSize);
    }

    /**
     * Creates a new buffered character-output stream that uses an output
     * buffer of the given size.
     * 创建一个使用指定大小sz输出缓冲区的缓冲字符输出流
     *
     * @param  out  A Writer
     * @param  sz   Output-buffer size, a positive integer
     *
     * @exception  IllegalArgumentException  If {@code sz <= 0}
     */
    public BufferedWriter(Writer out, int sz) {
        super(out);
        if (sz <= 0)
            throw new IllegalArgumentException("Buffer size <= 0");
        this.out = out;
        cb = new char[sz];
        nChars = sz;
        nextChar = 0;

        lineSeparator = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("line.separator"));
    }

    /** 确认这个流是否被关闭 */
    private void ensureOpen() throws IOException {
        if (out == null)
            throw new IOException("Stream closed");
    }

    /**
     * Flushes the output buffer to the underlying character stream, without
     * flushing the stream itself.  This method is non-private only so that it
     * may be invoked by PrintStream.
     * 将这个输出缓冲区刷新到下层字符流中，但不刷新下层流本身。这个类不是private，所以可以被PrintStream调用
     */
    void flushBuffer() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (nextChar == 0)
                return;
            out.write(cb, 0, nextChar);//下层输出流输出缓冲区内的所有有效字符
            nextChar = 0;
        }
    }

    /**
     * Writes a single character.
     * 写入单个字符
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(int c) throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (nextChar >= nChars)
                flushBuffer();//如果缓冲区已满，将缓冲区内的字符写入到下层输出流
            cb[nextChar++] = (char) c;//将c复制到数组中
        }
    }

    /**
     * Our own little min method, to avoid loading java.lang.Math if we've run
     * out of file descriptors and we're trying to print a stack trace.
     * 简单的min方法，避免加载java.lang.Math在用尽文件描述符和尝试打印堆栈追踪时
     */
    private int min(int a, int b) {
        if (a < b) return a;
        return b;
    }

    /**
     * Writes a portion of an array of characters.
     * 写入一个字符数组的一部分
     *
     * <p> Ordinarily this method stores characters from the given array into
     * this stream's buffer, flushing the buffer to the underlying stream as
     * needed.  If the requested length is at least as large as the buffer,
     * however, then this method will flush the buffer and write the characters
     * directly to the underlying stream.  Thus redundant
     * <code>BufferedWriter</code>s will not copy data unnecessarily.
     * 一般来说这个方法从数组存储字符到流的缓冲区，如果需要的话刷新缓冲区到下层输出流。
     * 如果请求的长度超过了缓冲区大小，这个方法会刷新缓冲区并将字符直接写入到下层输出流。
     * 因此多余的BufferedWriter不会不必要的复制数据。
     *
     * @param  cbuf  A character array
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }

            if (len >= nChars) {
                /* If the request length exceeds the size of the output buffer,
                   flush the buffer and then write the data directly.  In this
                   way buffered streams will cascade harmlessly.
                   如果请求长度超过了输出缓冲区大小，刷新缓冲区直接写数据。这样缓冲输出流是级联无害的 */
                flushBuffer();
                out.write(cbuf, off, len);//直接使用下层输出流的write方法
                return;
            }

            int b = off, t = off + len;
            while (b < t) {
                int d = min(nChars - nextChar, t - b);
                System.arraycopy(cbuf, b, cb, nextChar, d);//复制字符到缓冲区，长度为缓冲区剩余空间和剩余要输入字符的较小值
                b += d;
                nextChar += d;
                if (nextChar >= nChars)
                    flushBuffer();//缓冲区满了时刷新
            }
        }
    }

    /**
     * Writes a portion of a String.
     * 写一个字符串的一部分
     *
     * <p> If the value of the <tt>len</tt> parameter is negative then no
     * characters are written.  This is contrary to the specification of this
     * method in the {@linkplain java.io.Writer#write(java.lang.String,int,int)
     * superclass}, which requires that an {@link IndexOutOfBoundsException} be
     * thrown.
     * 如果len参数是负数，没有字符会被写入。这个和java.io.Writer.write(java.lang.String,int,int)
     * 超类是相反的，它要求抛出IndexOutOfBoundsException
     *
     * @param  s     String to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(String s, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();

            int b = off, t = off + len;
            while (b < t) {
                int d = min(nChars - nextChar, t - b);
                s.getChars(b, b + d, cb, nextChar);
                b += d;
                nextChar += d;
                if (nextChar >= nChars)
                    flushBuffer();
            }
        }
    }

    /**
     * Writes a line separator.  The line separator string is defined by the
     * system property <tt>line.separator</tt>, and is not necessarily a single
     * newline ('\n') character.
     * 写一个行分隔符，行分隔符通过系统属性line.separator来定义，不一定是一个'\n'字符
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void newLine() throws IOException {
        write(lineSeparator);
    }

    /**
     * Flushes the stream.
     * 刷新流
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
        synchronized (lock) {
            flushBuffer();
            out.flush();//下层输入流的刷新在这里调用
        }
    }

    @SuppressWarnings("try")
    public void close() throws IOException {
        synchronized (lock) {
            if (out == null) {//out为null说明已经被关闭了
                return;
            }
            try (Writer w = out) {//out随着这个代码块结束而关闭
                flushBuffer();//先做一次刷新，避免有没有输入到下层输出流的数据
            } finally {
                out = null;
                cb = null;//缓冲区被移除
            }
        }
    }
}
