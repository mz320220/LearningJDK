/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Writes text to a character-output stream, buffering characters so as to
 * provide for the efficient writing of single characters, arrays, and strings.
 * 将文本写入字符输出流，缓冲字符以提供单个字符，数组和字符串的高效写入。
 *
 * <p> The buffer size may be specified, or the default size may be accepted.
 * The default is large enough for most purposes.
 * 可以指定缓冲区大小，或者可以接受默认大小。 默认值足够大，可用于大多数用途。
 *
 * <p> A newLine() method is provided, which uses the platform's own notion of
 * line separator as defined by the system property {@code line.separator}.
 * Not all platforms use the newline character ('\n') to terminate lines.
 * Calling this method to terminate each output line is therefore preferred to
 * writing a newline character directly.
 * 提供了一个newLine（）方法，它使用平台自己的系统属性line.separator定义的行分隔符概念。
 * 并非所有平台都使用换行符（'\ n'）来终止行。 因此，调用此方法来终止每个输出行比直接写入换行符要好一点。
 *
 * <p> In general, a Writer sends its output immediately to the underlying
 * character or byte stream.  Unless prompt output is required, it is advisable
 * to wrap a BufferedWriter around any Writer whose write() operations may be
 * costly, such as FileWriters and OutputStreamWriters.  For example,
 * 一般来说，Writer将其输出立即发送到底层字符或字节流。
 * 除非需要提示输出，否则建议将BufferedWriter包装在其他write（）操作可能很昂贵的Writer上，例如FileWriters和OutputStreamWriters。
 * 例如：包装在FileWriter外部~
 * <pre>
 * PrintWriter out
 *   = new PrintWriter(new BufferedWriter(new FileWriter("foo.out")));
 * </pre>
 *
 * will buffer the PrintWriter's output to the file.  Without buffering, each
 * invocation of a print() method would cause characters to be converted into
 * bytes that would then be written immediately to the file, which can be very
 * inefficient.
 * 将缓冲PrintWriter的输出到文件。 没有缓冲，每次调用print（）方法都会使字符转换为字节，然后立即写入文件，这可能非常低效。
 *
 * @see PrintWriter
 * @see FileWriter
 * @see OutputStreamWriter
 * @see java.nio.file.Files#newBufferedWriter
 *
 * @author      Mark Reinhold
 * @since       1.1
 */

public class BufferedWriter extends Writer {

    private Writer out; //输出流对象

    private char cb[]; //缓冲区对应的字符数组
    private int nChars, nextChar; //nChars：缓冲区字符总个数，nextChar：下一个要操作的字符在缓冲区的位置

    private static int defaultCharBufferSize = 8192;

    /**EM DONE
     * Creates a buffered character-output stream that uses a default-sized
     * output buffer.
     * 创建使用默认大小的输出缓冲区的缓冲字符输出流。
     * @param  out  A Writer
     */
    public BufferedWriter(Writer out) {
        this(out, defaultCharBufferSize);
    }

    /**EM DONE
     * Creates a new buffered character-output stream that uses an output
     * buffer of the given size.
     * 创建一个新的缓冲字符输出流，使用给定大小的输出缓冲区。
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
    }

    /** Checks to make sure that the stream has not been closed
     * 确保流未关闭，通过是否为null为依据*/
    private void ensureOpen() throws IOException {
        if (out == null)
            throw new IOException("Stream closed");
    }

