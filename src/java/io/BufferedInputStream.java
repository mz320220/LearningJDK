/*
 * Copyright (c) 1994, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;

/**
 * A <code>BufferedInputStream</code> adds
 * functionality to another input stream-namely,
 * the ability to buffer the input and to
 * support the <code>mark</code> and <code>reset</code>
 * methods. When  the <code>BufferedInputStream</code>
 * is created, an internal buffer array is
 * created. As bytes  from the stream are read
 * or skipped, the internal buffer is refilled
 * as necessary  from the contained input stream,
 * many bytes at a time. The <code>mark</code>
 * operation  remembers a point in the input
 * stream and the <code>reset</code> operation
 * causes all the  bytes read since the most
 * recent <code>mark</code> operation to be
 * reread before new bytes are  taken from
 * the contained input stream.
 *
 * A BufferedInputStream为另一个输入流添加了功能，即：1、缓冲输入，2、支持mark和reset方法的功能。
 * 当创建BufferedInputStream时，将创建一个内部缓冲区数组。 当从流中读取或跳过字节时，内部缓冲区将根据需要从所包含的输入流中重新填充，一次有多个字节。
 * mark操作会记住输入流中的一点；
 * reset操作会导致从最近的mark操作之后读取的所有字节被重新读取，在从包含的输入流中取出新的字节之前。
 * @author  Arthur van Hoff
 * @since   1.0
 */
public
class BufferedInputStream extends FilterInputStream {

    //EM DONE:默认buffer缓冲区大小
    private static int DEFAULT_BUFFER_SIZE = 8192;

    /**EM DONE
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     * 允许分配的最大缓冲区大小，一些虚拟机会保留一些字段作为头字节。
     * 尝试分配超过该大小的内存可能导致OutOfMemoryError，请求数组大小超过虚拟机限制。
     */
    private static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**EM DONE
     * As this class is used early during bootstrap, it's motivated to use
     * Unsafe.compareAndSetObject instead of AtomicReferenceFieldUpdater
     * (or VarHandles) to reduce dependencies and improve startup time.
     * 由于这个类在引导过程的早期就被使用了，所以鼓励使用unsafe.compareandsetObject而不是AtomicReferenceFieldDupdater（或varhandles），
     * 以减少依赖性并提高启动时间。
     *
     */
    private static final Unsafe U = Unsafe.getUnsafe();

    //获取给定名称的字段在其类的存储分配中的位置。
    private static final long BUF_OFFSET
            = U.objectFieldOffset(BufferedInputStream.class, "buf");

    /**EM DONE
     * The internal buffer array where the data is stored. When necessary,
     * it may be replaced by another array of
     * a different size.
     * 存储数据的内部缓冲区数组。
     * 必要时，可以用另一个大小不同的数组替换
     */
    /*
     * We null this out with a CAS on close(), which is necessary since
     * closes can be asynchronous. We use nullness of buf[] as primary
     * indicator that this stream is closed. (The "in" field is also
     * nulled out on close.)
     * 我们在close（）上使用一个cas(compare and swap)将其清空，这是必需的，因为close可以是异步的。
     * 我们使用buf[]的null属性（也即buf[]为null）作为该流已关闭的标志。（关闭时，“In”字段也为null。）
     * 注意使用了volatile修饰
     */
    protected volatile byte[] buf;

    /**EM DONE
     * The index one greater than the index of the last valid byte in
     * the buffer.
     * 索引一大于缓冲区中最后一个有效字节的索引。 -> 缓冲区中有效字节数（不是输入流中的）
     *
     * This value is always
     * in the range <code>0</code> through <code>buf.length</code>;
     * elements <code>buf[0]</code>  through <code>buf[count-1]
     * </code>contain buffered input data obtained
     * from the underlying  input stream.
     * 1、count值在0-buf.length之间
     * 2、buf[0]-buf[count-1]包含了从基础数据中获取的所有缓冲数据
     */
    protected int count;

