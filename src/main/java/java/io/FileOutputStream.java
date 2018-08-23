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

import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;


/**
 * A file output stream is an output stream for writing data to a
 * <code>File</code> or to a <code>FileDescriptor</code>. Whether or not
 * a file is available or may be created depends upon the underlying
 * platform.  Some platforms, in particular, allow a file to be opened
 * for writing by only one <tt>FileOutputStream</tt> (or other
 * file-writing object) at a time.  In such situations the constructors in
 * this class will fail if the file involved is already open.
 * 文件输出流将数据也到一个文件或者是一个文件描述符中，无论文件是否有效或者可能根据所在平台创建一个新的文件。
 * 在一些平台上，一个文件只允许被一个FileOutputStream或者其他文件写入对象进行操作，在这种环境下，本类的构建在文件已经被打开时可能会出错
 *
 * <p><code>FileOutputStream</code> is meant for writing streams of raw bytes
 * such as image data. For writing streams of characters, consider using
 * <code>FileWriter</code>.
 * FileOutputStream写入的是比特，可以用于写入图片数据，如果要写入字符的话可以考虑使用FileWriter
 *
 * @author  Arthur van Hoff
 * @see     java.io.File
 * @see     java.io.FileDescriptor
 * @see     java.io.FileInputStream
 * @see     java.nio.file.Files#newOutputStream
 * @since   JDK1.0
 */
public
class FileOutputStream extends OutputStream
{
    /**
     * The system dependent file descriptor.系统依赖的文件描述符
     */
    private final FileDescriptor fd;

    /**
     * True if the file is opened for append.文件为扩展模式在末尾添加时为true
     */
    private final boolean append;

    /**
     * The associated channel, initialized lazily.相关联的文件通道，延迟初始化
     */
    private FileChannel channel;

    /**
     * The path of the referenced file
     * (null if the stream is created with a file descriptor)
     * 文件路径，如果该流是通过文件描述符来创建，为null
     */
    private final String path;

    private final Object closeLock = new Object();
    private volatile boolean closed = false;

    /**
     * Creates a file output stream to write to the file with the
     * specified name. A new <code>FileDescriptor</code> object is
     * created to represent this file connection.
     * 通过具体的名字创建一个文件输出流来写入到文件。一个新的文件描述符被创建来代表这个文件连接。
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with <code>name</code> as its argument.
     * 如果有一个安全管理器，它的checkWrite方法需要传入name参数
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     * 如果文件存在但它是一个目录而不是规则的文件，或者文件不出在但不能被创建，或者文件因为其他原因不能被打开，抛出FileNotFoundException
     *
     * @param      name   the system-dependent filename系统相关文件名
     * @exception  FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @exception  SecurityException  if a security manager exists and its
     *               <code>checkWrite</code> method denies write access
     *               to the file.如果存在安全管理器，它的checkWrite方法拒绝了对文件的写入权限
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null, false);//默认输出流从文件头部开始写入，会导致文本被清空
    }

    /**
     * Creates a file output stream to write to the file with the specified
     * name.  If the second argument is <code>true</code>, then
     * bytes will be written to the end of the file rather than the beginning.
     * A new <code>FileDescriptor</code> object is created to represent this
     * file connection.
     * 通过具体的名字创建一个文件输出流来写入到文件。如果第二个参数时true，那么byte会写入到文件的末尾而不是从头开始。一个新的文件描述符被创建来代表这个文件连接。
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with <code>name</code> as its argument.
     * 如果有一个安全管理器，它的checkWrite方法需要传入name参数
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     * 如果文件存在但它是一个目录而不是规则的文件，或者文件不出在但不能被创建，或者文件因为其他原因不能被打开，抛出FileNotFoundException
     *
     * @param     name        the system-dependent file name系统相关文件名
     * @param     append      if <code>true</code>, then bytes will be written
     *                   to the end of the file rather than the beginning 为true时从文件原内容不变末尾写入，否则清空文件从头写入
     * @exception  FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason.
     * @exception  SecurityException  if a security manager exists and its
     *               <code>checkWrite</code> method denies write access
     *               to the file.
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     * @since     JDK1.1
     */
    public FileOutputStream(String name, boolean append)
        throws FileNotFoundException
    {
        this(name != null ? new File(name) : null, append);
    }

