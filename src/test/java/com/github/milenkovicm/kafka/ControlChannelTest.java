/*
 * Copyright 2015 Marko Milenkovic (http://github.com/milenkovicm)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.milenkovicm.kafka;

import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.github.milenkovicm.kafka.channel.ControlKafkaChannel;
import com.github.milenkovicm.kafka.channel.KafkaPromise;
import com.github.milenkovicm.kafka.protocol.Error;
import com.github.milenkovicm.kafka.protocol.KafkaException;
import com.github.milenkovicm.kafka.protocol.MetadataResponse;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class ControlChannelTest extends AbstractSingleBrokerTest {
    final String topic = "test";

    @Test
    public void test_fetcMetadata() throws Exception {
        createTopic(topic);

        ProducerProperties properties = new ProducerProperties();
        properties.override(ProducerProperties.NETTY_DEBUG_PIPELINE, true);

        ControlKafkaChannel controlKafkaChannel = new ControlKafkaChannel("localhost", START_PORT, topic, properties);
        controlKafkaChannel.connect().sync();

        final CountDownLatch latch = new CountDownLatch(1);
        final KafkaPromise future = controlKafkaChannel.fetchMetadata(topic);
        future.addListener(new FutureListener<MetadataResponse>() {

            @Override
            public void operationComplete(Future<MetadataResponse> future) throws Exception {
                Assert.assertNotNull(future.get());
                latch.countDown();
            }
        });

        Assert.assertTrue("latch not triggered", latch.await(2, TimeUnit.SECONDS));
        controlKafkaChannel.disconnect();
    }

    @Test
    public void test_fetchMetadata_notExistingTopic() throws Exception {
        String topicName = "topic_does_not_exist";

        ProducerProperties properties = new ProducerProperties();
        properties.override(ProducerProperties.NETTY_DEBUG_PIPELINE, true);

        ControlKafkaChannel controlKafkaChannel = new ControlKafkaChannel("localhost", START_PORT, topicName, properties);

        controlKafkaChannel.connect().sync();
        try {
            controlKafkaChannel.fetchMetadata(topicName).sync();
            Assert.fail("shouldn't get here");
        } catch (KafkaException e) {
            Assert.assertThat(e.error, is(Error.LEADER_NOT_AVAILABLE));
        }

        controlKafkaChannel.disconnect();
    }
}