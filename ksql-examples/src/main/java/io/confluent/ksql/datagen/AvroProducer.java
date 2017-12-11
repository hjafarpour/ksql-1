/**
 * Copyright 2017 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.confluent.ksql.datagen;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.ksql.GenericRow;
import io.confluent.ksql.serde.avro.KsqlGenericRowAvroSerializer;
import io.confluent.ksql.util.KsqlConfig;

import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AvroProducer extends DataGenProducer {

  @Override
  protected Serializer<GenericRow> getSerializer(
      Schema avroSchema,
      org.apache.kafka.connect.data.Schema kafkaSchema,
      String topicName
  ) {
    Serializer<GenericRow> result = new KsqlGenericRowAvroSerializer(kafkaSchema,
                                                                     new MockSchemaRegistryClient(),
                                                                     new KsqlConfig(Collections.emptyMap()),
                                                                     false);
    Map<String, String> serializerConfiguration = new HashMap<>();
    serializerConfiguration.put(KsqlGenericRowAvroSerializer.AVRO_SERDE_SCHEMA_CONFIG, avroSchema.toString());
    result.configure(serializerConfiguration, false);
    return result;
  }
}