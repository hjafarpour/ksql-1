/*
 * Copyright 2019 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.test.tools;

import org.apache.kafka.clients.producer.ProducerRecord;

public class FakeKafkaRecord {

  private final Record testRecord;
  private final ProducerRecord producerRecord;

  public FakeKafkaRecord(final Record testRecord, final ProducerRecord producerRecord) {
    this.testRecord = testRecord;
    this.producerRecord = producerRecord;
  }

  public Record getTestRecord() {
    return testRecord;
  }

  public ProducerRecord getProducerRecord() {
    return producerRecord;
  }
}