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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.operators.observable.ObservableReplay.*;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.PublishSubject;

public class ObservableReplayTest {
    @Test
    public void testBufferedReplay() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedWindowReplay() {
        PublishSubject<Integer> source = PublishSubject.create();
        TestScheduler scheduler = new TestScheduler();
        ConnectableObservable<Integer> co = source.replay(3, 100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onNext(5);
            scheduler.advanceTimeBy(90, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(4);

            inOrder.verify(observer1, times(1)).onNext(5);

            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(5);
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplay() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testReplaySelector() {
        final Function<Integer, Integer> dbl = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * 2;
            }

        };

        Function<Observable<Integer>, Observable<Integer>> selector = new Function<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector);

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

    }

    @Test
    public void testBufferedReplaySelector() {

        final Function<Integer, Integer> dbl = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * 2;
            }

        };

        Function<Observable<Integer>, Observable<Integer>> selector = new Function<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 3);

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onComplete();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplaySelector() {

        final Function<Integer, Integer> dbl = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * 2;
            }

        };

        Function<Observable<Integer>, Observable<Integer>> selector = new Function<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler);

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onComplete();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onComplete();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedReplayError() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onError(new RuntimeException("Forced failure"));

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();

        }

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }

    @Test
    public void testWindowedReplayError() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onError(new RuntimeException("Forced failure"));
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();

        }
        {
            Observer<Object> observer1 = TestHelper.mockObserver();
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onComplete();
        }
    }

    @Test
    public void testSynchronousDisconnect() {
        final AtomicInteger effectCounter = new AtomicInteger();
        Observable<Integer> source = Observable.just(1, 2, 3, 4)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) {
                effectCounter.incrementAndGet();
                System.out.println("Sideeffect #" + v);
            }
        });

        Observable<Integer> result = source.replay(
        new Function<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> o) {
                return o.take(2);
            }
        });

        for (int i = 1; i < 3; i++) {
            effectCounter.set(0);
            System.out.printf("- %d -%n", i);
            result.subscribe(new Consumer<Integer>() {

                @Override
                public void accept(Integer t1) {
                    System.out.println(t1);
                }

            }, new Consumer<Throwable>() {

                @Override
                public void accept(Throwable t1) {
                    t1.printStackTrace();
                }
            },
            new Action() {
                @Override
                public void run() {
                    System.out.println("Done");
                }
            });
            assertEquals(2, effectCounter.get());
        }
    }


    /*
     * test the basic expectation of OperatorMulticast via replay
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_UnsubscribeSource() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Action sourceUnsubscribed = mock(Action.class);
        Observer<Integer> spiedSubscriberBeforeConnect = TestHelper.mockObserver();
        Observer<Integer> spiedSubscriberAfterConnect = TestHelper.mockObserver();

        // Observable under test
        Observable<Integer> source = Observable.just(1,2);

        ConnectableObservable<Integer> replay = source
                .doOnNext(sourceNext)
                .doOnDispose(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .replay();

        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.subscribe(spiedSubscriberBeforeConnect);
        replay.connect();
        replay.subscribe(spiedSubscriberAfterConnect);
        replay.subscribe(spiedSubscriberAfterConnect);

        verify(spiedSubscriberBeforeConnect, times(2)).onSubscribe((Disposable)any());
        verify(spiedSubscriberAfterConnect, times(2)).onSubscribe((Disposable)any());

        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceNext, times(1)).accept(2);
        verify(sourceCompleted, times(1)).run();
        verifyObserverMock(spiedSubscriberBeforeConnect, 2, 4);
        verifyObserverMock(spiedSubscriberAfterConnect, 2, 4);

        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(spiedSubscriberBeforeConnect);
        verifyNoMoreInteractions(spiedSubscriberAfterConnect);

    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribe() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Action sourceUnsubscribed = mock(Action.class);
        final TestScheduler mockScheduler = new TestScheduler();

        Observer<Integer> mockObserverBeforeConnect = TestHelper.mockObserver();
        Observer<Integer> mockObserverAfterConnect = TestHelper.mockObserver();

        // Observable under test
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3)
                .doOnNext(sourceNext)
                .doOnDispose(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect).onSubscribe((Disposable)any());
        verify(mockObserverAfterConnect).onSubscribe((Disposable)any());

        mockScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceNext, times(1)).accept(2);
        verify(sourceNext, times(1)).accept(3);
        verify(sourceCompleted, times(1)).run();
        verifyObserverMock(mockObserverBeforeConnect, 1, 3);
        verifyObserverMock(mockObserverAfterConnect, 1, 3);

        // FIXME not supported
//        verify(spiedWorker, times(1)).isUnsubscribed();
        // FIXME publish calls cancel too
        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    /**
     * Specifically test interaction with a Scheduler with subscribeOn.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue2191_SchedulerUnsubscribeOnError() throws Exception {
        // setup mocks
        Consumer<Integer> sourceNext = mock(Consumer.class);
        Action sourceCompleted = mock(Action.class);
        Consumer<Throwable> sourceError = mock(Consumer.class);
        Action sourceUnsubscribed = mock(Action.class);
        final TestScheduler mockScheduler = new TestScheduler();
        Observer<Integer> mockObserverBeforeConnect = TestHelper.mockObserver();
        Observer<Integer> mockObserverAfterConnect = TestHelper.mockObserver();

        // Observable under test
        Function<Integer, Integer> mockFunc = mock(Function.class);
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(mockFunc.apply(1)).thenReturn(1);
        when(mockFunc.apply(2)).thenThrow(illegalArgumentException);
        ConnectableObservable<Integer> replay = Observable.just(1, 2, 3).map(mockFunc)
                .doOnNext(sourceNext)
                .doOnDispose(sourceUnsubscribed)
                .doOnComplete(sourceCompleted)
                .doOnError(sourceError)
                .subscribeOn(mockScheduler).replay();

        replay.subscribe(mockObserverBeforeConnect);
        replay.connect();
        replay.subscribe(mockObserverAfterConnect);

        verify(mockObserverBeforeConnect).onSubscribe((Disposable)any());
        verify(mockObserverAfterConnect).onSubscribe((Disposable)any());


        mockScheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        // verify interactions
        verify(sourceNext, times(1)).accept(1);
        verify(sourceError, times(1)).accept(illegalArgumentException);
        verifyObserver(mockObserverBeforeConnect, 1, 1, illegalArgumentException);
        verifyObserver(mockObserverAfterConnect, 1, 1, illegalArgumentException);

        // FIXME no longer supported
//        verify(spiedWorker, times(1)).isUnsubscribed();
        // FIXME publish also calls cancel
        verify(sourceUnsubscribed, times(1)).run();

        verifyNoMoreInteractions(sourceNext);
        verifyNoMoreInteractions(sourceCompleted);
        verifyNoMoreInteractions(sourceError);
        verifyNoMoreInteractions(sourceUnsubscribed);
        verifyNoMoreInteractions(mockObserverBeforeConnect);
        verifyNoMoreInteractions(mockObserverAfterConnect);
    }

    private static void verifyObserverMock(Observer<Integer> mock, int numSubscriptions, int numItemsExpected) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onComplete();
        verifyNoMoreInteractions(mock);
    }

    private static void verifyObserver(Observer<Integer> mock, int numSubscriptions, int numItemsExpected, Throwable error) {
        verify(mock, times(numItemsExpected)).onNext((Integer) notNull());
        verify(mock, times(numSubscriptions)).onError(error);
        verifyNoMoreInteractions(mock);
    }

    public static Worker workerSpy(final Disposable mockDisposable) {
        return spy(new InprocessWorker(mockDisposable));
    }


    static class InprocessWorker extends Worker {
        private final Disposable mockDisposable;
        public boolean unsubscribed;

        InprocessWorker(Disposable mockDisposable) {
            this.mockDisposable = mockDisposable;
        }

        @Override
        public Disposable schedule(Runnable action) {
            action.run();
            return mockDisposable; // this subscription is returned but discarded
        }

        @Override
        public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
            action.run();
            return mockDisposable;
        }

        @Override
        public void dispose() {
            unsubscribed = true;
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed;
        }
    }

    @Test
    public void testBoundedReplayBuffer() {
        BoundedReplayBuffer<Integer> buf = new BoundedReplayBuffer<Integer>();
        buf.addLast(new Node(1));
        buf.addLast(new Node(2));
        buf.addLast(new Node(3));
        buf.addLast(new Node(4));
        buf.addLast(new Node(5));

        List<Integer> values = new ArrayList<Integer>();
        buf.collect(values);

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);

        buf.removeSome(2);
        buf.removeFirst();
        buf.removeSome(2);

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        buf.addLast(new Node(5));
        buf.addLast(new Node(6));
        buf.collect(values);

        Assert.assertEquals(Arrays.asList(5, 6), values);

    }

    @Test
    public void testTimedAndSizedTruncation() {
        TestScheduler test = new TestScheduler();
        SizeAndTimeBoundReplayBuffer<Integer> buf = new SizeAndTimeBoundReplayBuffer<Integer>(2, 2000, TimeUnit.MILLISECONDS, test);
        List<Integer> values = new ArrayList<Integer>();

        buf.next(1);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.next(2);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(1, 2), values);

        buf.next(3);
        buf.next(4);
        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(3, 4), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.next(5);

        values.clear();
        buf.collect(values);
        Assert.assertEquals(Arrays.asList(5), values);

        test.advanceTimeBy(2, TimeUnit.SECONDS);
        buf.complete();

        values.clear();
        buf.collect(values);
        Assert.assertTrue(values.isEmpty());

        Assert.assertEquals(1, buf.size);
        Assert.assertTrue(buf.hasCompleted());
    }

    @Test
    public void testColdReplayNoBackpressure() {
        Observable<Integer> source = Observable.range(0, 1000).replay().autoConnect();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        List<Integer> onNextEvents = ts.values();
        assertEquals(1000, onNextEvents.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals((Integer)i, onNextEvents.get(i));
        }
    }

    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(final Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published Observable being executed");
                        observer.onNext("one");
                        observer.onComplete();
                    }
                }).start();
            }
        }).replay().autoConnect();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Consumer<String>() {

            @Override
            public void accept(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }

    @Test
    public void testUnsubscribeSource() throws Exception {
        Action unsubscribe = mock(Action.class);
        Observable<Integer> o = Observable.just(1).doOnDispose(unsubscribe).cache();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        verify(unsubscribe, times(1)).run();
    }

    @Test
    public void testTake() {
        TestObserver<Integer> ts = new TestObserver<Integer>();

        Observable<Integer> cached = Observable.range(1, 100).replay().autoConnect();
        cached.take(10).subscribe(ts);

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        // FIXME no longer assertable
//        ts.assertUnsubscribed();
    }

    @Test
    public void testAsync() {
        Observable<Integer> source = Observable.range(1, 10000);
        for (int i = 0; i < 100; i++) {
            TestObserver<Integer> ts1 = new TestObserver<Integer>();

            Observable<Integer> cached = source.replay().autoConnect();

            cached.observeOn(Schedulers.computation()).subscribe(ts1);

            ts1.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts1.assertNoErrors();
            ts1.assertTerminated();
            assertEquals(10000, ts1.values().size());

            TestObserver<Integer> ts2 = new TestObserver<Integer>();
            cached.observeOn(Schedulers.computation()).subscribe(ts2);

            ts2.awaitTerminalEvent(2, TimeUnit.SECONDS);
            ts2.assertNoErrors();
            ts2.assertTerminated();
            assertEquals(10000, ts2.values().size());
        }
    }
    @Test
    public void testAsyncComeAndGo() {
        Observable<Long> source = Observable.interval(1, 1, TimeUnit.MILLISECONDS)
                .take(1000)
                .subscribeOn(Schedulers.io());
        Observable<Long> cached = source.replay().autoConnect();

        Observable<Long> output = cached.observeOn(Schedulers.computation());

        List<TestObserver<Long>> list = new ArrayList<TestObserver<Long>>(100);
        for (int i = 0; i < 100; i++) {
            TestObserver<Long> ts = new TestObserver<Long>();
            list.add(ts);
            output.skip(i * 10).take(10).subscribe(ts);
        }

        List<Long> expected = new ArrayList<Long>();
        for (int i = 0; i < 10; i++) {
            expected.add((long)(i - 10));
        }
        int j = 0;
        for (TestObserver<Long> ts : list) {
            ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertTerminated();

            for (int i = j * 10; i < j * 10 + 10; i++) {
                expected.set(i - j * 10, (long)i);
            }

            ts.assertValueSequence(expected);

            j++;
        }
    }

    @Test
    public void testNoMissingBackpressureException() {
        final int m = 4 * 1000 * 1000;
        Observable<Integer> firehose = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> t) {
                t.onSubscribe(Disposables.empty());
                for (int i = 0; i < m; i++) {
                    t.onNext(i);
                }
                t.onComplete();
            }
        });

        TestObserver<Integer> ts = new TestObserver<Integer>();
        firehose.replay().autoConnect().observeOn(Schedulers.computation()).takeLast(100).subscribe(ts);

        ts.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertEquals(100, ts.values().size());
    }

    @Test
    public void testValuesAndThenError() {
        Observable<Integer> source = Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .replay().autoConnect();


        TestObserver<Integer> ts = new TestObserver<Integer>();
        source.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNotComplete();
        Assert.assertEquals(1, ts.errors().size());

        TestObserver<Integer> ts2 = new TestObserver<Integer>();
        source.subscribe(ts2);

        ts2.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts2.assertNotComplete();
        Assert.assertEquals(1, ts2.errors().size());
    }

    @Test
    public void unsafeChildThrows() {
        final AtomicInteger count = new AtomicInteger();

        Observable<Integer> source = Observable.range(1, 100)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        })
        .replay().autoConnect();

        TestObserver<Integer> ts = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        source.subscribe(ts);

        Assert.assertEquals(100, count.get());

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test
    public void replayScheduler() {

        Observable.just(1).replay(Schedulers.computation())
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replayTime() {
        Observable.just(1).replay(1, TimeUnit.MINUTES)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeScheduler() {

        Observable.just(1).replay(1, Schedulers.computation())
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySizeAndTime() {
        Observable.just(1).replay(1, 1, TimeUnit.MILLISECONDS)
        .autoConnect()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorSizeScheduler() {
        Observable.just(1).replay(Functions.<Observable<Integer>>identity(), 1, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorScheduler() {
        Observable.just(1).replay(Functions.<Observable<Integer>>identity(), Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void replaySelectorTime() {
        Observable.just(1).replay(Functions.<Observable<Integer>>identity(), 1, TimeUnit.MINUTES)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

}
