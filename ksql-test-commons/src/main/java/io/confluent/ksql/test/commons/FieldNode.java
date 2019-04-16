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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.confluent.ksql.test.commons.StructuredDataSourceMatchers.FieldMatchers;
import io.confluent.ksql.test.commons.StructuredDataSourceMatchers.OptionalMatchers;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Field;
import org.hamcrest.Matcher;

public class FieldNode {

  static final FieldNode NULL = new FieldNode("explicitly set to NULL", Optional.empty());

  private final String name;
  private final Optional<ConnectSchema> schema;

  FieldNode(
      @JsonProperty("name") final String name,

      @JsonProperty("schema")
      @JsonDeserialize(using = ConnectSchemaDeserializer.class) final Optional<ConnectSchema> schema
  ) {
    this.name = name == null ? "" : name;
    this.schema = Objects.requireNonNull(schema, "schema");

    if (this.name.isEmpty()) {
      throw new InvalidFieldException("name", "empty or missing");
    }
  }

  @SuppressWarnings("unchecked")
  Matcher<Optional<Field>> build() {
    if (this == NULL) {
      return is(Optional.empty());
    }

    final Matcher<Optional<Field>> nameMatcher = OptionalMatchers.of(FieldMatchers.hasName(name));

    final Matcher<Optional<Field>> schemaMatcher = schema
        .map(FieldMatchers::hasSchema)
        .map(OptionalMatchers::of)
        .orElse(null);

    final Matcher[] matchers = Stream.of(nameMatcher, schemaMatcher)
        .filter(Objects::nonNull)
        .toArray(Matcher[]::new);

    return allOf(matchers);
  }
}