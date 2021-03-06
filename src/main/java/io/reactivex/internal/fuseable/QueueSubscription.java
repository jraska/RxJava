/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.internal.fuseable;

import java.util.Queue;

import org.reactivestreams.Subscription;

/**
 * An interface extending Queue and Subscription and allows negotiating
 * the fusion mode between subsequent operators  of the {@code Flowable} base reactive type.
 * <p>
 * The negotiation happens in subscription time when the upstream
 * calls the {@code onSubscribe} with an instance of this interface. The
 * downstream has then the obligation to call {@link #requestFusion(int)}
 * with the appropriate mode before calling {@code request()}.
 * <p>
 * In <b>synchronous fusion</b>, all upstream values are either already available or is generated
 * when {@link #poll()} is called synchronously. When the {@link #poll()} returns null,
 * that is the indication if a terminated stream. Downstream should not call {@link #request(long)}
 * in this mode. In this mode, the upstream won't call the onXXX methods.
 * <p>
 * In <b>asynchronous fusion</b>, upstream values may become available to {@link #poll()} eventually.
 * Upstream signals onError() and onComplete() as usual but onNext may not actually contain
 * the upstream value but have {@code null} instead. Downstream should treat such onNext as indication
 * that {@link #poll()} can be called. In this mode, the downstream still has to call {@link #request(long)}
 * to indicate it is prepared to receive more values.
 * <p>
 * The general rules for consuming the {@link Queue} interface:
 * <ul>
 * <li> {@link #poll()} has to be called sequentially (from within a serializing drain-loop).</li>
 * <li>In addition, callers of {@link #poll()} should be prepared to catch exceptions.</li>
 * <li>Due to how computation attaches to the {@link #poll()}, {@link #poll()} may return
 * {@code null} even if a preceding {@link #isEmpty()} returned false.</li>
 * </ul>
 * <p>
 * Implementations should only allow calling the following methods and the rest of the
 * {@link Queue} interface methods should throw {@link UnsupportedOperationException}:
 * <ul>
 * <li>{@link #poll()}</li>
 * <li>{@link #isEmpty()}</li>
 * <li>{@link #clear()}</li>
 * </ul>
 * @param <T> the value type transmitted through the queue
 */
public interface QueueSubscription<T> extends SimpleQueue<T>, Subscription {
    /**
     * Returned by the {@link #requestFusion(int)} if the upstream doesn't support
     * the requested mode.
     */
    int NONE = 0;

    /**
     * Request a synchronous fusion mode and can be returned by {@link #requestFusion(int)}
     * for an accepted mode.
     * <p>
     * In synchronous fusion, all upstream values are either already available or is generated
     * when {@link #poll()} is called synchronously. When the {@link #poll()} returns null,
     * that is the indication if a terminated stream. Downstream should not call {@link #request(long)}
     * in this mode. In this mode, the upstream won't call the onXXX methods and callers of
     * {@link #poll()} should be prepared to catch exceptions. Note that {@link #poll()} has
     * to be called sequentially (from within a serializing drain-loop).
     */
    int SYNC = 1;

    /**
     * Request an asynchronous fusion mode and can be returned by {@link #requestFusion(int)}
     * for an accepted mode.
     * <p>
     * In asynchronous fusion, upstream values may become available to {@link #poll()} eventually.
     * Upstream signals onError() and onComplete() as usual but onNext may not actually contain
     * the upstream value but have {@code null} instead. Downstream should treat such onNext as indication
     * that {@link #poll()} can be called. Note that {@link #poll()} has to be called sequentially
     * (from within a serializing drain-loop). In addition, callers of {@link #poll()} should be
     * prepared to catch exceptions. In this mode, the downstream still has to call {@link #request(long)}
     * to indicate it is prepared to receive more values.
     */
    int ASYNC = 2;

    /**
     * Request any of the {@link #SYNC} or {@link #ASYNC} modes.
     */
    int ANY = SYNC | ASYNC;

    /**
     * Used in binary or combination with the other constants as an input to {@link #requestFusion(int)}
     * indicating that the {@link #poll()} will be called behind an asynchronous boundary and thus
     * may change the non-trivial computation locations attached to the {@link #poll()} chain of
     * fused operators.
     * <p>
     * For example, fusing map() and observeOn() may move the computation of the map's function over to
     * the thread run after the observeOn(), which is generally unexpected.
     */
    int BOUNDARY = 4;

    /**
     * Request a fusion mode from the upstream.
     * <p>
     * This should be called before {@code onSubscribe} returns or even
     * calls {@link #request(long)}.
     * <p>
     * Calling this method multiple times or after {@code onSubscribe} finished is not allowed
     * and may result in undefined behavior.
     * <p>
     * @param mode the requested fusion mode, allowed values are {@link #SYNC}, {@link #ASYNC},
     * {@link #ANY} combined with {@link #BOUNDARY} (e.g., {@code requestFusion(SYNC | BOUNDARY)}).
     * @return the established fusion mode: {@link #NONE}, {@link #SYNC}, {@link #ASYNC}.
     */
    int requestFusion(int mode);
}
