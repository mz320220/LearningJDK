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

import java.util.Arrays;
import java.util.Objects;

/**EM DONE
 * A {@code ByteArrayInputStream} contains
 * an internal buffer that contains bytes that
 * may be read from the stream. An internal
 * counter keeps track of the next byte to
 * be supplied by the {@code read} method.
 * A ByteArrayInputStream包含一个内部缓冲区，其中包含可以从流中读取的字节。
 * 内部计数器跟踪read方法要提供的下一个字节。
 *
 * <p>
 * Closing a {@code ByteArrayInputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 * 关闭ByteArrayInputStream没有任何效果。 在关闭流之后，可以调用此类中的方法，而不生成IOException 。
 *
 * @author  Arthur van Hoff
 * @see     java.io.StringBufferInputStream
 * @since   1.0
 */
public class ByteArrayInputStream extends InputStream {

    /**EM DONE
     * An array of bytes that was provided
     * by the creator of the stream. Elements {@code buf[0]}
     * through {@code buf[count-1]} are the
     * only bytes that can ever be read from the
     * stream;  element {@code buf[pos]} is
     * the next byte to be read.
     * 保存字节输入流数据的字节数组，buf[0]到buf[count-1]为可被读取的字节
     * buf[pos]是下一个可被读取的字节
     */
    protected byte buf[];

    /**EM DONE
     * The index of the next character to read from the input stream buffer.
     * This value should always be nonnegative
     * and not larger than the value of {@code count}.
     * The next byte to be read from the input stream buffer
     * will be {@code buf[pos]}.
     * 下一个可被读取的字节的索引，非负 && 不大于count
     */
    protected int pos;

    /**EM DONE
     * The currently marked position in the stream.
     * ByteArrayInputStream objects are marked at position zero by
     * default when constructed.  They may be marked at another
     * position within the buffer by the {@code mark()} method.
     * The current buffer position is set to this point by the
     * {@code reset()} method.
     * <p>
     * If no mark has been set, then the value of mark is the offset
     * passed to the constructor (or 0 if the offset was not supplied).
     *
     * @since   1.1
     */
    protected int mark = 0; //mark默认是0，如果构造方法存在offset则mark为offset的值

    /**EM DONE
     * The index one greater than the last valid character in the input
     * stream buffer.
     * This value should always be nonnegative
     * and not larger than the length of {@code buf}.
     * It  is one greater than the position of
     * the last byte within {@code buf} that
     * can ever be read  from the input stream buffer.
     * 字节流的长度
     */
    protected int count;

    /**EM DONE
     * Creates a {@code ByteArrayInputStream}
     * so that it  uses {@code buf} as its
     * buffer array.
     * The buffer array is not copied.
     * The initial value of {@code pos}
     * is {@code 0} and the initial value
     * of  {@code count} is the length of
     * {@code buf}.
     *  构造函数：创建一个内容为buf的字节流
     * @param   buf   the input buffer.
     */
    public ByteArrayInputStream(byte buf[]) {
        this.buf = buf; //初始化内容
        this.pos = 0;  //初始化下一个被读取的字节索引
        this.count = buf.length; //初始化字节流长度
    }

    /**EM DONE
     * Creates {@code ByteArrayInputStream}
     * that uses {@code buf} as its
     * buffer array. The initial value of {@code pos}
     * is {@code offset} and the initial value
     * of {@code count} is the minimum of {@code offset+length}
     * and {@code buf.length}.
     * 构造函数：创建一个内容为buf的字节流，并且是从offset开始读取数据，读取的长度为length
     *
     * The buffer array is not copied. The buffer's mark is
     * set to the specified offset.
     * ！！不复制缓冲区（直接赋值），缓冲区的mark被赋值为offset
     *
     *
     * @param   buf      the input buffer.
     * @param   offset   the offset in the buffer of the first byte to read.
     * @param   length   the maximum number of bytes to read from the buffer.
     */
    public ByteArrayInputStream(byte buf[], int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        this.count = Math.min(offset + length, buf.length); //所以不要求offset + length不大于buf.length,做了容错处理
        this.mark = offset; //ATTENTION
    }

    /**EM DONE
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an {@code int} in the range
     * {@code 0} to {@code 255}. If no byte is available
     * because the end of the stream has been reached, the value
     * {@code -1} is returned.
     * 读取下一个字节。字节以int类型返回，范围在0-255内。到达stream末尾则返回-1
     *
     * <p>
     * This {@code read} method
     * cannot block.
     *
     * @return  the next byte of data, or {@code -1} if the end of the
     *          stream has been reached.
     */
    // 注意：& 0xff --> 消除运算符升级自动部位导致的符号问题
    public synchronized int read() {
        return (pos < count) ? (buf[pos++] & 0xff) : -1;
    }

