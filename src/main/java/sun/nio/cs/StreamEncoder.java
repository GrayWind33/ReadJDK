/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 */

package sun.nio.cs;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class StreamEncoder extends Writer {

	private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;// 默认ByteBuffer大小8K

	private volatile boolean isOpen = true;

	private void ensureOpen() throws IOException {
		if (!isOpen)
			throw new IOException("Stream closed");
	}

	// java.io.OutputStreamWriter工厂模式
	public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, String charsetName)
			throws UnsupportedEncodingException {
		String csn = charsetName;
		if (csn == null)
			csn = Charset.defaultCharset().name();
		try {
			if (Charset.isSupported(csn))
				return new StreamEncoder(out, lock, Charset.forName(csn));
		} catch (IllegalCharsetNameException x) {
		}
		throw new UnsupportedEncodingException(csn);
	}

	public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, Charset cs) {
		return new StreamEncoder(out, lock, cs);
	}

	public static StreamEncoder forOutputStreamWriter(OutputStream out, Object lock, CharsetEncoder enc) {
		return new StreamEncoder(out, lock, enc);
	}

	// Factory for java.nio.channels.Channels.newWriter

	public static StreamEncoder forEncoder(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
		return new StreamEncoder(ch, enc, minBufferCap);
	}

	// -- Public methods corresponding to those in OutputStreamWriter --

	// All synchronization and state/argument checking is done in these public
	// methods; the concrete stream-encoder subclasses defined below need not
	// do any such checking.

	public String getEncoding() {
		if (isOpen())
			return encodingName();
		return null;
	}

	public void flushBuffer() throws IOException {
		synchronized (lock) {
			if (isOpen())
				implFlushBuffer();
			else
				throw new IOException("Stream closed");
		}
	}

	public void write(int c) throws IOException {
		char cbuf[] = new char[1];
		cbuf[0] = (char) c;
		write(cbuf, 0, 1);
	}

	public void write(char cbuf[], int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}
			implWrite(cbuf, off, len);
		}
	}

	public void write(String str, int off, int len) throws IOException {
		/* 创建字符缓冲区前检查长度 */
		if (len < 0)
			throw new IndexOutOfBoundsException();
		char cbuf[] = new char[len];
		str.getChars(off, off + len, cbuf, 0);// 将str中的value从off开始长度len的内容复制到cbuf中
		write(cbuf, 0, len);
	}

	public void flush() throws IOException {
		synchronized (lock) {
			ensureOpen();
			implFlush();
		}
	}

	public void close() throws IOException {
		synchronized (lock) {
			if (!isOpen)
				return;
			implClose();
			isOpen = false;//只能关闭一次
		}
	}

	private boolean isOpen() {
		return isOpen;
	}

	// -- Charset-based stream encoder impl --

	private Charset cs;
	private CharsetEncoder encoder;
	private ByteBuffer bb;

	// Exactly one of these is non-null
	private final OutputStream out;
	private WritableByteChannel ch;

	// Leftover first char in a surrogate pair代理对中剩下的第一个字符
	private boolean haveLeftoverChar = false;
	private char leftoverChar;
	private CharBuffer lcb = null;

	private StreamEncoder(OutputStream out, Object lock, Charset cs) {
		this(out, lock, cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)// 有畸形输入错误时解码器丢弃错误的输入，替换为替代值然后继续后面的操作
				.onUnmappableCharacter(CodingErrorAction.REPLACE));// 有不可用图形表示的字符错误出现时解码器丢弃错误的输入，替换为替代值然后继续后面的操作
	}

	private StreamEncoder(OutputStream out, Object lock, CharsetEncoder enc) {
		super(lock);// lock是OutputStream对象本身
		this.out = out;
		this.ch = null;
		this.cs = enc.charset();
		this.encoder = enc;

		// 在堆外内存速度更快之前不使用这段代码
		if (false && out instanceof FileOutputStream) {
			ch = ((FileOutputStream) out).getChannel();
			if (ch != null)
				bb = ByteBuffer.allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
		}
		if (ch == null) {
			bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
		}
	}

	private StreamEncoder(WritableByteChannel ch, CharsetEncoder enc, int mbc) {
		this.out = null;
		this.ch = ch;
		this.cs = enc.charset();
		this.encoder = enc;
		this.bb = ByteBuffer.allocate(mbc < 0 ? DEFAULT_BYTE_BUFFER_SIZE : mbc);
	}

	private void writeBytes() throws IOException {
		bb.flip();//ByteBuffer准备输出当前内容，将limit设为当前位置，pos设为0
		int lim = bb.limit();
		int pos = bb.position();
		assert (pos <= lim);
		int rem = (pos <= lim ? lim - pos : 0);

		if (rem > 0) {//输出ByteBuffer中全部内容
			if (ch != null) {
				if (ch.write(bb) != rem)
					assert false : rem;
			} else {
				out.write(bb.array(), bb.arrayOffset() + pos, rem);
			}
		}
		bb.clear();//清空ByteBuffer
	}

	private void flushLeftoverChar(CharBuffer cb, boolean endOfInput) throws IOException {
		if (!haveLeftoverChar && !endOfInput)
			return;
		if (lcb == null)//lcb内一开始是空的
			lcb = CharBuffer.allocate(2);
		else
			lcb.clear();
		if (haveLeftoverChar)
			lcb.put(leftoverChar);//将leftoverChar放入lcb中
		if ((cb != null) && cb.hasRemaining())
			lcb.put(cb.get());//cb的内容复制到lcb中
		lcb.flip();//将limit设为当前位置，pos设为0，所以现在lcb要输出的内容就是刚才从leftoverChar（如果有的话）和cb里读入的
		while (lcb.hasRemaining() || endOfInput) {
			CoderResult cr = encoder.encode(lcb, bb, endOfInput);//将lcb的内容编码为字节尽可能多的放入到ByteBuffer中
			if (cr.isUnderflow()) {//cr未溢出，ByteBuffer还有剩余的空间
				if (lcb.hasRemaining()) {//lcb还有剩余的数据
					leftoverChar = lcb.get();
					if (cb != null && cb.hasRemaining())
						flushLeftoverChar(cb, endOfInput);
					return;
				}
				break;
			}
			if (cr.isOverflow()) {//cr溢出，超出了ByteBuffer的上限
				assert bb.position() > 0;//ByteBuffer中存在数据
				writeBytes();//将ByteBuffer中的数据写入到输出流后清空ByteBuffer
				continue;
			}
			cr.throwException();
		}
		haveLeftoverChar = false;
	}

	void implWrite(char cbuf[], int off, int len) throws IOException {
		CharBuffer cb = CharBuffer.wrap(cbuf, off, len);// 将字符数组组装成一个堆内CharBuffer，数组中的内容不存在复制

		if (haveLeftoverChar)
			flushLeftoverChar(cb, false);

		while (cb.hasRemaining()) {
			CoderResult cr = encoder.encode(cb, bb, false);
			if (cr.isUnderflow()) {
				assert (cb.remaining() <= 1) : cb.remaining();
				if (cb.remaining() == 1) {
					//如果当前缓冲区仅剩一个字符，保存到leftoverChar并修改haveLeftoverChar状态，结束输出
					haveLeftoverChar = true;
					leftoverChar = cb.get();
				}
				break;
			}
			if (cr.isOverflow()) {//ByteBuffer满了
				assert bb.position() > 0;
				writeBytes();//将ByteBuffer的内容写入到输出流里面
				continue;
			}
			cr.throwException();
		}
	}

	void implFlushBuffer() throws IOException {
		if (bb.position() > 0)// 如果ByteBuffer内还有剩余的数据，将它们写入文件
			writeBytes();
	}

	void implFlush() throws IOException {
		implFlushBuffer();
		if (out != null)
			out.flush();//这里out的刷盘操作取决于子类的具体实现
	}

	void implClose() throws IOException {
		flushLeftoverChar(null, true);//将leftoverChar的内容写入的输出流
		try {
			for (;;) {
				CoderResult cr = encoder.flush(bb);
				if (cr.isUnderflow())//cr未溢出说明ByteBuffer中的数据全部落盘了
					break;
				if (cr.isOverflow()) {//cr溢出说明ByteBuffer仍然存在数据
					assert bb.position() > 0;
					writeBytes();//将ByteBuffer中的数据写入输出流
					continue;
				}
				cr.throwException();
			}

			if (bb.position() > 0)
				writeBytes();
			if (ch != null)//关闭文件通道或者输出流
				ch.close();
			else
				out.close();
		} catch (IOException x) {
			encoder.reset();
			throw x;
		}
	}

	String encodingName() {
		return ((cs instanceof HistoricallyNamedCharset) ? ((HistoricallyNamedCharset) cs).historicalName()
				: cs.name());
	}
}
