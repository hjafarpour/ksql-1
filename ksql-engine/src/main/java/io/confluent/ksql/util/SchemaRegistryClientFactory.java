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

package io.confluent.ksql.util;

import java.util.HashMap;
import java.util.Map;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

public class SchemaRegistryClientFactory {

  public static String schemaRegistryUrl;
  public static SchemaRegistryClient schemaRegistryClient;

  public static Map<String, SchemaRegistryClient> schemaRegistryClientMap = new HashMap<>();

  public static void setSchemaRegistryClient(String schemaRegistryUrlStr) {
    schemaRegistryUrl = schemaRegistryUrlStr;
    schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 1000);
    schemaRegistryClientMap.put(schemaRegistryUrl, schemaRegistryClient);
  }

  public static SchemaRegistryClient getSchemaRegistryClient() {
    if (schemaRegistryClient == null) {
      throw new KsqlException("Schema registry client has not been initialized.");
    }
    return schemaRegistryClient;
  }

  public static synchronized SchemaRegistryClient getSchemaRegistryClient(String srUrl) {
    if (srUrl.equalsIgnoreCase(schemaRegistryUrl)) {
      return schemaRegistryClient;
    } else {
      SchemaRegistryClient srClient = schemaRegistryClientMap.get(srUrl);
      if (srClient != null) {
        return srClient;
      } else {
        srClient = new CachedSchemaRegistryClient(srUrl, 1000);
        schemaRegistryClientMap.put(srUrl, srClient);
        return srClient;
      }
    }
  }

  public static void setSchemaRegistryClient(
      SchemaRegistryClient schemaRegistryClient) {
    SchemaRegistryClientFactory.schemaRegistryClient = schemaRegistryClient;
    schemaRegistryClientMap.put(schemaRegistryUrl, schemaRegistryClient);
  }
}
