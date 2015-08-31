package com.github.apache9.nedis;

import static com.github.apache9.nedis.TestUtils.assertMapEquals;
import static com.github.apache9.nedis.TestUtils.assertSetEquals;
import static com.github.apache9.nedis.TestUtils.cleanRedis;
import static com.github.apache9.nedis.TestUtils.probeFreePort;
import static com.github.apache9.nedis.TestUtils.waitUntilRedisUp;
import static com.github.apache9.nedis.util.NedisUtils.bytesToString;
import static com.github.apache9.nedis.util.NedisUtils.newBytesMap;
import static com.github.apache9.nedis.util.NedisUtils.toBytes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.apache9.nedis.protocol.HashEntry;
import com.github.apache9.nedis.protocol.ScanParams;
import com.github.apache9.nedis.protocol.ScanResult;
import com.github.apache9.nedis.util.NedisUtils;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Apache9
 */
public class TestCommands {

    private static int PORT;

    private static RedisServer REDIS;

    private static NedisClient CLIENT;

    @BeforeClass
    public static void setUp() throws IOException, InterruptedException {
        PORT = probeFreePort();
        REDIS = new RedisServer(PORT);
        REDIS.start();
        waitUntilRedisUp(PORT);
        CLIENT = NedisUtils.newPooledClient(NedisClientPoolBuilder.builder()
                .remoteAddress("127.0.0.1", PORT).build());
    }

    @AfterClass
    public static void tearDownAfterClass() throws InterruptedException {
        if (CLIENT != null) {
            CLIENT.close().sync();
        }
        REDIS.stop();
    }

    @After
    public void tearDown() throws InterruptedException {
        cleanRedis(PORT);
    }

    private static final Function<byte[], String> BYTES_TO_STRING = new Function<byte[], String>() {

        @Override
        public String apply(byte[] input) {
            return bytesToString(input);
        }
    };

    private static Set<String> toStringSet(List<byte[]> list) {
        return Sets.newHashSet(Lists.transform(list, BYTES_TO_STRING));
    }

    private static Map<String, String> toStringMap(Map<byte[], byte[]> map) {
        Map<String, String> newMap = Maps.newHashMapWithExpectedSize(map.size());
        for (Map.Entry<byte[], byte[]> e: map.entrySet()) {
            newMap.put(bytesToString(e.getKey()), bytesToString(e.getValue()));
        }
        return newMap;
    }

    @Test
    public void testSetsCommands() throws InterruptedException {
        assertEquals(1L, CLIENT.sadd(toBytes("foo"), toBytes("bar")).sync().getNow().longValue());
        assertEquals(1L, CLIENT.scard(toBytes("foo")).sync().getNow().longValue());
        assertFalse(CLIENT.sismember(toBytes("foo"), toBytes("foo")).sync().getNow().booleanValue());
        assertTrue(CLIENT.sismember(toBytes("foo"), toBytes("bar")).sync().getNow().booleanValue());
        assertSetEquals(Sets.newHashSet("bar"), toStringSet(CLIENT.smembers(toBytes("foo")).sync()
                .getNow()));
        assertEquals("bar", bytesToString(CLIENT.srandmember(toBytes("foo")).sync().getNow()));

        assertEquals(1L, CLIENT.sadd(toBytes("foo"), toBytes("bar"), toBytes("barbar")).sync()
                .getNow().longValue());

        assertSetEquals(Sets.newHashSet("bar", "barbar"),
                toStringSet(CLIENT.srandmember(toBytes("foo"), 2).sync().getNow()));

        assertEquals(1L, CLIENT.srem(toBytes("foo"), toBytes("barbarbar"), toBytes("bar")).sync()
                .getNow().longValue());
        assertTrue(CLIENT.smove(toBytes("foo"), toBytes("foofoo"), toBytes("barbar")).sync()
                .getNow().booleanValue());
        assertFalse(CLIENT.smove(toBytes("foofoo"), toBytes("foo"), toBytes("bar")).sync().getNow()
                .booleanValue());
        assertEquals("barbar", bytesToString(CLIENT.spop(toBytes("foofoo")).sync().getNow()));
        assertNull(CLIENT.spop(toBytes("foo")).sync().getNow());

        assertEquals(3L, CLIENT.sadd(toBytes("s1"), toBytes("v1"), toBytes("v2"), toBytes("v3"))
                .sync().getNow().longValue());
        assertEquals(3L, CLIENT.sadd(toBytes("s2"), toBytes("v1"), toBytes("v2"), toBytes("v4"))
                .sync().getNow().longValue());

        assertSetEquals(Sets.newHashSet("v3"),
                toStringSet(CLIENT.sdiff(toBytes("s1"), toBytes("s2")).sync().getNow()));

        assertEquals(1L, CLIENT.sdiffstore(toBytes("s3"), toBytes("s2"), toBytes("s1")).sync()
                .getNow().longValue());
        assertSetEquals(Sets.newHashSet("v4"), toStringSet(CLIENT.smembers(toBytes("s3")).sync()
                .getNow()));

        assertSetEquals(Sets.newHashSet("v1", "v2"),
                toStringSet(CLIENT.sinter(toBytes("s1"), toBytes("s2")).sync().getNow()));

        assertEquals(2L, CLIENT.sinterstore(toBytes("s3"), toBytes("s2"), toBytes("s1")).sync()
                .getNow().longValue());
        assertSetEquals(Sets.newHashSet("v1", "v2"), toStringSet(CLIENT.smembers(toBytes("s3"))
                .sync().getNow()));

        assertSetEquals(Sets.newHashSet("v1", "v2", "v3", "v4"),
                toStringSet(CLIENT.sunion(toBytes("s1"), toBytes("s2")).sync().getNow()));

        assertEquals(4L, CLIENT.sunionstore(toBytes("s3"), toBytes("s2"), toBytes("s1")).sync()
                .getNow().longValue());
        assertSetEquals(Sets.newHashSet("v1", "v2", "v3", "v4"),
                toStringSet(CLIENT.smembers(toBytes("s3")).sync().getNow()));

        Set<String> members = Sets.newHashSet();
        byte[] cursor = null;
        for (;;) {
            ScanResult<byte[]> scanResult = CLIENT
                    .sscan(toBytes("s3"), new ScanParams().cursor(cursor).count(1)).sync().getNow();
            members.addAll(Lists.transform(scanResult.values(), BYTES_TO_STRING));
            if (!scanResult.more()) {
                break;
            }
            cursor = scanResult.cursor();
        }
        assertSetEquals(Sets.newHashSet("v1", "v2", "v3", "v4"), members);
    }