    /**
     * Creates a file output stream to write to the file represented by
     * the specified <code>File</code> object. A new
     * <code>FileDescriptor</code> object is created to represent this
     * file connection.
     * 创建一个文件输出流来向一个具体的file中写入数据。一个新的文件描述符被创建来代表这个文件连接。
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with the path represented by the <code>file</code>
     * argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param      file               the file to be opened for writing.
     * @exception  FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @exception  SecurityException  if a security manager exists and its
     *               <code>checkWrite</code> method denies write access
     *               to the file.
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);//清空文件并且从头开始输入
    }

    /**
     * Creates a file output stream to write to the file represented by
     * the specified <code>File</code> object. If the second argument is
     * <code>true</code>, then bytes will be written to the end of the file
     * rather than the beginning. A new <code>FileDescriptor</code> object is
     * created to represent this file connection.
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with the path represented by the <code>file</code>
     * argument as its argument.
     * <p>
     * If the file exists but is a directory rather than a regular file, does
     * not exist but cannot be created, or cannot be opened for any other
     * reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param      file               the file to be opened for writing.
     * @param     append      if <code>true</code>, then bytes will be written
     *                   to the end of the file rather than the beginning
     * @exception  FileNotFoundException  if the file exists but is a directory
     *                   rather than a regular file, does not exist but cannot
     *                   be created, or cannot be opened for any other reason
     * @exception  SecurityException  if a security manager exists and its
     *               <code>checkWrite</code> method denies write access
     *               to the file.
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     * @since 1.4
     */
    public FileOutputStream(File file, boolean append)
        throws FileNotFoundException
    {
        String name = (file != null ? file.getPath() : null);//file的路径和文件名
        SecurityManager security = System.getSecurityManager();//获取操作系统的安全管理器
        if (security != null) {
            security.checkWrite(name);//检查对文件是否有写入权限
        }
        if (name == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        this.fd = new FileDescriptor();
        fd.attach(this);//便于文件描述符关闭文件
        this.append = append;
        this.path = name;

        open(name, append);
    }

    /**
     * Creates a file output stream to write to the specified file
     * descriptor, which represents an existing connection to an actual
     * file in the file system.
     * 创建一个文件输入流写入到具体的文件描述符中，该文件描述符表示了对一个文件系统中实际文件存在的链接
     * <p>
     * First, if there is a security manager, its <code>checkWrite</code>
     * method is called with the file descriptor <code>fdObj</code>
     * argument as its argument.
     * 安全管理器的checkWrite参数是文件描述符fdObj
     * <p>
     * If <code>fdObj</code> is null then a <code>NullPointerException</code>
     * is thrown.
     * <p>
     * 如果fdObj是null会抛出NullPointerException
     * This constructor does not throw an exception if <code>fdObj</code>
     * is {@link java.io.FileDescriptor#valid() invalid}.
     * However, if the methods are invoked on the resulting stream to attempt
     * I/O on the stream, an <code>IOException</code> is thrown.
     * 如果fdObj不可用不会抛出异常，但是，如果此时尝试调用该流的IO方法会抛出IOException
     *
     * @param      fdObj   the file descriptor to be opened for writing
     * @exception  SecurityException  if a security manager exists and its
     *               <code>checkWrite</code> method denies
     *               write access to the file descriptor
     * @see        java.lang.SecurityManager#checkWrite(java.io.FileDescriptor)
     */
    public FileOutputStream(FileDescriptor fdObj) {
        SecurityManager security = System.getSecurityManager();
        if (fdObj == null) {
            throw new NullPointerException();
        }
        if (security != null) {
            security.checkWrite(fdObj);
        }
        this.fd = fdObj;
        this.append = false;//从头写入
        this.path = null;//使用文件描述符时没有路径

        fd.attach(this);
    }

    /**
     * Opens a file, with the specified name, for overwriting or appending.
     * @param name name of file to be opened
     * @param append whether the file is to be opened in append mode
     */
    private native void open0(String name, boolean append)
        throws FileNotFoundException;

    // wrap native call to allow instrumentation
    /**
     * Opens a file, with the specified name, for overwriting or appending.
     * 通过具体的名字打开一个文件来重写或扩展
     * @param name name of file to be opened
     * @param append whether the file is to be opened in append mode
     */
    private void open(String name, boolean append)
        throws FileNotFoundException {
        open0(name, append);//调用native方法打开文件
    }

    /**
     * Writes the specified byte to this file output stream.
     *
     * @param   b   the byte to be written.要写入的byte
     * @param   append   {@code true} if the write operation first
     *     advances the position to the end of file为true时写到文件末尾
     */
    private native void write(int b, boolean append) throws IOException;

    /**
     * Writes the specified byte to this file output stream. Implements
     * the <code>write</code> method of <code>OutputStream</code>.
     * 将具体的byte写入到文件输出流中，实现了OutputStream.write方法
     *
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        write(b, append);
    }

    /**
     * Writes a sub array as a sequence of bytes.
     * native方法，将一个bytes序列的子数组写入
     * @param b the data to be written
     * @param off the start offset in the data开始的偏移位置
     * @param len the number of bytes that are written写入的bytes长度
     * @param append {@code true} to first advance the position to the
     *     end of file
     * @exception IOException If an I/O error has occurred.
     */
    private native void writeBytes(byte b[], int off, int len, boolean append)
        throws IOException;

    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * to this file output stream.
     *
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte b[]) throws IOException {
        writeBytes(b, 0, b.length, append);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this file output stream.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte b[], int off, int len) throws IOException {
        writeBytes(b, off, len, append);
    }

