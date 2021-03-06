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

package io.reactivex.internal.subscribers;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class SubscriptionLambdaSubscriber<T> implements Subscriber<T>, Subscription {
    final Subscriber<? super T> actual;
    final Consumer<? super Subscription> onSubscribe;
    final LongConsumer onRequest;
    final Action onCancel;

    Subscription s;

    public SubscriptionLambdaSubscriber(Subscriber<? super T> actual,
            Consumer<? super Subscription> onSubscribe,
            LongConsumer onRequest,
            Action onCancel) {
        this.actual = actual;
        this.onSubscribe = onSubscribe;
        this.onCancel = onCancel;
        this.onRequest = onRequest;
    }

    @Override
    public void onSubscribe(Subscription s) {
        // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
        try {
            onSubscribe.accept(s);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            s.cancel();
            RxJavaPlugins.onError(e);
            EmptySubscription.error(e, actual);
            return;
        }
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        actual.onComplete();
    }

    @Override
    public void request(long n) {
        try {
            onRequest.accept(n);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            RxJavaPlugins.onError(e);
        }
        s.request(n);
    }

    @Override
    public void cancel() {
        try {
            onCancel.run();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            RxJavaPlugins.onError(e);
        }
        s.cancel();
    }
}