    /**
     * Flushes the output buffer to the underlying character stream, without
     * flushing the stream itself.  This method is non-private only so that it
     * may be invoked by PrintStream.
     *  对缓冲区执行flush()操作，将缓冲区的数据写入到Writer中
     */
    void flushBuffer() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (nextChar == 0)
                return;
            out.write(cb, 0, nextChar);
            nextChar = 0;
        }
    }

    /**EM DONE
     * Writes a single character.
     * 写一个字符到缓冲区
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(int c) throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (nextChar >= nChars)
                flushBuffer(); //缓冲区已满则flush一次
            cb[nextChar++] = (char) c; //转换为字符格式
        }
    }

    /**EM DONE
     * Our own little min method, to avoid loading java.lang.Math if we've run
     * out of file descriptors and we're trying to print a stack trace.
     * 返回较小值的函数，避免引入Java Math类
     */
    private int min(int a, int b) {
        if (a < b) return a;
        return b;
    }

    /**EM DONE
     * Writes a portion of an array of characters.
     * 写入字符数组的一部分。
     *
     * <p> Ordinarily this method stores characters from the given array into
     * this stream's buffer, flushing the buffer to the underlying stream as
     * needed.  If the requested length is at least as large as the buffer,
     * however, then this method will flush the buffer and write the characters
     * directly to the underlying stream.  Thus redundant
     * {@code BufferedWriter}s will not copy data unnecessarily.
     * 通常，此方法将给定数组中的字符存储到此流的缓冲区中，根据需要将缓冲区刷新到底层流。
     * ！！！如果请求的长度至少与缓冲区一样大，那么这个方法将刷新缓冲区，并将字符直接写入底层流。（省略到缓冲区的字符拷贝）
     *
     * @param  cbuf  A character array
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to write
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code off} is negative, or {@code len} is negative,
     *          or {@code off + len} is negative or greater than the length
     *          of the given array
     *
     * @throws  IOException  If an I/O error occurs
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
                   请求写入的数据超过缓冲区大小，flush缓冲区后将字符直接写入stream*/
                flushBuffer();
                out.write(cbuf, off, len); //直接写入
                return;
            }

            int b = off, t = off + len;
            while (b < t) {
                /**
                 * nChars -nextChar:缓冲区中剩余可写入字符串大小
                 * t-b：剩余待写入的字符串大小
                 * 两者取消：每次只能写入大小较小的一个长度
                 */
                int d = min(nChars - nextChar, t - b);
                System.arraycopy(cbuf, b, cb, nextChar, d);
                b += d;
                nextChar += d;
                if (nextChar >= nChars)
                    flushBuffer();
            }
        }
    }

    /**EM DONE
     * Writes a portion of a String.
     * 写一个字符串的一部分。
     * @implSpec
     * While the specification of this method in the
     * {@linkplain java.io.Writer#write(java.lang.String,int,int) superclass}
     * recommends that an {@link IndexOutOfBoundsException} be thrown
     * if {@code len} is negative or {@code off + len} is negative,
     * the implementation in this class does not throw such an exception in
     * these cases but instead simply writes no characters.
     * 如果len或off+len为负值，superclass要求抛出一个IndexOutOfBoundsException 。
     * 而B本方法中，如果len参数的值为负，则不会写入任何字符。 这与superclass中的这种方法的规范并不相同
     *
     * @param  s     String to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code off} is negative,
     *          or {@code off + len} is greater than the length
     *          of the given string
     *
     * @throws  IOException  If an I/O error occurs
     */
    public void write(String s, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();

            int b = off, t = off + len;
            while (b < t) {
                int d = min(nChars - nextChar, t - b);
                //从字符串s的b位置取值到b+d位置，复制到字符串数组cb的nextChar处
                s.getChars(b, b + d, cb, nextChar);
                b += d;
                nextChar += d;
                if (nextChar >= nChars)
                    flushBuffer();
            }
        }
    }

    /**EM DONE
     * Writes a line separator.  The line separator string is defined by the
     * system property {@code line.separator}, and is not necessarily a single
     * newline ('\n') character.
     * 写一行行分隔符。 行分隔符字符串由系统属性line.separator定义，并不一定是单个换行符（'\ n'）字符。
     * ——更好的可移植性
     * @exception  IOException  If an I/O error occurs
     */
    public void newLine() throws IOException {
        write(System.lineSeparator());
    }

    /**EM DONE
     * Flushes the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
        synchronized (lock) {
            flushBuffer();
            out.flush();
        }
    }

    /**EM DONE
     * 关闭流，先刷新。
     * 一旦流已关闭，进一步的write（）或flush（）调用将导致抛出IOException。
     * 关闭以前关闭的流无效——幂等性
     */
    public void close() throws IOException {
        synchronized (lock) {
            if (out == null) {
                return;
            }
            try (Writer w = out) {
                flushBuffer();
            } finally {
                out = null;
                cb = null;
            }
        }
    }
}
