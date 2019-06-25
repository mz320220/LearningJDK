/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**EM DONE
 * Reads text from a character-input stream, buffering characters so as to
 * provide for the efficient reading of characters, arrays, and lines.
 * 从字符输入流读取文本，缓冲字符，以提供字符，数组和行的高效读取。
 *
 * <p> The buffer size may be specified, or the default size may be used.  The
 * default is large enough for most purposes.
 * 可以指定缓冲区大小，或者可以使用默认大小。通常默认值足够大，可用于大多数用途。
 *
 * <p> In general, each read request made of a Reader causes a corresponding
 * read request to be made of the underlying character or byte stream.  It is
 * therefore advisable to wrap a BufferedReader around any Reader whose read()
 * operations may be costly, such as FileReaders and InputStreamReaders.  For
 * example,
 * 通常，由读取器做出的每个读取请求将引起 -> 对底层字符或字节流的相应读取请求(开销较高)。
 * 因此，建议将BufferedReader包装在其它可能开销较高的read（）上，例如FileReaders和InputStreamReaders。
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
 * 上方程序将缓冲指定文件的输入。 没有缓冲，每次调用read（）或readLine（）可能会导致直接从文件中读取字节，转换成字符，然后返回，这可能非常低效。
 *
 * <p> Programs that use DataInputStreams for textual input can be localized by
 * replacing each DataInputStream with an appropriate BufferedReader.
 * 使用DataInputStreams进行文本输入的程序可以通过用适当的BufferedReader替换每个DataInputStream进行本地化。
 *
 * @see FileReader
 * @see InputStreamReader
 * @see java.nio.file.Files#newBufferedReader
 *
 * @author      Mark Reinhold
 * @since       1.1
 */

public class BufferedReader extends Reader {

    private Reader in;

    //字符缓冲区charBuffer
    private char cb[];
    //nChars:缓冲区中字符的总个数；nextChar:下一个字符在缓冲区的位置
    private int nChars, nextChar;

    /**
     *INVALIDATED：标记无效，标记了但是由于标记位置太长导致无效。
     * UNMARKED：从未标记过
     */
    private static final int INVALIDATED = -2;
    private static final int UNMARKED = -1;

    private int markedChar = UNMARKED;
    // “标记”能标记位置的最大长度
    private int readAheadLimit = 0; /* Valid only when markedChar > 0 */

    /** If the next character is a line feed, skip it
     * 是否忽略换行符标记
     */
    private boolean skipLF = false;

    /** The skipLF flag when the mark was set */
    private boolean markedSkipLF = false;

    private static int defaultCharBufferSize = 8192; //默认缓冲区大小
    private static int defaultExpectedLineLength = 80; //默认一行字符大小

