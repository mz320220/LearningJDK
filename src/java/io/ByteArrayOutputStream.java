/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * The data can be retrieved using {@code toByteArray()} and
 * {@code toString()}.
 * ByteArrayOutputStream实现了一个output stream，其中的数据以byte字节数组形式写出。缓存会随着数据不断写入自增长.
 * <p>
 * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 * 关闭ByteArrayOutputStream没有影响，其中的方法可以在流被关闭后被调用而不会抛出任何IO异常
 *
 * @author  Arthur van Hoff
 * @since   1.0
 */

public class ByteArrayOutputStream extends OutputStream {

    /**
     * The buffer where data is stored.
     * 保存“字节数组输出流”数据的数组
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer.
     * 缓冲区中的有效字节数
     */
    protected int count;

    /**
     * Creates a new {@code ByteArrayOutputStream}. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     * 构造函数：默认创建的字节数组初始大小是32，在必要时会增长
     */
    public ByteArrayOutputStream() {
        this(32);
    }

    /**
     * Creates a new {@code ByteArrayOutputStream}, with a buffer capacity of
     * the specified size, in bytes.
     * 构造指定大小的输出流
     *
     * @param  size   the initial size.
     * @throws IllegalArgumentException if size is negative.
     */
    public ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                                               + size);
        }
        buf = new byte[size];
    }

    /**
     * Increases the capacity if necessary to ensure that it can hold
     * at least the number of elements specified by the minimum
     * capacity argument.
     * 保证容量可以容纳指定大小minCapcacity，若“实际容量 < minCapacity”，则增加“字节数组输出流”的容量
     *
     * @param  minCapacity the desired minimum capacity
     * @throws OutOfMemoryError if {@code minCapacity < 0}.  This is
     * interpreted as a request for the unsatisfiably large capacity
     * {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
     */
    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0)
            grow(minCapacity);
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 可分配的最大数组大小，
     * 有些虚拟机在数组中保留一些头信息。
     * 尝试分配更大的数组可能会导致
     * OutOfMemoryError:请求的数组大小超过VM限制(所以-8)
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     * 扩容：以保证可以持有minCapacity指定的最小容量
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;       //原大小为缓冲区数组大小
        int newCapacity = oldCapacity << 1; //新的容量在原基础上扩大一倍
        if (newCapacity - minCapacity < 0)  //如果扩大后仍然小于指定的minCapacity，则以minCapacity作为新的容量大小
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)   //如果扩容后或指定的新的容量大小newCapacity已经大于最大允许缓冲区大小
                                                // 则指定大小为MAX_ARRAY_SIZE或MAX_VALUE
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);  //复制原缓冲区的数组到newCapacity大小的数组中
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     * Writes the specified byte to this {@code ByteArrayOutputStream}.
     * 写入一个字节b到“字节数组输出流”中，并将计数+1
     *
     * @param   b   the byte to be written.
     */
    public synchronized void write(int b) {
        ensureCapacity(count + 1); //确保缓冲区在现有基础上可以写入一个byte(否则会进行扩容)
        buf[count] = (byte) b; //缓冲区buf的第count个字节写入b
        count += 1; //计数+1
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this {@code ByteArrayOutputStream}.
     * 写入输出流，从数组b的off位置写入len个
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @throws  IndexOutOfBoundsException if {@code off} is negative,
     * {@code len} is negative, or {@code len} is greater than
     * {@code b.length - off}
     */
    public synchronized void write(byte b[], int off, int len) {
        //校验，需要满足：off + len <= b.length
        Objects.checkFromIndexSize(off, len, b.length);
        ensureCapacity(count + len);
        //数组复制：从b的off处复制len个到buf的count处
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes the complete contents of the specified byte array
     * to this {@code ByteArrayOutputStream}.
     *
     * @apiNote
     * This method is equivalent to {@link #write(byte[],int,int)
     * 等效于：
     * write(b, 0, b.length)}.
     *
     * @param   b     the data.
     * @throws  NullPointerException if {@code b} is {@code null}.
     * @since   11
     */
    public void writeBytes(byte b[]) {
        write(b, 0, b.length);
    }

    /**
     * Writes the complete contents of this {@code ByteArrayOutputStream} to
     * the specified output stream argument, as if by calling the output
     * stream's write method using {@code out.write(buf, 0, count)}.
     * 将ByteArrayOutputStream的完整内容写入到输出流OutputStream中
     *
     * @param   out   the output stream to which to write the data.
     * @throws  NullPointerException if {@code out} is {@code null}.
     * @throws  IOException if an I/O error occurs.
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Resets the {@code count} field of this {@code ByteArrayOutputStream}
     * to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     * 重置“字节数组输出流”的计数。所有输出流中的内容都会被舍弃，output stream可以被重新使用，重用已分配的缓冲区空间
     *
     * @see     java.io.ByteArrayInputStream#count
     */
    public synchronized void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     * 将字节输出流中的内容写入到一个新的字节数组中
     *
     * @return  the current contents of this output stream, as a byte array.
     * @see     java.io.ByteArrayOutputStream#size()
     */
    public synchronized byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    /**
     * Returns the current size of the buffer.
     * 返回缓冲区当前大小
     *
     * @return  the value of the {@code count} field, which is the number
     *          of valid bytes in this output stream.
     * @see     java.io.ByteArrayOutputStream#count
     */
    public synchronized int size() {
        return count;
    }

    /**
     * Converts the buffer's contents into a string decoding bytes using the
     * platform's default character set. The length of the new {@code String}
     * is a function of the character set, and hence may not be equal to the
     * size of the buffer.
     * 以平台默认格式输出到字符串中
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the default replacement string for the platform's
     * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
     * class should be used when more control over the decoding process is
     * required.
     *
     * @return String decoded from the buffer's contents.
     * @since  1.1
     */
    public synchronized String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link java.nio.charset.Charset charset}.
     * 指定格式输出到字符串中
     *
     * <p> This method is equivalent to {@code #toString(charset)} that takes a
     * {@link java.nio.charset.Charset charset}.
     *
     * <p> An invocation of this method of the form
     *
     * <pre> {@code
     *      ByteArrayOutputStream b = ...
     *      b.toString("UTF-8")
     *      }
     * </pre>
     *
     * behaves in exactly the same way as the expression
     *
     * <pre> {@code
     *      ByteArrayOutputStream b = ...
     *      b.toString(StandardCharsets.UTF_8)
     *      }
     * </pre>
     *
     *
     * @param  charsetName  the name of a supported
     *         {@link java.nio.charset.Charset charset}
     * @return String decoded from the buffer's contents.
     * @throws UnsupportedEncodingException
     *         If the named charset is not supported
     * @since  1.1
     */
    public synchronized String toString(String charsetName)
        throws UnsupportedEncodingException
    {
        return new String(buf, 0, count, charsetName);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the specified {@link java.nio.charset.Charset charset}. The length of the new
     * {@code String} is a function of the charset, and hence may not be equal
     * to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param      charset  the {@linkplain java.nio.charset.Charset charset}
     *             to be used to decode the {@code bytes}
     * @return     String decoded from the buffer's contents.
     * @since      10
     */
    public synchronized String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }

    /**
     * Creates a newly allocated string. Its size is the current size of
     * the output stream and the valid contents of the buffer have been
     * copied into it. Each character <i>c</i> in the resulting string is
     * constructed from the corresponding element <i>b</i> in the byte
     * array such that:
     * <blockquote><pre>{@code
     *     c == (char)(((hibyte & 0xff) << 8) | (b & 0xff))
     * }</pre></blockquote>
     *
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@link #toString(String charsetName)} or {@link #toString(Charset charset)}
     * method, which takes an encoding-name or charset argument,
     * or the {@code toString()} method, which uses the platform's default
     * character encoding.
     *
     * @param      hibyte    the high byte of each resulting Unicode character.
     * @return     the current contents of the output stream, as a string.
     * @see        java.io.ByteArrayOutputStream#size()
     * @see        java.io.ByteArrayOutputStream#toString(String)
     * @see        java.io.ByteArrayOutputStream#toString()
     */
    @Deprecated
    public synchronized String toString(int hibyte) {
        return new String(buf, hibyte, 0, count);
    }

    /**
     * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     *
     * close()方法在该类中无影响，流关闭后调用也不会抛出IO异常
     */
    public void close() throws IOException {
    }

}
