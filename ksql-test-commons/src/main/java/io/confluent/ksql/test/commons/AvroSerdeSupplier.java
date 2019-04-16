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

package io.confluent.ksql.test.commons;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class AvroSerdeSupplier implements SerdeSupplier {
  @Override
  public Serializer getSerializer(final SchemaRegistryClient schemaRegistryClient) {
    return new KafkaAvroSerializer(schemaRegistryClient);
  }

  @Override
  public Deserializer getDeserializer(final SchemaRegistryClient schemaRegistryClient) {
    return new KafkaAvroDeserializer(schemaRegistryClient);
  }
}