    /**EM DONE
     * The current position in the buffer. This is the index of the next
     * character to be read from the <code>buf</code> array.
     * 缓冲区中的当前位置。 ——也是将被从缓冲区读取的下一个字符的索引
     *
     * <p>
     * This value is always in the range <code>0</code>
     * through <code>count</code>. If it is less
     * than <code>count</code>, then  <code>buf[pos]</code>
     * is the next byte to be supplied as input;
     * if it is equal to <code>count</code>, then
     * the  next <code>read</code> or <code>skip</code>
     * operation will require more bytes to be
     * read from the contained  input stream.
     * 1、pos的值始终在0-count之间；
     * 2、如果pos小于count，则buf[pos]即为下一个可被作为输入的字节。（可读取的下一个字节）
     *    如果pos等于count，那么下一个read、skip操作需要从基础输入流中获取更多的字节。
     * @see     java.io.BufferedInputStream#buf
     */
    protected int pos;

    /**EM DONE
     * The value of the <code>pos</code> field at the time the last
     * <code>mark</code> method was called.
     * pos字段在最后一个 mark方法被调用时的值。
     *
     * <p>
     * This value is always
     * in the range <code>-1</code> through <code>pos</code>.
     * If there is no marked position in  the input
     * stream, this field is <code>-1</code>. If
     * there is a marked position in the input
     * stream,  then <code>buf[markpos]</code>
     * is the first byte to be supplied as input
     * after a <code>reset</code> operation. If
     * <code>markpos</code> is not <code>-1</code>,
     * then all bytes from positions <code>buf[markpos]</code>
     * through  <code>buf[pos-1]</code> must remain
     * in the buffer array (though they may be
     * moved to  another place in the buffer array,
     * with suitable adjustments to the values
     * of <code>count</code>,  <code>pos</code>,
     * and <code>markpos</code>); they may not
     * be discarded unless and until the difference
     * between <code>pos</code> and <code>markpos</code>
     * exceeds <code>marklimit</code>.
     * 1、markpos的值在-1和pos之间。
     * 2、没有marked的位置，则markpos为-1
     *    如果有marked的位置，那buf[markpos]是执行reset后可以被读取的第一个字节。
     * 3、如果markpos不等于-1（有marked位置）所有buf[markpos]到buf[pos-1]字节都必须保留在缓冲区数组中。
     *   （即使可能会被移动到缓冲区的其他位置 ——通过合理的调整count、pos、markpos的值）
     *   且不得被丢弃除非pos和markpos之前的差值超过marklimit。
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#pos
     */
    protected int markpos = -1;

    /**EM DONE
     * The maximum read ahead allowed after a call to the
     * <code>mark</code> method before subsequent calls to the
     * <code>reset</code> method fail.
     * mark方法调用后，后续调用 reset方法失败之前允许的，最大超前读。
     *
     * Whenever the difference between <code>pos</code>
     * and <code>markpos</code> exceeds <code>marklimit</code>,
     * then the  mark may be dropped by setting
     * <code>markpos</code> to <code>-1</code>.
     * 当pos 与 markpos差异＞marklimit，mark方法会被舍弃（重置markpos的值为-1）
     *
     * @see     java.io.BufferedInputStream#mark(int)
     * @see     java.io.BufferedInputStream#reset()
     */
    protected int marklimit;

