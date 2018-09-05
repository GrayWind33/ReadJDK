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


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads text from a character-input stream, buffering characters so as to
 * provide for the efficient reading of characters, arrays, and lines.
 * 从字符输入流读取文本，缓冲字符来提供高效地读取字符、数组和行。
 *
 * <p> The buffer size may be specified, or the default size may be used.  The
 * default is large enough for most purposes.
 * 缓冲区的大小可能是指定的或者是默认大小，默认大小对于大部分情况足够大了。
 *
 * <p> In general, each read request made of a Reader causes a corresponding
 * read request to be made of the underlying character or byte stream.  It is
 * therefore advisable to wrap a BufferedReader around any Reader whose read()
 * operations may be costly, such as FileReaders and InputStreamReaders.  For
 * example,
 * 一般来说，Reader的每一个读取请求引起一个下层的字节或者字符输入流的读取请求。所以建议用BufferedReader
 * 来包裹一个read方法花费大的Reader，比如FileReaders和InputStreamReaders
 *
 * <pre>
 * BufferedReader in
 *   = new BufferedReader(new FileReader("foo.in"));
 * </pre>
 *
 * will buffer the input from the specified file.  Without buffering, each
 * invocation of read() or readLine() could cause bytes to be read from the
 * file, converted into characters, and then returned, which can be very
 * inefficient.
 * 将会缓存从特定文件来的输入。没有缓冲，每一次调用read()或者readLine()可能引起从文件读取字节，转换为字符后返回，这样效率不高。
 *
 * <p> Programs that use DataInputStreams for textual input can be localized by
 * replacing each DataInputStream with an appropriate BufferedReader.
 * 使用DataInputStreams进行文本输入的程序可以通过替换每一个DataInputStream为合适的BufferedReader进行局部化
 *
 * @see FileReader
 * @see InputStreamReader
 * @see java.nio.file.Files#newBufferedReader
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public class BufferedReader extends Reader {
	/**
	 * 下层输入流
	 */
    private Reader in;
    /**
     * 缓冲区
     */
    private char cb[];
    /**
     * 最后一个有效字符的位置
     */
    private int nChars;
    /**
     * 下一个要读取的字符位置
     */
    private int nextChar;

    private static final int INVALIDATED = -2;
    private static final int UNMARKED = -1;
    /**
     * 标记的位置，小于等于-1时代表不存在mark
     */
    private int markedChar = UNMARKED;
    private int readAheadLimit = 0; /* 只有markedChar > 0时是有效的 */

    /** 如果下一个字符是换行符，跳过它 */
    private boolean skipLF = false;

    /** 设定mark时skipLF的标记 */
    private boolean markedSkipLF = false;

    private static int defaultCharBufferSize = 8192;
    private static int defaultExpectedLineLength = 80;

    /**
     * Creates a buffering character-input stream that uses an input buffer of
     * the specified size.
     * 创建一个缓冲字符输入流使用一个指定大小的输入缓冲区
     *
     * @param  in   A Reader
     * @param  sz   Input-buffer size
     *
     * @exception  IllegalArgumentException  If {@code sz <= 0}
     */
    public BufferedReader(Reader in, int sz) {
        super(in);
        if (sz <= 0)
            throw new IllegalArgumentException("Buffer size <= 0");
        this.in = in;
        cb = new char[sz];
        nextChar = nChars = 0;
    }

    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     * 创建一个缓冲字符输入流使用一个默认大小8K的输入缓冲区
     *
     * @param  in   A Reader
     */
    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }

    /** 检查确认流没有被关闭 */
    private void ensureOpen() throws IOException {
        if (in == null)
            throw new IOException("Stream closed");
    }

    /**
     * Fills the input buffer, taking the mark into account if it is valid.
     * 填充输入缓冲区，如果mark是有效的需要将它加入考虑，只有在可被读取的字符被全部读取完后才会调用这个方法
     */
    private void fill() throws IOException {
        int dst;
        if (markedChar <= UNMARKED) {
            /* 没有mark */
            dst = 0;
        } else {
            /* 存在mark */
            int delta = nextChar - markedChar;
            if (delta >= readAheadLimit) {
                /* Gone past read-ahead limit: Invalidate mark
                 * 从mark开始到读取位置的字节数超过了限制，将markedChar设为-2使它失效 */
                markedChar = INVALIDATED;
                readAheadLimit = 0;
                dst = 0;
            } else {
                if (readAheadLimit <= cb.length) {//能够保留的最大mark部分长度超过了缓冲区大小
                    /* Shuffle in the current buffer在当前缓冲区中洗牌 */
                    System.arraycopy(cb, markedChar, cb, 0, delta);//将从markedChar开始的所有有效字符移到缓冲区头部
                    markedChar = 0;
                    dst = delta;
                } else {
                    /* Reallocate buffer to accommodate read-ahead limit
                     * 重新分配缓冲区的大小来满足预读要求 */
                    char ncb[] = new char[readAheadLimit];//新大小是readAheadLimit
                    System.arraycopy(cb, markedChar, ncb, 0, delta);//将有效字符复制到新的数组
                    cb = ncb;
                    markedChar = 0;//因为从标记位开始的有效字符被移动到了头上，所以标记位从0开始
                    dst = delta;
                }
                nextChar = nChars = delta;
            }
        }

        int n;
        do {
            n = in.read(cb, dst, cb.length - dst);//从下层输入流读取字符
        } while (n == 0);
        if (n > 0) {
            nChars = dst + n;
            nextChar = dst;
        }
    }

    /**
     * Reads a single character.
     * 读取单个字符
     *
     * @return The character read, as an integer in the range
     *         0 to 65535 (<tt>0x00-0xffff</tt>), or -1 if the
     *         end of the stream has been reached
     *         返回的是转为int的字符，返回是0-0xffff，因为存在两个字节的字符，到达文件末尾返回-1
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            for (;;) {
                if (nextChar >= nChars) {
                    fill();//读取到最后一个有效字符时，从输入流读取字符到缓冲区
                    if (nextChar >= nChars)
                        return -1;
                }
                if (skipLF) {//如果下一个字符是换行符要跳过
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                        continue;
                    }
                }
                return cb[nextChar++];//从缓冲区返回要读取的字符
            }
        }
    }

    /**
     * Reads characters into a portion of an array, reading from the underlying
     * stream if necessary.
     * 将字符读取到数组的一部分，如果需要的话从底层流读取
     */
    private int read1(char[] cbuf, int off, int len) throws IOException {
        if (nextChar >= nChars) {
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, and if line feeds are not
               being skipped, do not bother to copy the characters into the
               local buffer.  In this way buffered streams will cascade
               harmlessly.如果需要的长度超过了缓冲区大小，并且没有mark/reset活动，并且换行没有被跳过
               不要麻烦地将字符复制到本地缓冲区。这样可以避免没必要的损耗 */
            if (len >= cb.length && markedChar <= UNMARKED && !skipLF) {
                return in.read(cbuf, off, len);
            }
            fill();//读取到最后一个缓冲区内的有效字符时，从下层输入流读取字符到缓冲区
        }
        if (nextChar >= nChars) return -1;//下层输入流没有有效字符时返回-1
        if (skipLF) {
            skipLF = false;
            if (cb[nextChar] == '\n') {
                nextChar++;//下一个字符时换行符时跳过
                if (nextChar >= nChars)//如果跳过换行后有效字符不足则尝试从下层输入流中读取
                    fill();
                if (nextChar >= nChars)
                    return -1;
            }
        }
        int n = Math.min(len, nChars - nextChar);//读取的字符长度是len和缓冲区内有效字符的较小值
        System.arraycopy(cb, nextChar, cbuf, off, n);//从缓冲区复制字符到目标数组中
        nextChar += n;
        return n;//返回实际读取的字符数
    }

    /**
     * Reads characters into a portion of an array.
     * 读取字符到数组的一部分中
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link Reader#read(char[], int, int) read}</code> method of the
     * <code>{@link Reader}</code> class.  As an additional convenience, it
     * attempts to read as many characters as possible by repeatedly invoking
     * the <code>read</code> method of the underlying stream.  This iterated
     * <code>read</code> continues until one of the following conditions becomes
     * true: <ul>
     *
     *   <li> The specified number of characters have been read,
     *
     *   <li> The <code>read</code> method of the underlying stream returns
     *   <code>-1</code>, indicating end-of-file, or
     *
     *   <li> The <code>ready</code> method of the underlying stream
     *   returns <code>false</code>, indicating that further input requests
     *   would block.
     * 这个方法实现了Reader.read(char[], int, int)方法的一般约束。作为额外的便利，它会试图读取尽可能多的字符，通过重复调用下层输入流的read方法。
     * 这个重复的read会持续到一下一种情况为true：指定的字符数已经被读取，下层输入流的read方法返回-1说明到达了文件结束符，或者下层输入流的ready方法返回false说明后面的输入请求会阻塞
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of characters
     * actually read.
     * 如果第一个下层输入流的read方法返回-1，这个方法会返回-1.否则这个方法返回实际读取的字符数。
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many characters as possible in the same fashion.
     * 这个类的子类鼓励但不是必须去尝试用同样的方法读取尽可能多的字符
     *
     * <p> Ordinarily this method takes characters from this stream's character
     * buffer, filling it from the underlying stream as necessary.  If,
     * however, the buffer is empty, the mark is not valid, and the requested
     * length is at least as large as the buffer, then this method will read
     * characters directly from the underlying stream into the given array.
     * Thus redundant <code>BufferedReader</code>s will not copy data
     * unnecessarily.
     * 一般这个方法从这个输入流的字符缓冲区获取字符，如果需要的话从下层输入流来填充缓冲区。但是，如果缓冲区是空的，mark是无效的，请求的长度超过了缓冲区的长度，
     * 这个方法会从下层输入流直接读取字符到给定的数组。因此多余的BufferedReader不会进行不必要的复制。
     *
     * @param      cbuf  Destination buffer
     * @param      off   Offset at which to start storing characters
     * @param      len   Maximum number of characters to read
     *
     * @return     The number of characters read, or -1 if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {//同步操作
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int n = read1(cbuf, off, len);//从缓冲区读取数据到数组，如果调用时缓冲区就已经没有有效数据从下层输入流读取
            if (n <= 0) return n;
            while ((n < len) && in.ready()) {
                int n1 = read1(cbuf, off + n, len - n);//循环尝试从下层输入流读取数据到缓冲区
                if (n1 <= 0) break;//下层输入流没有数据时返回
                n += n1;
            }
            return n;
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a linefeed.
     * 读取一行文本。一行通过一个换行符或者一个回车或者一个回车跟着一个换行符来终止。
     *
     * @param      ignoreLF  If true, the next '\n' will be skipped为true时会跳过下一个'\n'
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached一个包含行内容但是不包含行终止字符的字符串，如果已经到达流结尾的话返回null
     *
     * @see        java.io.LineNumberReader#readLine()
     *
     * @exception  IOException  If an I/O error occurs
     */
    String readLine(boolean ignoreLF) throws IOException {
        StringBuffer s = null;
        int startChar;

        synchronized (lock) {
            ensureOpen();
            boolean omitLF = ignoreLF || skipLF;

        bufferLoop:
            for (;;) {

                if (nextChar >= nChars)
                    fill();//缓冲区内没有有效字符时，从下层输入流读取字符到缓冲区
                if (nextChar >= nChars) { /* 到达了EOF */
                    if (s != null && s.length() > 0)
                        return s.toString();//读取到了字符则返回字符串
                    else
                        return null;//没有读取到字符返回null
                }
                boolean eol = false;//缓冲区内有有效数据，还没有到达行结束
                char c = 0;
                int i;

                /* 如果有必要的话跳过一个 '\n'*/
                if (omitLF && (cb[nextChar] == '\n'))
                    nextChar++;
                skipLF = false;//只有第一个字符是'\n'时才会跳过
                omitLF = false;

            charLoop:
                for (i = nextChar; i < nChars; i++) {//读取范围不超过缓冲区内的所有有效字符
                    c = cb[i];
                    if ((c == '\n') || (c == '\r')) {
                        eol = true;//到达了行结束
                        break charLoop;//这里跳出了循环，所以nextChar依然在换行符或者回车的位置上
                    }
                }

                startChar = nextChar;
                nextChar = i;

                if (eol) {//到达了行结束
                    String str;
                    if (s == null) {//将新读取的内容添加到返回结果中
                        str = new String(cb, startChar, i - startChar);
                    } else {
                        s.append(cb, startChar, i - startChar);
                        str = s.toString();
                    }
                    nextChar++;//此时nextChar向前移动一位，经过了之前的换行符或回车
                    if (c == '\r') {//如果行结束标记是回车，跳过下一个换行符对应LRLF这种情况
                        skipLF = true;
                    }
                    return str;
                }

                if (s == null)
                    s = new StringBuffer(defaultExpectedLineLength);
                s.append(cb, startChar, i - startChar);//没有到达行结尾时，直接将新增内容添加到返回的字符串中
            }
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a linefeed.
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     *
     * @see java.nio.file.Files#readAllLines
     */
    public String readLine() throws IOException {
        return readLine(false);
    }

    /**
     * Skips characters.
     * 跳过字符
     *
     * @param  n  The number of characters to skip
     *
     * @return    The number of characters actually skipped
     *
     * @exception  IllegalArgumentException  If <code>n</code> is negative.
     * @exception  IOException  If an I/O error occurs
     */
    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (lock) {
            ensureOpen();
            long r = n;
            while (r > 0) {
                if (nextChar >= nChars)
                    fill();//缓冲区内没有有效字符时尝试从下层输入流读取字符到缓冲区
                if (nextChar >= nChars) /* 到达了EOF */
                    break;
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;//调用时第一个有效字符时'\n'时跳过它
                    }
                }
                long d = nChars - nextChar;//当前缓冲区内的有效字符个数
                if (r <= d) {//缓冲区内的有效字符数超过了要跳过的字符数，直接增加nextChar
                    nextChar += r;
                    r = 0;
                    break;
                }
                else {//缓冲区内数据不足时，nextChar移动到最后一个有效字符的位置
                    r -= d;
                    nextChar = nChars;
                }
            }
            return n - r;//返回跳过的字符数
        }
    }

    /**
     * Tells whether this stream is ready to be read.  A buffered character
     * stream is ready if the buffer is not empty, or if the underlying
     * character stream is ready.
     * 告知这个流是否准备完读取。一个字符缓冲流在缓冲区非空或者下层输入流准备完时是准备完成的状态。
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();

            /*
             * If newline needs to be skipped and the next char to be read
             * is a newline character, then just skip it right away.
             * 如果一个新行需要跳过并且下一个要读取的字符是新行的字符，立刻跳过它
             */
            if (skipLF) {
                /* Note that in.ready() will return true if and only if the next
                 * read on the stream will not block.
                 * in.ready()仅当流中的下一个读取不会被阻塞时会返回true
                 */
                if (nextChar >= nChars && in.ready()) {
                    fill();//缓冲区没有有效数据且下层输入流准备完时进行读取
                }
                if (nextChar < nChars) {
                    if (cb[nextChar] == '\n')//如果缓冲区内第一个有效字符是'\n'则跳过
                        nextChar++;
                    skipLF = false;
                }
            }
            return (nextChar < nChars) || in.ready();
        }
    }

    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.
     *
     * @param readAheadLimit   Limit on the number of characters that may be
     *                         read while still preserving the mark. An attempt
     *                         to reset the stream after reading characters
     *                         up to this limit or beyond may fail.
     *                         A limit value larger than the size of the input
     *                         buffer will cause a new buffer to be allocated
     *                         whose size is no smaller than limit.
     *                         Therefore large values should be used with care.
     *                         在持有mark时允许读取的最大字符数。当读取了超过这个限制的字符后尝试reset会失败。当这个限制超过了缓冲区大小时会引起新分配一个不小于这个大小的缓冲区，因此要小心输入一个大值。
     *
     * @exception  IllegalArgumentException  If {@code readAheadLimit < 0}
     * @exception  IOException  If an I/O error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (lock) {
            ensureOpen();
            this.readAheadLimit = readAheadLimit;
            markedChar = nextChar;
            markedSkipLF = skipLF;
        }
    }

    /**
     * Resets the stream to the most recent mark.
     *
     * @exception  IOException  If the stream has never been marked,
     *                          or if the mark has been invalidated
     */
    public void reset() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (markedChar < 0)
                throw new IOException((markedChar == INVALIDATED)
                                      ? "Mark invalid"
                                      : "Stream not marked");
            nextChar = markedChar;
            skipLF = markedSkipLF;
        }
    }

    public void close() throws IOException {
        synchronized (lock) {
            if (in == null)
                return;
            try {
                in.close();
            } finally {
                in = null;
                cb = null;
            }
        }
    }

    /**
     * Returns a {@code Stream}, the elements of which are lines read from
     * this {@code BufferedReader}.  The {@link Stream} is lazily populated,
     * i.e., read only occurs during the
     * <a href="../util/stream/package-summary.html#StreamOps">terminal
     * stream operation</a>.
     * 返回一个流，元素是从这个BufferedReader读取的行。流是延迟构成的，比如读取只发生在终止流操作中
     *
     * <p> The reader must not be operated on during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     * reader在执行终止流操作期间不能被操作，否则，终止流的结果会不确定
     *
     * <p> After execution of the terminal stream operation there are no
     * guarantees that the reader will be at a specific position from which to
     * read the next character or line.
     * 在执行终止流操作之后，不保证reader会在某个特定位置以读取下一个字符或者行。
     *
     * <p> If an {@link IOException} is thrown when accessing the underlying
     * {@code BufferedReader}, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the {@code Stream}
     * method that caused the read to take place. This method will return a
     * Stream if invoked on a BufferedReader that is closed. Any operation on
     * that stream that requires reading from the BufferedReader after it is
     * closed, will cause an UncheckedIOException to be thrown.
     * 如果访问下层BufferedReader时抛出了IOException，它会被包装为UncheckedIOException由Stream抛出
     * 这个方法如果调用在一个关闭的BufferedReader上会返回stream，任何请求从关闭后的BufferedReader中读取的请求都会引起抛出UncheckedIOException
     *
     * @return a {@code Stream<String>} providing the lines of text
     *         described by this {@code BufferedReader}
     *
     * @since 1.8
     */
    public Stream<String> lines() {
        Iterator<String> iter = new Iterator<String>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (nextLine != null) {
                    return true;
                } else {
                    try {
                        nextLine = readLine();
                        return (nextLine != null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next() {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iter, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
