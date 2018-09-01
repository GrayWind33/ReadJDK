/*
 * Copyright (c) 1994, 2003, Oracle and/or its affiliates. All rights reserved.
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
 * The class implements a buffered output stream. By setting up such
 * an output stream, an application can write bytes to the underlying
 * output stream without necessarily causing a call to the underlying
 * system for each byte written.
 * 该类实现了一个缓冲输出流，通过设置这样的输出流，应用可以在写入字节到下层输出流时不需要在写每个字节都调用一次下层系统
 *
 * @author  Arthur van Hoff
 * @since   JDK1.0
 */
public
class BufferedOutputStream extends FilterOutputStream {
    /**
     * 存储数据的内部缓冲区
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer. This value is always
     * in the range <tt>0</tt> through <tt>buf.length</tt>; elements
     * <tt>buf[0]</tt> through <tt>buf[count-1]</tt> contain valid
     * byte data.
     * 缓冲区中有效字节的数量。这个值得范围总是从0到buf.length，元素buf[0]到buf[count-1]含有有效的字节数据
     */
    protected int count;

    /**
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream.
     * 创建一个新的缓冲输出流来写入数据到指定的下层输出流当中
     *
     * @param   out   the underlying output stream.
     */
    public BufferedOutputStream(OutputStream out) {
        this(out, 8192);//默认缓冲区大小是8K
    }

    /**
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream with the specified buffer
     * size.
     * 创建一个新的缓冲输出流来写入数据到指定的下层输出流当中，指定它的缓冲区大小
     *
     * @param   out    the underlying output stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if size &lt;= 0.
     */
    public BufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");//size必须大于0
        }
        buf = new byte[size];
    }

    /** Flush the internal buffer刷新内部缓冲区 */
    private void flushBuffer() throws IOException {
        if (count > 0) {//如果缓冲区内存在有效数据
            out.write(buf, 0, count);//将缓冲区内的有效数据全部写入到输出流
            count = 0;
        }
    }

    /**
     * Writes the specified byte to this buffered output stream.
     * 将指定的字节写入到这个缓冲输出流中
     *
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();//如果缓冲区满了则将缓冲区内的数据全部写入到输出流中
        }
        buf[count++] = (byte)b;//将b存储到缓冲区中
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this buffered output stream.
     * 从指定的字节数组中写从off偏移开始len长度的字节到这个缓冲输出流中
     *
     * <p> Ordinarily this method stores bytes from the given array into this
     * stream's buffer, flushing the buffer to the underlying output stream as
     * needed.  If the requested length is at least as large as this stream's
     * buffer, however, then this method will flush the buffer and write the
     * bytes directly to the underlying output stream.  Thus redundant
     * <code>BufferedOutputStream</code>s will not copy data unnecessarily.
     * 一般来说这个方法存储给出数组中的字节到这个流的缓冲区，必要的时候刷新缓冲区到下层输出流。
     * 如果请求的长度大于鞥与缓冲区大小，这个方法会刷新缓冲区，然后将字节直接写入到下层输出流中。
     * 这样多余的BufferedOutputStream就不会无必要的复制数据。
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized void write(byte b[], int off, int len) throws IOException {
        if (len >= buf.length) {
            /* If the request length exceeds the size of the output buffer,
               flush the output buffer and then write the data directly.
               In this way buffered streams will cascade harmlessly. 
               如果请求的长度超出了输出缓冲区的大小，刷新输出缓冲区然后直接写数据。这样缓冲流可以无损害地流动*/
            flushBuffer();
            out.write(b, off, len);//直接调用下层输出流的write方法
            return;
        }
        if (len > buf.length - count) {//要输出的字节长度超过了剩余缓冲区大小则先刷新清空缓冲区
            flushBuffer();
        }
        System.arraycopy(b, off, buf, count, len);//将要输出的内容存储到缓冲区
        count += len;
    }

    /**
     * Flushes this buffered output stream. This forces any buffered
     * output bytes to be written out to the underlying output stream.
     * 刷新这个缓冲输出流，这个命令任何的缓冲区中的输出字节写出到底层的输出流中
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public synchronized void flush() throws IOException {
        flushBuffer();//将缓冲区内的数据全部写入到输出流中
        out.flush();//触发下层输出流的刷新操作，对于FileOutputStream的话是什么也不做
    }
}