    /**
     * Closes this file output stream and releases any system resources
     * associated with this stream. This file output stream may no longer
     * be used for writing bytes.
     * 关闭这个文件输出流并释放任何关联的系统资源，这个输出流不能再用于写入bytes
     *
     * <p> If this stream has an associated channel then the channel is closed
     * as well.
     * 如果流关联到了通道，则通道也关闭
     *
     * @exception  IOException  if an I/O error occurs.
     *
     * @revised 1.4
     * @spec JSR-51
     */
    public void close() throws IOException {
        synchronized (closeLock) {//close只能进行一次所以需要是线程安全的，只能由一个线程进行
            if (closed) {
                return;
            }
            closed = true;
        }

        if (channel != null) {
            channel.close();//关闭文件通道
        }

        fd.closeAll(new Closeable() {
            public void close() throws IOException {
               close0();
           }
        });
    }

    /**
     * Returns the file descriptor associated with this stream.
     * 返回文件描述符
     *
     * @return  the <code>FileDescriptor</code> object that represents
     *          the connection to the file in the file system being used
     *          by this <code>FileOutputStream</code> object.
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FileDescriptor
     */
     public final FileDescriptor getFD()  throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
     }

    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file output stream.
     * 返回关联的文件通道
     *
     * <p> The initial {@link java.nio.channels.FileChannel#position()
     * position} of the returned channel will be equal to the
     * number of bytes written to the file so far unless this stream is in
     * append mode, in which case it will be equal to the size of the file.
     * Writing bytes to this stream will increment the channel's position
     * accordingly.  Changing the channel's position, either explicitly or by
     * writing, will change this stream's file position.
     * 返回通道的初始化等于到目前为止写入文件的bytes数量，除非当前的流是扩展模式，该模式下等于文件的大小。
     * 写入bytes将会增加通道的位置，无论是通过写入或者指明来改变通道的位置都会改变流的文件位置
     *
     * @return  the file channel associated with this file output stream
     *
     * @since 1.4
     * @spec JSR-51
     */
    public FileChannel getChannel() {
        synchronized (this) {//确保只初始化一次
            if (channel == null) {
                channel = FileChannelImpl.open(fd, path, false, true, append, this);
            }
            return channel;
        }
    }

    /**
     * Cleans up the connection to the file, and ensures that the
     * <code>close</code> method of this file output stream is
     * called when there are no more references to this stream.
     * 清除所有到文件的连接，确保当没有其他对这个流的引用时，close方法被调用
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FileInputStream#close()
     */
    protected void finalize() throws IOException {
        if (fd != null) {
            if (fd == FileDescriptor.out || fd == FileDescriptor.err) {
                flush();
            } else {
                /* if fd is shared, the references in FileDescriptor
                 * will ensure that finalizer is only called when
                 * safe to do so. All references using the fd have
                 * become unreachable. We can call close()
                 * 如果fd被共享，FileDescriptor中的引用会确保终结器只在安全的时候被调用。
                 * 所有使用fd的引用都不可达时，我们调用close
                 */
                close();
            }
        }
    }

    private native void close0() throws IOException;

    private static native void initIDs();

    static {
        initIDs();
    }

}
