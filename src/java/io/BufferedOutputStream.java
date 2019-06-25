/*
 * Copyright (c) 1994, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**EM DONE
 * The class implements a buffered output stream. By setting up such
 * an output stream, an application can write bytes to the underlying
 * output stream without necessarily causing a call to the underlying
 * system for each byte written.
 *
 * 该类实现缓冲输出流。通过设置这样的输出流，应用程序可以向底层输出流写入字节，
 * 而不必为写入的每个字节进行底层系统的调用。
 *
 * @author  Arthur van Hoff
 * @since   1.0
 */
public class BufferedOutputStream extends FilterOutputStream {
    /**
     * The internal buffer where data is stored.
     * 存储数据的内部缓冲区。
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer. This value is always
     * in the range {@code 0} through {@code buf.length}; elements
     * {@code buf[0]} through {@code buf[count-1]} contain valid
     * byte data.
     * 缓冲区中有效字节的数量。长度始终在0-buf.length之间
     * buf[0] to buf[count-1]包含了所有有效的字节数据
     */
    protected int count;

    /**EM DONE
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream.
     * 构造一个大小为8192的输出流
     *
     * @param   out   the underlying output stream.
     */
    public BufferedOutputStream(OutputStream out) {
        this(out, 8192);
    }

    /**EM DONE
     * Creates a new buffered output stream to write data to the
     * specified underlying output stream with the specified buffer
     * size.
     * 构造特定大小的输出流
     *
     * @param   out    the underlying output stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if size &lt;= 0.
     */
    public BufferedOutputStream(OutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    /**EM DONE
     * Flush the internal buffer
     * 刷新缓冲区数据到底层输出流。
     */
    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    /**EM DONE
     * Writes the specified byte to this buffered output stream.
     * 将“数据b(转换成字节类型)”写入到输出流中
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public synchronized void write(int b) throws IOException {
        /**若缓冲已满，则先将缓冲数据写入到输出流中。
         * count表示buf缓冲区中有效的字节长度，>=buf.length说明已经满了
         */
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte)b;
    }

    /**EM DONE
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this buffered output stream.
     * 从指定的字节数组写入 len个字节，从偏移 off开始到缓冲的输出流。
     *
     * <p> Ordinarily this method stores bytes from the given array into this
     * stream's buffer, flushing the buffer to the underlying output stream as
     * needed.  If the requested length is at least as large as this stream's
     * buffer, however, then this method will flush the buffer and write the
     * bytes directly to the underlying output stream.  Thus redundant
     * <code>BufferedOutputStream</code>s will not copy data unnecessarily.
     *  正常来说：
     *  write方法从byte数组中copy到缓冲区buffer，然后flush数据到stream中。
     *  然而如果长度大于缓冲区大小：
     *  flush一次缓冲区后，直接写copy数据到stream中，所以不必要在进行冗余的拷贝过程
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        //待处理的数据长度大于缓冲区的长度
        if (len >= buf.length) {
            /* If the request length exceeds the size of the output buffer,
               flush the output buffer and then write the data directly.
               In this way buffered streams will cascade harmlessly.
               调用一次flush把buffer原有数据处理完毕后，直接调用outputstream.write写入
               */
            flushBuffer();
            out.write(b, off, len);
            return;
        }
        //待处理数据不大于缓冲区，但是大于缓冲区中可用空间
        if (len > buf.length - count) {
            //flush把缓冲区数据写入stream
            flushBuffer();
        }
        /**
         * 拷贝b偏移量off处长度len的数据到buf偏移量count处数据（由此处可知并没有写入stream）
         * ！！！需要手动flush一次才会真正的写入
         */
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**EM DONE
     * Flushes this buffered output stream. This forces any buffered
     * output bytes to be written out to the underlying output stream.
     * 将“缓冲数据”写入到输出流中
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    @Override
    public synchronized void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
}
