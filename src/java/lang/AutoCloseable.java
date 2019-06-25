/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * An object that may hold resources (such as file or socket handles)
 * until it is closed. The {@link #close()} method of an {@code AutoCloseable}
 * object is called automatically when exiting a {@code
 * try}-with-resources block for which the object has been declared in
 * the resource specification header. This construction ensures prompt
 * release, avoiding resource exhaustion exceptions and errors that
 * may otherwise occur.
 * 可以保存资源的对象（如文件或套接字句柄），直到它关闭。
 * 在退出已在资源规范头中的try -with-resources块时自动调用 -> AutoCloseable对象的close()方法
 * tyr-with这种结构确保（资源）迅速释放，避免资源耗尽异常和可能发生的错误。（会自动调用close）
 *
 * @apiNote
 * <p>It is possible, and in fact common, for a base class to
 * implement AutoCloseable even though not all of its subclasses or
 * instances will hold releasable resources.  For code that must operate
 * in complete generality, or when it is known that the {@code AutoCloseable}
 * instance requires resource release, it is recommended to use {@code
 * try}-with-resources constructions. However, when using facilities such as
 * {@link java.util.stream.Stream} that support both I/O-based and
 * non-I/O-based forms, {@code try}-with-resources blocks are in
 * general unnecessary when using non-I/O-based forms.
 * 基类实现AutoCloseable是可能的，实际上比较普遍的，即使并不不是它所有的子类或实例都持有可释放的资源。
 * 对于必须以完全一般性操作的代码，或者当知道AutoCloseable实例需要资源释放时，建议使用try资源结构。
 * 然而，在使用一些例如Stream同时支持IO和NIO的组件，在NIO模式下tyr-with并非必要。
 *
 * EM DONE
 *
 * @author Josh Bloch
 * @since 1.7
 */
public interface AutoCloseable {
    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * 关闭此资源，放弃任何潜在资源。
     * try -with-resources语句管理的对象会自动调用此方法。
     *
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * 然这个接口方法被声明为抛出（一般的）异常 ，强烈建议实施者
     *  1、声明close方法的具体实现来抛出更多特定的异常(如IOException等)
     *  2、如果关闭操作不能失败，则不要抛出任何异常。（吃掉并打印日志）
     *
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * 由于关闭操作可能失败的情况需要执行人员的注意。 强烈建议
     * ——放弃基础资源，并将在内部标记资源内部标记为已关闭(资源)，然后再投出异常。
     * close方法不太可能被多次调用，所以这个特性可以：
     * 1、确保资源及时释放；
     * 2、该特性减少了当资源包裹或被另一资源包装时可能出现的问题。
     *
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * 强烈建议这个接口的实现者不要使用close方法抛出InterruptedException；
     * InterruptedException异常与线程的中断状态相互作用，如果InterruptedException为suppressed，则可能会发生运行时行为不当 。
     *
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * 更普遍的来说，如果会导致异常被抑制的问题， 那AutoCloseable.close方法不应该抛出该类型的异常。
     *
     * <p>Note that unlike the {@link java.io.Closeable#close close}
     * method of {@link java.io.Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * 请注意：不像java.ioCloseable的close方法，AutoCloseable的close方法不需要为幂等(幂等：多次调用结果相同)。
     * 换句话说，多次调用AutoCloseable的close方法可能会有一些可见的副作用，而不像Closeable.close，多次调用Closeable.close，不会有任何影响。
     *
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     * （说了一大堆）但是，强烈建议这个界面的实现者使他们的close方法是幂等的。（真无语，那就实现AutoCloseable的子类Closeable就好啦）
     *
     * @throws Exception if this resource cannot be closed
     */
    void close() throws Exception;
}
