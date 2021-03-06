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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableFromIterableTest {

    @Test(expected = NullPointerException.class)
    public void testNull() {
        Flowable.fromIterable(null);
    }

    @Test
    public void testListIterable() {
        Flowable<String> observable = Flowable.fromIterable(Arrays.<String> asList("one", "two", "three"));

        Subscriber<String> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    /**
     * This tests the path that can not optimize based on size so must use setProducer.
     */
    @Test
    public void testRawIterable() {
        Iterable<String> it = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    int i;

                    @Override
                    public boolean hasNext() {
                        return i < 3;
                    }

                    @Override
                    public String next() {
                        return String.valueOf(++i);
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

        };
        Flowable<String> observable = Flowable.fromIterable(it);

        Subscriber<String> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("2");
        verify(observer, times(1)).onNext("3");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testObservableFromIterable() {
        Flowable<String> observable = Flowable.fromIterable(Arrays.<String> asList("one", "two", "three"));

        Subscriber<String> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testBackpressureViaRequest() {
        ArrayList<Integer> list = new ArrayList<Integer>(Flowable.bufferSize());
        for (int i = 1; i <= Flowable.bufferSize() + 1; i++) {
            list.add(i);
        }
        Flowable<Integer> o = Flowable.fromIterable(list);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        ts.assertNoValues();
        ts.request(1);

        o.subscribe(ts);

        ts.assertValue(1);
        ts.request(2);
        ts.assertValues(1, 2, 3);
        ts.request(3);
        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.request(list.size());
        ts.assertTerminated();
    }

    @Test
    public void testNoBackpressure() {
        Flowable<Integer> o = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5));

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        ts.assertNoValues();
        ts.request(Long.MAX_VALUE); // infinite

        o.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertTerminated();
    }

    @Test
    public void testSubscribeMultipleTimes() {
        Flowable<Integer> o = Flowable.fromIterable(Arrays.asList(1, 2, 3));

        for (int i = 0; i < 10; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            o.subscribe(ts);

            ts.assertValues(1, 2, 3);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }

    @Test
    public void testFromIterableRequestOverflow() throws InterruptedException {
        Flowable<Integer> o = Flowable.fromIterable(Arrays.asList(1,2,3,4));

        final int expectedCount = 4;
        final CountDownLatch latch = new CountDownLatch(expectedCount);

        o.subscribeOn(Schedulers.computation())
        .subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }

            @Override
            public void onComplete() {
                //ignore
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
                request(Long.MAX_VALUE - 1);
            }});
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testFromEmptyIterableWhenZeroRequestedShouldStillEmitOnCompletedEagerly() {

        final AtomicBoolean completed = new AtomicBoolean(false);

        Flowable.fromIterable(Collections.emptyList()).subscribe(new DefaultSubscriber<Object>() {

            @Override
            public void onStart() {
//                request(0);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Object t) {

            }});
        assertTrue(completed.get());
    }

    @Test
    public void testDoesNotCallIteratorHasNextMoreThanRequiredWithBackpressure() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    int count = 1;

                    @Override
                    public void remove() {
                        // ignore
                    }

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Flowable.fromIterable(iterable).take(1).subscribe();
        assertFalse(called.get());
    }

    @Test
    public void testDoesNotCallIteratorHasNextMoreThanRequiredFastPath() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                        // ignore
                    }

                    int count = 1;

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Flowable.fromIterable(iterable).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // unsubscribe on first emission
                cancel();
            }
        });
        assertFalse(called.get());
    }

    @Test
    public void getIteratorThrows() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new TestException("Forced failure");
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void hasNextThrowsImmediately() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException("Forced failure");
                    }

                    @Override
                    public Integer next() {
                        return null;
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void hasNextThrowsSecondTimeFastpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException("Forced failure");
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void hasNextThrowsSecondTimeSlowpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException("Forced failure");
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5);

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void nextThrowsFastpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new TestException("Forced failure");
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void nextThrowsSlowpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new TestException("Forced failure");
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5);

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void deadOnArrival() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5);
        ts.cancel();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

    }

    @Test
    public void fusionWithConcatMap() {
        TestSubscriber<Integer> to = new TestSubscriber<Integer>();

        Flowable.fromIterable(Arrays.asList(1, 2, 3, 4)).concatMap(
        new Function<Integer, Flowable  <Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return Flowable.range(v, 2);
            }
        }).subscribe(to);

        to.assertValues(1, 2, 2, 3, 3, 4, 4, 5);
        to.assertNoErrors();
        to.assertComplete();
    }
}