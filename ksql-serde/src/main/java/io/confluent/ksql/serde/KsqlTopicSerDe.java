/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License; you may not use this file
 * except in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.serde;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.ksql.GenericRow;
import io.confluent.ksql.util.KsqlConfig;

import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.connect.data.Schema;


public abstract class KsqlTopicSerDe {

  private final DataSource.DataSourceSerDe serDe;

  protected KsqlTopicSerDe(final DataSource.DataSourceSerDe serDe) {
    this.serDe = serDe;
  }

  public DataSource.DataSourceSerDe getSerDe() {
    return serDe;
  }

  public abstract Serde<GenericRow> getGenericRowSerde(
      Schema schemaMaybeWithSource,
      KsqlConfig ksqlConfig,
      boolean isInternal,
      Supplier<SchemaRegistryClient> schemaRegistryClientFactory,
      String loggerNamePrefix);
}