    /**EM DONE
     * Check to make sure that underlying input stream has not been
     * nulled out due to close; if not return it;
     * 确认基础的输入流不为空，空抛出IOException，否则返回InputStream
     */
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in; //见FilterInputStream.in
        if (input == null)
            throw new IOException("Stream closed");
        return input;
    }

    /**EM DONE
     * Check to make sure that buffer has not been nulled out due to
     * close; if not return it;
     * 确认缓冲区数组buf不为null，null说明stream已经关闭抛出IO异常，否则返回buf
     */
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null)
            throw new IOException("Stream closed");
        return buffer;
    }

    /**EM DONE
     * Creates a <code>BufferedInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use. An internal
     * buffer array is created and  stored in <code>buf</code>.
     * 创建一个默认大小的BufferedInputStream并保存其参数，输入流 in ，供以后使用。 一个内部缓冲区会被创建并保存在buf字节数组中。
     *
     * @param   in   the underlying input stream.
     */
    public BufferedInputStream(InputStream in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    /**EM DONE
     * Creates a <code>BufferedInputStream</code>
     * with the specified buffer size,
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.  An internal
     * buffer array of length  <code>size</code>
     * is created and stored in <code>buf</code>.
     *  创建一个指定大小的BufferedInputStream并保存其参数，输入流 in ，供以后使用。
     *  一个大小为size的内部缓冲区会被创建并保存在buf字节数组中
     *
     * @param   in     the underlying input stream.
     * @param   size   the buffer size.
     * @exception IllegalArgumentException if {@code size <= 0}.
     */
    public BufferedInputStream(InputStream in, int size) {
        super(in); //父类FilterInputStream构造方法
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    /**EM DONE
     * Fills the buffer with more data, taking into account
     * shuffling and other tricks for dealing with marks.
     * Assumes that it is being called by a synchronized method.
     * This method also assumes that all data has already been read in,
     * hence pos > count.
     *
     * 在缓冲区中填充更多的数据，同时考虑到account重组和其他处理mark的技巧。
     * ——假设它是由同步方法调用的，且假设此方法还所有数据都已被读取，因此pos>count。
     */
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        //没有mark标志直接舍弃缓冲区数据。（重新开始准备读取）pos=0
        if (markpos < 0)
            pos = 0;
        //pos已经到达buffer最大位置（buffer中已经没有空间，才需要进行下面的清理、扩容等处理）
        else if (pos >= buffer.length)
            //存在mark操作，则mark之前的数据可以throw away：0*******markpos-------pos/buffer.length
            if (markpos > 0) {
                int sz = pos - markpos;
                //从buffer的markpos处复制sz个元素到buffer的0处，保留了markpos到pos之前的数据
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            /**
             * ATTENTION:这个地方需要和mark L490方法配合理解。
             * 在使用mark方法后markpos才会!=-1，
             * 在使用mark方法时，才会对marklimit进行初始化：才会走到下面的if条件中去（否则在直接markpos<0条件下进行处理）
             *
             * markpos不小于0 && markpos不大于0，所以：markpos == 0 && buffer.length >= marklimit
             * 走到这个条件说明：当前buffer已经全部读取完毕（pos >= buffer.length）
             *                   且mark方法后读取的数据、也即buffer长度>=marklimit，依然没有reset这个时候：
             * -> 直接舍弃，不再留存直接drop重新读取。
             *
             * ！！！！这里可以发现并不是读取的数据超过marklimit后直接重置markpos，而是等到buffer读取完毕后。
             * 可以参考：https://blog.csdn.net/dengjili/article/details/79416467
             */
            } else if (buffer.length >= marklimit) {
                markpos = -1;   /* buffer got too big, invalidate mark */
                pos = 0;        /* drop buffer contents */
            } else if (buffer.length >= MAX_BUFFER_SIZE) {
                throw new OutOfMemoryError("Required array size too large");
            //grow buffer，对buffer进行扩容（markpos==0 && buffer.length < marklimit && buffer.length < MAX_BUFFER_SIZE）
            } else {
                int nsz = (pos <= MAX_BUFFER_SIZE - pos) ?
                        pos * 2 : MAX_BUFFER_SIZE; //pos是否小于MAX_BUFFER_SIZE-pos，小于则*2，否则直接以MAX_BUFFER_SIZE为准
                if (nsz > marklimit)
                /** 下面的操作说明：marklimit始终是marklimit和buffer.size()中较大的那一个**/
                nsz = marklimit;
                byte[] nbuf = new byte[nsz];
                //移动buffer[0-pos]到nbuf[0]位置处
                System.arraycopy(buffer, 0, nbuf, 0, pos);
                //用buffer与this.BUF_OFFSET处进行比较，一致则更新buffer为nbuf
                if (!U.compareAndSetObject(this, BUF_OFFSET, buffer, nbuf)) {
                    // Can't replace buf if there was an async close.
                    // Note: This would need to be changed if fill()
                    // is ever made accessible to multiple threads.
                    // But for now, the only way CAS can fail is via close.
                    // assert buf == null;
                    throw new IOException("Stream closed");
                }
                buffer = nbuf;
            }
        //开始读取
        count = pos;
        /**
         * 见InputStream.read()，从stream中向buffer中读取，有pos开始，长度为buffer.length-pos;
         * n:表示读取的byte数量
         */
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos);
        if (n > 0)
            count = n + pos;
    }

    /**EM DONE
     * See
     * the general contract of the <code>read</code>
     * method of <code>InputStream</code>.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     * @see        java.io.FilterInputStream#in
     *
     * 读取Date的下一个字节，如果stream读取完毕返回-1
     */
    public synchronized int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count)
                return -1;
        }
        return getBufIfOpen()[pos++] & 0xff; //相当于buffer[pos++] & 0xff;
    }

    /**
     * Read characters into a portion of an array, reading from the underlying
     * stream at most once if necessary.(如有必要最多一次)
     * 将缓冲区buffer中的数据写到字节数组b中。off是字节数组b的起始位置，len是写入长度
     */
    private int read1(byte[] b, int off, int len) throws IOException {
        int avail = count - pos;
        //avail <=0 说明缓冲区已满
        if (avail <= 0) {
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, do not bother to copy the
               bytes into the local buffer.  In this way buffered streams will
               cascade harmlessly. */
            /**
             * 加速机制。
             * 如果读取的长度大于缓冲区的长度 && 没有mark操作
             * 则直接从原始输入流中进行读取，从而避免无谓的COPY（从原始输入流至缓冲区，读取缓冲区全部数据，清空缓冲区，
             * 重新填入原始输入流数据）
             */
            if (len >= getBufIfOpen().length && markpos < 0) {
                return getInIfOpen().read(b, off, len);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) return -1;
        }
        int cnt = (avail < len) ? avail : len;
        //从getBufIfOpen()的pos处复制cnt个数据到pos的b处
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }

    /**EM DONE
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     * 从给定的偏移开始，将字节输入流中的字节读入指定的字节数组。
     *
     * <p> This method implements the general contract of the corresponding
     * <code>{@link InputStream#read(byte[], int, int) read}</code> method of
     * the <code>{@link InputStream}</code> class.  As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the <code>read</code> method of the underlying stream.  This
     * iterated <code>read</code> continues until one of the following
     * conditions becomes true: <ul>
     *
     *   <li> The specified number of bytes have been read,
     *
     *   <li> The <code>read</code> method of the underlying stream returns
     *   <code>-1</code>, indicating end-of-file, or
     *
     *   <li> The <code>available</code> method of the underlying stream
     *   returns zero, indicating that further input requests would block.
     *
     * 该方法执行InputStream类对应的read方法的一般性的实现。 为了更加便利，它尝试通过重复read基础流来读取尽可能多的字节。
     * read方法会持续迭代，直到：
     * 1、指定的字节数已被读取；
     * 2、底层流的read方法返回-1 ，表示文件结尾
     * 3、底层流的available方法返回0，表示进一步的输入请求将阻塞。
     *
     * </ul> If the first <code>read</code> on the underlying stream returns
     * <code>-1</code> to indicate end-of-file then this method returns
     * <code>-1</code>.  Otherwise this method returns the number of bytes
     * actually read.
     * 如果基础流上的第一个read返回-1以指示文件结束，则此方法返回-1 。 否则，此方法返回实际读取的字节数。
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many bytes as possible in the same fashion.
     * 鼓励使用这个类的子类（尝试以相同的方式读取尽可能多的字节）但不是必需的。
     *
     * @param      b     destination buffer.
     * @param      off   offset at which to start storing bytes.
     * @param      len   maximum number of bytes to read.
     * @return     the number of bytes read, or <code>-1</code> if the end of
     *             the stream has been reached.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     */
    public synchronized int read(byte b[], int off, int len)
        throws IOException
    {
        getBufIfOpen(); // Check for closed stream
        /**
         * 如果需要正确读取则需要：
         *  off > 0
         *  len > 0
         *  b.length - (off + len) > 0
         */
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (;;) {
            /**
             * 受制于count - pos，read1()方法一次并不一定能够读完，先尝试尽可能读：read1(b, off , len);
             * nread <=0 ,说明最后一次read1已经读取完毕。
             *      ——n==0，说明第一次读已经读取完毕，则nread即为读取到的字节个数
             *      ——否则说明经过多次读取，n为实际读取到的字节数
             */
            int nread = read1(b, off + n, len - n);
            if (nread <= 0)
                return (n == 0) ? nread : n;
            /**如果一次没有读取完毕则把读取到的字节长度累计到n上*/
            n += nread;
            /**如果已经读取到len长度，则不必进行下一次循环*/
            if (n >= len)
                return n;
            // if not closed but no bytes available, return
            InputStream input = in;
            if (input != null && input.available() <= 0)
                return n;
        }
    }

    /**EM DONE
     * See the general contract of the <code>skip</code>
     * method of <code>InputStream</code>.
     *
     * @throws IOException  if this input stream has been closed by
     *                      invoking its {@link #close()} method,
     *                      {@code in.skip(n)} throws an IOException,
     *                      or an I/O error occurs.
     */
    public synchronized long skip(long n) throws IOException {
        getBufIfOpen(); // Check for closed stream
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {   //小于0说明缓冲区已满
            // If no mark position set then don't keep in buffer
            //没有markpos<0，无mark那么直接操作stream进行skip
            if (markpos <0)
            /**
             * 查看InputStream源码的skip可知，实际上是把stream需要skip的字节读取到一个无用的skipbuffer中。
             * 本处可能会抛出异常！！如stream不在available
             */
            return getInIfOpen().skip(n);

            // Fill in buffer to save bytes for reset
            fill();
            avail = count - pos;
            if (avail <= 0)
                return 0;
        }
        //实际可以skipped为avail和n中较小的那个长度；
        long skipped = (avail < n) ? avail : n;
        //直接移动pos实现skip
        pos += skipped;
        return skipped;
    }

    /**EM DONE
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation might be
     * the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * This method returns the sum of the number of bytes remaining to be read in
     * the buffer (<code>count&nbsp;- pos</code>) and the result of calling the
     * {@link java.io.FilterInputStream#in in}.available().
     *
     * 返回从该输入流中可以读取（或跳过）的字节数的估计值，而不会被下一次调用此输入流的方法阻塞。
     * 下一个调用可能是同一个线程或另一个线程。单次读取或跳过该方法大小的字节，将不会被阻塞，但可能会读取或跳过较少的字节（少于上次预估的）。
     *
     * @return     an estimate of the number of bytes that can be read (or skipped
     *             over) from this input stream without blocking.
     * @exception  IOException  if this input stream has been closed by
     *                          invoking its {@link #close()} method,
     *                          or an I/O error occurs.
     */
    public synchronized int available() throws IOException {
        int n = count - pos;
        int avail = getInIfOpen().available();  //不抛出异常则avail始终为0
        return n > (Integer.MAX_VALUE - avail)
                    ? Integer.MAX_VALUE
                    : n + avail;
    }

    /**EM DONE
     * See the general contract of the <code>mark</code>
     * method of <code>InputStream</code>.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.BufferedInputStream#reset()
     *
     * 这个方法最重要的是初始化marklimit，在fill()方法整理、buffer时有重要作用
     */
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    /**EM DONE
     * See the general contract of the <code>reset</code>
     * method of <code>InputStream</code>.
     * <p>
     * If <code>markpos</code> is <code>-1</code>
     * (no mark has been set or the mark has been
     * invalidated), an <code>IOException</code>
     * is thrown. Otherwise, <code>pos</code> is
     * set equal to <code>markpos</code>.
     * 1、markpost=-1，抛出IOException
     * 2、否则将pos回退到markpos处
     *
     * @exception  IOException  if this stream has not been marked or,
     *                  if the mark has been invalidated, or the stream
     *                  has been closed by invoking its {@link #close()}
     *                  method, or an I/O error occurs.
     * @see        java.io.BufferedInputStream#mark(int)
     */
    public synchronized void reset() throws IOException {
        getBufIfOpen(); // Cause exception if closed
        if (markpos < 0)
            throw new IOException("Resetting to invalid mark");
        pos = markpos;
    }

    /**EM DONE
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods. The <code>markSupported</code>
     * method of <code>BufferedInputStream</code> returns
     * <code>true</code>.
     *
     * @return  a <code>boolean</code> indicating if this stream type supports
     *          the <code>mark</code> and <code>reset</code> methods.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return true;
    }

    /**EM DONE
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * Once the stream has been closed, further read(), available(), reset(),
     * or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     * 关闭inputstream和相关的系统资源。
     * 关闭后再对其调用read、available、reset、skip都将抛出IOException
     * 支持幂等，多次close无影响
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        byte[] buffer;
        while ( (buffer = buf) != null) {
            if (U.compareAndSetObject(this, BUF_OFFSET, buffer, null)) {
                InputStream input = in;
                //通过置为null的方式进行关闭，可见L198 getInIfOpen()方法
                in = null;
                if (input != null)
                    input.close();
                return;
            }
            // Else retry in case a new buf was CASed in fill()
        }
    }
}