    /**EM DONE
     * Creates a buffering character-input stream that uses an input buffer of
     * the specified size.
     * 创建一个固定大小的字符类型输入流BufferedReader
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

    /**EM DONE
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     *  设置一个默认大小8192字符类型输入流BufferedReader
     * @param  in   A Reader
     */
    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }

    /**EM DONE
     * Checks to make sure that the stream has not been closed
     * 确认流未关闭 -> 通过==null进行判断
     */
    private void ensureOpen() throws IOException {
        if (in == null)
            throw new IOException("Stream closed");
    }

    /**EM DONE
     * Fills the input buffer, taking the mark into account if it is valid.
     * 填充缓冲区函数。
     */
    private void fill() throws IOException {
        int dst; //缓冲区可插入数据的起始位置
        if (markedChar <= UNMARKED) {
            /* No mark */
            dst = 0; //没有标记直接将dst设置为0
        } else {
            /* Marked */
            int delta = nextChar - markedChar; //标记的长度：标记位置到下一个操作字符间的长度
            if (delta >= readAheadLimit) {
                /** Gone past read-ahead limit: Invalidate mark
                 * 标记长度大于上限readAheadLimit，直接舍弃
                 * */
                markedChar = INVALIDATED;
                readAheadLimit = 0;
                dst = 0;
            } else {
                if (readAheadLimit <= cb.length) { //标记长度小于标记上限 && 标记上限小于等于缓冲区大小
                    /* Shuffle in the current buffer，拖拽缓冲区cb偏移量markChar处delta长度到缓冲区开头 */
                    System.arraycopy(cb, markedChar, cb, 0, delta);
                    markedChar = 0;
                    dst = delta;
                } else { //标记长度小于标记上限 && 标记上限大于缓冲区大小
                    /** Reallocate buffer to accommodate read-ahead limit，重新分配缓冲区以适应预读限制
                     * new char[readAheadLimit];缓冲区大小为标记上限
                     * */
                    char ncb[] = new char[readAheadLimit];
                    System.arraycopy(cb, markedChar, ncb, 0, delta);
                    cb = ncb;
                    markedChar = 0;
                    dst = delta;
                }
                nextChar = nChars = delta;
            }
        }

        int n;
        do {
            //从in中读取cb.length-dst个字符到cb偏移量为dst的位置
            n = in.read(cb, dst, cb.length - dst);
        } while (n == 0); //n为0没读到，继续读取,为-1说明已经到stream末尾，终止读取
        if (n > 0) {
            nChars = dst + n; //缓冲区当前字符总数
            nextChar = dst;  //缓冲区下一个可读取的字符位置（dst前的为标记的已读取过的字符）
        }
    }

    /**EM DONE
     * Reads a single character.
     * 从BufferedReader中读取一个字符，该字符以int的方式返回
     * @return The character read, as an integer in the range
     *         0 to 65535 ({@code 0x00-0xffff}), or -1 if the
     *         end of the stream has been reached
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            for (;;) {
                if (nextChar >= nChars) { //说明缓冲区数据已经被读取完毕
                    fill(); //把数据流中数据加载到缓冲区中
                    if (nextChar >= nChars) //依然没有变化，说明stream已经读取完毕
                        return -1;
                }
                if (skipLF) {
                    skipLF = false; //因为只读取一个字符，也只判断一次
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                        continue;
                    }
                }
                return cb[nextChar++];
            }
        }
    }

    /**EM DONE
     * Reads characters into a portion of an array, reading from the underlying
     * stream if necessary.
     * 读取字符到一个字符串数组中，如有必要直接从stream中读取（跳过stream -> buffer这一步骤）
     */
    private int read1(char[] cbuf, int off, int len) throws IOException {
        if (nextChar >= nChars) { //说明缓冲区数据已经处理完毕
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, and if line feeds are not
               being skipped, do not bother to copy the characters into the
               local buffer.  In this way buffered streams will cascade
               harmlessly. */
            /**
             * len >= cb.length : 说明待读取的长度已经超过了缓冲区大小
             * markedChar <= UNMARKED：说明已经不需要处理mark方法
             * ！skipLF ：不需要处理换行符
             * ——> 满足这几个条件则直接从stream中read数据，省略stream -> buffer这一步骤
             */
            if (len >= cb.length && markedChar <= UNMARKED && !skipLF) {
                return in.read(cbuf, off, len);
            }
            //否则fill对缓冲区进行处理
            fill();
        }
        if (nextChar >= nChars) return -1; //fill后无变化直接退出
        if (skipLF) { //处理换行符
            skipLF = false;
            if (cb[nextChar] == '\n') {
                nextChar++;
                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars)
                    return -1;
            }
        }
        int n = Math.min(len, nChars - nextChar); //一次读取的实际是len和缓冲区剩余空间较小的那个长度
        System.arraycopy(cb, nextChar, cbuf, off, n);
        nextChar += n;
        return n;
    }

    /**EM DONE
     * Reads characters into a portion of an array.
     * 将字符读取到数组的一部分中，数组cbuf，偏移量off，长度len
     *
     * （这个和BufferedInputStream好类似哦~）
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
     *
     *  该方法执行Reader类对应的read方法的一般性的实现。 为了更加便利，它尝试通过重复read读取基础流来读取尽可能多的字符。直到：
     *  1、指定的字符数已被读取；
     *  2、底层流的read方法返回-1 ，表示文件结尾
     *  3、底层流的read方法返回false，表示进一步的输入请求将阻塞。(在BufferInputStream中是available方法返回0)
     *
     *  reader ->boolean ready ->判断底层流是否可以读取
     *  InputStream ->int available ->判断底层流是否可以读取
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of characters
     * actually read.
     * 如果基础流上的第一个read返回-1以指示文件结束，则此方法返回-1 。 否则，此方法返回实际读取的字符数。
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many characters as possible in the same fashion.
     * 鼓励使用这个类的子类（尝试以相同的方式读取尽可能多的字符）但不是必需的。
     *
     * <p> Ordinarily this method takes characters from this stream's character
     * buffer, filling it from the underlying stream as necessary.  If,
     * however, the buffer is empty, the mark is not valid, and the requested
     * length is at least as large as the buffer, then this method will read
     * characters directly from the underlying stream into the given array.
     * Thus redundant <code>BufferedReader</code>s will not copy data
     * unnecessarily.
     * 通常情况是从stream读取字符到buffer进行填充。然而满足一下几个条件时：
     * 1、buffer为空
     * 2、mark标志无效
     * 3、请求读取的长度大于等于buffer的大小
     * ——> 会直接从stream读取字符，省略从stream copy char到buffer的步骤
     *
     * @param      cbuf  Destination buffer
     * @param      off   Offset at which to start storing characters
     * @param      len   Maximum number of characters to read
     *
     * @return     The number of characters read, or -1 if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     * @exception  IndexOutOfBoundsException {@inheritDoc}
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        //线程安全
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            //尝试第一次读取
            int n = read1(cbuf, off, len);
            if (n <= 0) return n; //返回-1说明fill后依然没有读取到数据，直接返回
            while ((n < len) && in.ready()) { //循环读取数据
                int n1 = read1(cbuf, off + n, len - n);
                if (n1 <= 0) break;
                n += n1;
            }
            return n;
        }
    }

    /**EM DONE
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     * 读一行文字。 一行被视为由换行符（'\ n'），回车符（'\ r'）中的任何一个或随后的换行符终止。
     *
     * @param      ignoreLF  If true, the next '\n' will be skipped
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached without reading any characters
     *
     * @see        java.io.LineNumberReader#readLine()
     *
     * @exception  IOException  If an I/O error occurs
     *
     * ！！！注意这个方法并没有public修饰！
     */
    String readLine(boolean ignoreLF) throws IOException {
        StringBuffer s = null; //用来保存读取到的字符串
        int startChar;
        //线程安全
        synchronized (lock) {
            ensureOpen();
            boolean omitLF = ignoreLF || skipLF; //忽略换行符（一次）

        bufferLoop:
            for (;;) {

                if (nextChar >= nChars) //缓冲区已满 -> fill一次
                    fill();
                if (nextChar >= nChars) { /* EOF -> end of file :已经读完了*/
                    if (s != null && s.length() > 0)
                        return s.toString();
                    else
                        return null;
                }
                boolean eol = false; //eol -> end of line：是否读取到一行末尾的标志
                char c = 0;
                int i;

                /* Skip a leftover '\n', if necessary 只忽略一次！！*/
                if (omitLF && (cb[nextChar] == '\n'))
                    nextChar++;
                skipLF = false;
                omitLF = false;
            /**
             *  charLoop循环就是为了确认是否是eol，也即读取的终点。
             *  有两种情况会终止charLoop：
             *  1、已经读取到了一行的末尾：(c == '\n') || (c == '\r')
             *      ——这种情况eol为true，也即不用处理缓冲区即可完成读取
             *  2、虽然没有到末尾，但是已经到缓冲区末尾（！i < nChars）
             *      ——这种情况需要读取完缓冲区内容。后续更新nextChar = i -> nextChar >= nChars,进行外层bufferLoop大循环fill()
             */
            charLoop:
                for (i = nextChar; i < nChars; i++) {
                    c = cb[i];
                    if ((c == '\n') || (c == '\r')) {
                        eol = true;
                        break charLoop;
                    }
                }

                startChar = nextChar;
                nextChar = i; //本次charLoop确定的可以读取的内容：startChar -至-> nextChar

                if (eol) {
                    String str;
                    if (s == null) {
                        str = new String(cb, startChar, i - startChar);
                    } else {
                        s.append(cb, startChar, i - startChar);
                        str = s.toString();
                    }
                    nextChar++; //返回的长度只有i - startChar，最后一位换行符并不会读取到！但是需要跳过，所以这里nextChar++
                    if (c == '\r') {
                        skipLF = true;
                    }
                    return str;
                } //eol为true：读取完毕

                if (s == null)
                    s = new StringBuffer(defaultExpectedLineLength); //第一次初始化
                s.append(cb, startChar, i - startChar); //非最后一次，需要度去玩缓冲区字符串
            }
        }
    }

    /**EM DONE
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     * 读一行文字。 一行被视为由换行符（'\ n'），回车符（'\ r'）中的任何一个或随后的换行符终止。
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached without reading any characters
     *
     * @exception  IOException  If an I/O error occurs
     *
     * @see java.nio.file.Files#readAllLines
     *
     * public-> 默认是不会跳过换行符的
     */
    public String readLine() throws IOException {
        return readLine(false);
    }

    /**EM DONE
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
        //保证线程安全
        synchronized (lock) {
            ensureOpen();
            long r = n;
            while (r > 0) {
                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars) /* EOF */
                    break;
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                    }
                }
                long d = nChars - nextChar;
                if (r <= d) { //说明跳过预计长度的字符，不超过缓冲区可读字符串大小
                    nextChar += r;
                    r = 0;
                    break;
                }
                else { //缓冲区剩余字符不够skip的，skip完后r > 0，循环 --> nextChar >= nChars --> fill再处理
                    r -= d;
                    nextChar = nChars;
                }
            }
            return n - r; //实际skip的字符个数
        }
    }

    /**EM DONE
     * Tells whether this stream is ready to be read.  A buffered character
     * stream is ready if the buffer is not empty, or if the underlying
     * character stream is ready.
     *
     * 告诉这个流是否准备好被读取。 如果缓冲区不为空，或者底层字符流准备就绪，则缓冲字符流就绪。
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();

            /*
             * If newline needs to be skipped and the next char to be read
             * is a newline character, then just skip it right away.
             * 处理换行符
             */
            if (skipLF) {
                /* Note that in.ready() will return true if and only if the next
                 * read on the stream will not block.
                 * 只有针对stream的读取不会被屏蔽的情况下，in.ready()才会返回true
                 */
                if (nextChar >= nChars && in.ready()) {
                    fill();
                }
                if (nextChar < nChars) {
                    if (cb[nextChar] == '\n')
                        nextChar++;
                    skipLF = false;
                }
            }
            return (nextChar < nChars) || in.ready();
        }
    }

    /**EM DONE
     * Tells whether this stream supports the mark() operation, which it does.
     */
    public boolean markSupported() {
        return true;
    }

    /**EM DONE
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.
     * 标记流中的当前位置。 对reset（）的后续调用将尝试将流重新定位到此位置。
     *
     * @param readAheadLimit   Limit on the number of characters that may be
     *                         read while still preserving the mark. An attempt
     *                         to reset the stream after reading characters
     *                         up to this limit or beyond may fail.
     *                         A limit value larger than the size of the input
     *                         buffer will cause a new buffer to be allocated
     *                         whose size is no smaller than limit.
     *                         Therefore large values should be used with care.
     *     readAheadLimit：    保留标记时仍可以读取的限制字符数。
     *                         在读取字符到此限制或超出之后，尝试重新设置流可能会失败。
     *                         大于输入缓冲区大小的限制值将导致新的缓冲区被分配，其大小不小于limit。(fill) 因此，应谨慎使用大量值
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

    /**EM DONE
     * Resets the stream to the most recent mark.
     * reset到最近一次mark时的读取状态
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

    //关闭数据流
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

    /**EM DONE
     * Returns a {@code Stream}, the elements of which are lines read from
     * this {@code BufferedReader}.  The {@link Stream} is lazily populated,
     * i.e., read only occurs during the
     * <a href="../util/stream/package-summary.html#StreamOps">terminal
     * stream operation</a>.
     * 返回一个Stream ，其元素是从这个BufferedReader读取的行。
     * Stream是懒加载的方式，即“read”动作只发生在terminal stream operation期间。
     *
     * <p> The reader must not be operated on during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     * 在执行终端流操作期间，reader不能被操作。 否则，终端流操作的结果未定义。(可以参考stream的概念)
     *
     * <p> After execution of the terminal stream operation there are no
     * guarantees that the reader will be at a specific position from which to
     * read the next character or line.
     * 在执行终端流操作之后，不能保证读reader将处于从其读取下一个字符或行的特定位置。
     *
     * <p> If an {@link IOException} is thrown when accessing the underlying
     * {@code BufferedReader}, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the {@code Stream}
     * method that caused the read to take place. This method will return a
     * Stream if invoked on a BufferedReader that is closed. Any operation on
     * that stream that requires reading from the BufferedReader after it is
     * closed, will cause an UncheckedIOException to be thrown.
     * 如果在访问底层BufferedReader时抛出BufferedReader ，它将被包裹在UncheckedIOException中 ，这将从导致读取的Stream方法抛出。
     * 如果在关闭的BufferedReader中调用该方法，则此方法将返回一个Stream。
     * 该流的任何操作需要在关闭之后从BufferedReader进行读取，将导致抛出UncheckedIOException异常。
     *
     * @return a {@code Stream<String>} providing the lines of text
     *         described by this {@code BufferedReader}
     *
     * @since 1.8
     */
    public Stream<String> lines() {
        Iterator<String> iter = new Iterator<>() {
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