    @Test
    public void testHashesCommands() throws InterruptedException {
        assertTrue(CLIENT.hset(toBytes("h"), toBytes("f1"), toBytes("v1")).sync().getNow()
                .booleanValue());

        assertEquals("v1", bytesToString(CLIENT.hget(toBytes("h"), toBytes("f1")).sync().getNow()));
        assertNull(CLIENT.hget(toBytes("h"), toBytes("f2")).sync().getNow());

        assertTrue(CLIENT.hexists(toBytes("h"), toBytes("f1")).sync().getNow().booleanValue());
        assertFalse(CLIENT.hexists(toBytes("h"), toBytes("f2")).sync().getNow().booleanValue());

        assertFalse(CLIENT.hsetnx(toBytes("h"), toBytes("f1"), toBytes("v2")).sync().getNow());
        assertTrue(CLIENT.hsetnx(toBytes("h"), toBytes("f2"), toBytes("v2")).sync().getNow());

        assertSetEquals(Sets.newHashSet("v1", "v2"),
                toStringSet(CLIENT.hmget(toBytes("h"), toBytes("f1"), toBytes("f2")).sync()
                        .getNow()));
        Map<byte[], byte[]> map = newBytesMap();
        map.put(toBytes("f3"), toBytes("v3"));
        map.put(toBytes("f4"), toBytes("v4"));
        CLIENT.hmset(toBytes("h"), map).sync();

        assertEquals(4L, CLIENT.hlen(toBytes("h")).sync().getNow().longValue());

        assertMapEquals(ImmutableMap.of("f1", "v1", "f2", "v2", "f3", "v3", "f4", "v4"),
                toStringMap(CLIENT.hgetAll(toBytes("h")).sync().getNow()));

        Map<String, String> entries = Maps.newHashMap();
        byte[] cursor = null;
        for (;;) {
            ScanResult<HashEntry> scanResult = CLIENT
                    .hscan(toBytes("h"), new ScanParams().cursor(cursor).count(1)).sync().getNow();
            for (HashEntry e: scanResult.values()) {
                entries.put(bytesToString(e.field()), bytesToString(e.value()));
            }
            if (!scanResult.more()) {
                break;
            }
            cursor = scanResult.cursor();
        }
        assertMapEquals(ImmutableMap.of("f1", "v1", "f2", "v2", "f3", "v3", "f4", "v4"), entries);

        assertSetEquals(Sets.newHashSet("f1", "f2", "f3", "f4"),
                toStringSet(CLIENT.hkeys(toBytes("h")).sync().getNow()));
        assertSetEquals(Sets.newHashSet("v1", "v2", "v3", "v4"),
                toStringSet(CLIENT.hvals(toBytes("h")).sync().getNow()));

        assertEquals(2L, CLIENT.hdel(toBytes("h"), toBytes("f5"), toBytes("f4"), toBytes("f3"))
                .sync().getNow().longValue());

        assertEquals(10L, CLIENT.hincrby(toBytes("h"), toBytes("l"), 10).sync().getNow()
                .longValue());
        assertEquals("10", bytesToString(CLIENT.hget(toBytes("h"), toBytes("l")).sync().getNow()));

        assertEquals(
                Double.toString(5.0),
                Double.toString(CLIENT.hincrbyfloat(toBytes("h"), toBytes("d"), 5.0).sync()
                        .getNow()));
        assertEquals(
                Double.toString(5.0),
                Double.valueOf(
                        bytesToString(CLIENT.hget(toBytes("h"), toBytes("d")).sync().getNow()))
                        .toString());
    }
}
