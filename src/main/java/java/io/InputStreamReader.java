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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import sun.nio.cs.StreamDecoder;


/**
 * An InputStreamReader is a bridge from byte streams to character streams: It
 * reads bytes and decodes them into characters using a specified {@link
 * java.nio.charset.Charset charset}.  The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 * InputStreamReader是从字节流到字符流的桥梁：它读取字节并使用特定的字符集解码成字符。
 * 字符集可能通过名字来确定或者直接特别给出或者是平台的默认字符集。
 *
 * <p> Each invocation of one of an InputStreamReader's read() methods may
 * cause one or more bytes to be read from the underlying byte-input stream.
 * To enable the efficient conversion of bytes to characters, more bytes may
 * be read ahead from the underlying stream than are necessary to satisfy the
 * current read operation.
 * InputStreamReader的每一个read方法的调用可能会引起一个或多个字节从字节输出流中被读取。
 * 为了使字节能够有效的转换为字符，可能会提前从流中读取比当前读取操作所需字节数更多的字节。
 *
 * <p> For top efficiency, consider wrapping an InputStreamReader within a
 * BufferedReader.  For example:
 * 为了提高效率，考虑将InputStreamReader嵌入到BufferedReader中，比如
 *
 * <pre>
 * BufferedReader in
 *   = new BufferedReader(new InputStreamReader(System.in));
 * </pre>
 *
 * @see BufferedReader
 * @see InputStream
 * @see java.nio.charset.Charset
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public class InputStreamReader extends Reader {

    private final StreamDecoder sd;

    /**
     * Creates an InputStreamReader that uses the default charset.
     * 创建一个使用默认字符集的InputStreamReader
     *
     * @param  in   An InputStream
     */
    public InputStreamReader(InputStream in) {
        super(in);//Reader的lock是InputStream
        try {
            sd = StreamDecoder.forInputStreamReader(in, this, (String)null); // ## check lock object
        } catch (UnsupportedEncodingException e) {
            // The default encoding should always be available默认的字符集总是有效的，所以无参构造不会抛出
            throw new Error(e);
        }
    }

    /**
     * Creates an InputStreamReader that uses the named charset.
     * 创建一个使用指定名字字符集的InputStreamReader
     *
     * @param  in
     *         An InputStream
     *
     * @param  charsetName
     *         The name of a supported
     *         {@link java.nio.charset.Charset charset}
     *
     * @exception  UnsupportedEncodingException
     *             If the named charset is not supported
     */
    public InputStreamReader(InputStream in, String charsetName)
        throws UnsupportedEncodingException
    {
        super(in);
        if (charsetName == null)
            throw new NullPointerException("charsetName");
        sd = StreamDecoder.forInputStreamReader(in, this, charsetName);
    }

    /**
     * Creates an InputStreamReader that uses the given charset.
     * 创建一个使用给出的字符集的InputStreamReader
     *
     * @param  in       An InputStream
     * @param  cs       A charset
     *
     * @since 1.4
     * @spec JSR-51
     */
    public InputStreamReader(InputStream in, Charset cs) {
        super(in);
        if (cs == null)
            throw new NullPointerException("charset");
        sd = StreamDecoder.forInputStreamReader(in, this, cs);
    }

    /**
     * Creates an InputStreamReader that uses the given charset decoder.
     * 创建一个使用给出的字符集解码器的InputStreamReader
     *
     * @param  in       An InputStream
     * @param  dec      A charset decoder
     *
     * @since 1.4
     * @spec JSR-51
     */
    public InputStreamReader(InputStream in, CharsetDecoder dec) {
        super(in);
        if (dec == null)
            throw new NullPointerException("charset decoder");
        sd = StreamDecoder.forInputStreamReader(in, this, dec);
    }

    /**
     * Returns the name of the character encoding being used by this stream.
     * 返回这个流使用的字符编码名字
     *
     * <p> If the encoding has an historical name then that name is returned;
     * otherwise the encoding's canonical name is returned.
     * 如果编码有历史名则返回它，如果没有的话返回官方名字
     *
     * <p> If this instance was created with the {@link
     * #InputStreamReader(InputStream, String)} constructor then the returned
     * name, being unique for the encoding, may differ from the name passed to
     * the constructor. This method will return <code>null</code> if the
     * stream has been closed.
     * </p>
     * 如果这个对象是通过InputStreamReader(InputStream, String)构造的，返回的名字可能给传给构造函数的不同
     * 如果流已经被关闭会返回null
     * @return The historical name of this encoding, or
     *         <code>null</code> if the stream has been closed
     *
     * @see java.nio.charset.Charset
     *
     * @revised 1.4
     * @spec JSR-51
     */
    public String getEncoding() {
        return sd.getEncoding();
    }

    /**
     * Reads a single character.
     * 读取单个字符
     *
     * @return The character read, or -1 if the end of the stream has been
     *         reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        return sd.read();
    }

    /**
     * Reads characters into a portion of an array.
     * 读取字符到一个数组中
     *
     * @param      cbuf     Destination buffer目标缓冲区
     * @param      offset   Offset at which to start storing characters开始存储字符的偏移
     * @param      length   Maximum number of characters to read读取字符的最大数
     *
     * @return     The number of characters read, or -1 if the end of the
     *             stream has been reached读取的字符数，如果开始前就已经到达末尾则返回-1
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read(char cbuf[], int offset, int length) throws IOException {
        return sd.read(cbuf, offset, length);
    }

    /**
     * Tells whether this stream is ready to be read.  An InputStreamReader is
     * ready if its input buffer is not empty, or if bytes are available to be
     * read from the underlying byte stream.
     * 该流是否准备完毕读取。当输入缓冲区是非空时，或者字节能够从下方的字节流读取时，InputStreamReader是准备完的
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        return sd.ready();
    }

    public void close() throws IOException {
        sd.close();
    }
}