    /**EM DONE
     * Reads up to {@code len} bytes of data into an array of bytes from this
     * input stream.  If {@code pos} equals {@code count}, then {@code -1} is
     * returned to indicate end of file.  Otherwise, the  number {@code k} of
     * bytes read is equal to the smaller of {@code len} and {@code count-pos}.
     * If {@code k} is positive, then bytes {@code buf[pos]} through
     * {@code buf[pos+k-1]} are copied into {@code b[off]} through
     * {@code b[off+k-1]} in the manner performed by {@code System.arraycopy}.
     * The value {@code k} is added into {@code pos} and {@code k} is returned.
     * 从此输入流读取最多len字节的数据到一个字节数组。
     * 如果pos等于count ，则返回-1以指示文件结束。 否则，读取的字节数为k等于len和count-pos中的较小的。
     * 如果k为正，则字节buf[pos]到buf[pos+k-1]，被复制到b[off]到b[off+k-1]中（通过System.arraycopy )。
     * 值k被累加到pos并返回k 。
     *
     * <p>
     * This {@code read} method cannot block.
     *
     * @param   b     the buffer into which the data is read.
     * @param   off   the start offset in the destination array {@code b}
     * @param   len   the maximum number of bytes read.
     * @return  the total number of bytes read into the buffer, or
     *          {@code -1} if there is no more data because the end of
     *          the stream has been reached.
     * @throws  NullPointerException If {@code b} is {@code null}.
     * @throws  IndexOutOfBoundsException If {@code off} is negative,
     * {@code len} is negative, or {@code len} is greater than
     * {@code b.length - off}
     */
    public synchronized int read(byte b[], int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);

        if (pos >= count) {
            return -1;
        }

        int avail = count - pos;
        if (len > avail) {
            len = avail;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    //读取缓冲区中剩余所有字节流
    public synchronized byte[] readAllBytes() {
        byte[] result = Arrays.copyOfRange(buf, pos, count);
        pos = count;
        return result;
    }

    //对read方法做了包装，唯一区别是将-1处理成了0
    public int readNBytes(byte[] b, int off, int len) {
        int n = read(b, off, len);
        return n == -1 ? 0 : n;
    }

    //将缓冲区数据写入到指定的OutputStream中
    public synchronized long transferTo(OutputStream out) throws IOException {
        int len = count - pos;
        out.write(buf, pos, len);
        pos = count;
        return len;
    }

    /**EM DONE
     * Skips {@code n} bytes of input from this input stream. Fewer
     * bytes might be skipped if the end of the input stream is reached.
     * The actual number {@code k}
     * of bytes to be skipped is equal to the smaller
     * of {@code n} and  {@code count-pos}.
     * The value {@code k} is added into {@code pos}
     * and {@code k} is returned.
     * 从此输入流跳过n个字节的输入。
     * 如果达到输入流的结尾，则可能会跳过更少的字节。 要跳过的字节的实际数字为k:等于n和count-pos中的较小count-pos
     * 值k被累加到pos ，返回k 。
     *
     * @param   n   the number of bytes to be skipped.
     * @return  the actual number of bytes skipped.
     */
    public synchronized long skip(long n) {
        long k = count - pos;
        if (n < k) {
            k = n < 0 ? 0 : n; //k为n和缓冲区剩余字节数中较小的那个值
        }

        pos += k; //直接通过pos索引处理
        return k;
    }

    /**EM DONE
     * Returns the number of remaining bytes that can be read (or skipped over)
     * from this input stream.
     * <p>
     * The value returned is {@code count - pos},
     * which is the number of bytes remaining to be read from the input buffer.
     *
     *  缓冲区中可以被读取或跳过的字节数量
     * @return  the number of remaining bytes that can be read (or skipped
     *          over) from this input stream without blocking.
     */
    public synchronized int available() {
        return count - pos;
    }

    /**EM DONE
     * Tests if this {@code InputStream} supports mark/reset. The
     * {@code markSupported} method of {@code ByteArrayInputStream}
     * always returns {@code true}.
     * 是否支持mark操作，ByteArrayInputStream的本方法始终返回true
     *
     * @since   1.1
     */
    public boolean markSupported() {
        return true;
    }

    /**EM DONE
     * Set the current marked position in the stream.
     * ByteArrayInputStream objects are marked at position zero by
     * default when constructed.  They may be marked at another
     * position within the buffer by this method.
     * 设置流中当前标记的位置。 当构造时，ByteArrayInputStream对象在默认情况下标记为零。 它们可以通过此方法标记在缓冲区内的另一个位置。
     * <p>
     * If no mark has been set, then the value of the mark is the
     * offset passed to the constructor (or 0 if the offset was not
     * supplied).
     * 如果没有设置标记，则标记的值是传递给构造函数的偏移量（如果没有提供偏移量，则为0）。
     *
     * <p> Note: The {@code readAheadLimit} for this class
     *  has no meaning. ！！！这个类的readAheadLimit没有意义
     *
     * @since   1.1
     */
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    /**EM DONE
     * Resets the buffer to the marked position.  The marked position
     * is 0 unless another position was marked or an offset was specified
     * in the constructor.
     */
    public synchronized void reset() {
        pos = mark;
    }

    /**EM DONE
     * Closing a {@code ByteArrayInputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     * 关闭ByteArrayInputStream没有任何效果。
     * 甚至在关闭流之后，可以调用此类中的方法，而不生成IOException 。
     */
    public void close() throws IOException {
    }

}
