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

package io.reactivex.processors;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.TestSubscriber;

public class SerializedProcessorTest {

    @Test
    public void testBasic() {
        SerializedProcessor<String> subject = new SerializedProcessor<String>(PublishProcessor.<String> create());
        TestSubscriber<String> ts = new TestSubscriber<String>();
        subject.subscribe(ts);
        subject.onNext("hello");
        subject.onComplete();
        ts.awaitTerminalEvent();
        ts.assertValue("hello");
    }

    @Test
    public void testAsyncSubjectValueRelay() {
        AsyncProcessor<Integer> async = AsyncProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testAsyncSubjectValueEmpty() {
        AsyncProcessor<Integer> async = AsyncProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testAsyncSubjectValueError() {
        AsyncProcessor<Integer> async = AsyncProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testPublishSubjectValueRelay() {
        PublishProcessor<Integer> async = PublishProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
    }

    @Test
    public void testPublishSubjectValueEmpty() {
        PublishProcessor<Integer> async = PublishProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
    }
    @Test
    public void testPublishSubjectValueError() {
        PublishProcessor<Integer> async = PublishProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
    }

    @Test
    public void testBehaviorSubjectValueRelay() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectValueRelayIncomplete() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        async.onNext(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectIncompleteEmpty() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectEmpty() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectError() {
        BehaviorProcessor<Integer> async = BehaviorProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testReplaySubjectValueRelay() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        async.onNext(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBounded() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBoundedIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, async.getValue());
        assertTrue(async.hasValue());
        assertArrayEquals(new Object[] { 1 }, async.getValues());
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBoundedEmptyIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayEmptyIncomplete() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testReplaySubjectEmpty() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectError() {
        ReplayProcessor<Integer> async = ReplayProcessor.create();
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testReplaySubjectBoundedEmpty() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        async.onComplete();
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectBoundedError() {
        ReplayProcessor<Integer> async = ReplayProcessor.createWithSize(1);
        TestException te = new TestException();
        async.onError(te);
        FlowableProcessor<Integer> serial = async.toSerialized();

        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(async.getValue());
        assertFalse(async.hasValue());
        assertArrayEquals(new Object[] { }, async.getValues());
        assertArrayEquals(new Integer[] { }, async.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, async.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, async.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testDontWrapSerializedSubjectAgain() {
        PublishProcessor<Object> s = PublishProcessor.create();
        FlowableProcessor<Object> s1 = s.toSerialized();
        FlowableProcessor<Object> s2 = s1.toSerialized();
        assertSame(s1, s2);
    }
}