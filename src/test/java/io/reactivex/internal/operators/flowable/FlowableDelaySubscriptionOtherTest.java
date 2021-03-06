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
package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableDelaySubscriptionOtherTest {
    @Test
    public void testNoPrematureSubscription() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testNoMultipleSubscriptions() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);
        other.onNext(2);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testCompleteTriggersSubscription() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onComplete();

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testNoPrematureSubscriptionToError() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.<Integer>error(new TestException())
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onComplete();

        Assert.assertEquals("No subscription", 1, subscribed.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void testNoSubscriptionIfOtherErrors() {
        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.<Integer>error(new TestException())
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onError(new TestException());

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void testBackpressurePassesThrough() {

        PublishProcessor<Object> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        final AtomicInteger subscribed = new AtomicInteger();

        Flowable.just(1, 2, 3, 4, 5)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.getAndIncrement();
            }
        })
        .delaySubscription(other)
        .subscribe(ts);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        Assert.assertEquals("Premature subscription", 0, subscribed.get());

        other.onNext(1);

        Assert.assertEquals("No subscription", 1, subscribed.get());

        Assert.assertFalse("Not unsubscribed from other", other.hasSubscribers());

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertNoValues();

        ts.request(1);
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(2);
        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(10);
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void unsubscriptionPropagatesBeforeSubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.delaySubscription(other).subscribe(ts);

        Assert.assertFalse("source subscribed?", source.hasSubscribers());
        Assert.assertTrue("other not subscribed?", other.hasSubscribers());

        ts.dispose();

        Assert.assertFalse("source subscribed?", source.hasSubscribers());
        Assert.assertFalse("other still subscribed?", other.hasSubscribers());
    }

    @Test
    public void unsubscriptionPropagatesAfterSubscribe() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> other = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.delaySubscription(other).subscribe(ts);

        Assert.assertFalse("source subscribed?", source.hasSubscribers());
        Assert.assertTrue("other not subscribed?", other.hasSubscribers());

        other.onComplete();

        Assert.assertTrue("source not subscribed?", source.hasSubscribers());
        Assert.assertFalse("other still subscribed?", other.hasSubscribers());

        ts.dispose();

        Assert.assertFalse("source subscribed?", source.hasSubscribers());
        Assert.assertFalse("other still subscribed?", other.hasSubscribers());
    }

    @Test
    public void delayAndTakeUntilNeverSubscribeToSource() {
        PublishProcessor<Integer> delayUntil = PublishProcessor.create();
        PublishProcessor<Integer> interrupt = PublishProcessor.create();
        final AtomicBoolean subscribed = new AtomicBoolean(false);

        Flowable.just(1)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscribed.set(true);
            }
        })
        .delaySubscription(delayUntil)
        .takeUntil(interrupt)
        .subscribe();

        interrupt.onNext(9000);
        delayUntil.onNext(1);

        Assert.assertFalse(subscribed.get());
    }

    @Test(expected = NullPointerException.class)
    public void otherNull() {
        Flowable.just(1).delaySubscription((Flowable<Integer>)null);
    }
}