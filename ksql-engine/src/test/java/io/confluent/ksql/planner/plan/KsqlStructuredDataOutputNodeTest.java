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

package io.confluent.ksql.planner.plan;

import static io.confluent.ksql.planner.plan.PlanTestUtil.MAPVALUES_NODE;
import static io.confluent.ksql.planner.plan.PlanTestUtil.SOURCE_NODE;
import static io.confluent.ksql.planner.plan.PlanTestUtil.TRANSFORM_NODE;
import static io.confluent.ksql.planner.plan.PlanTestUtil.getNodeByName;
import static io.confluent.ksql.planner.plan.PlanTestUtil.verifyProcessorNode;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.confluent.ksql.ddl.DdlConfig;
import io.confluent.ksql.function.InternalFunctionRegistry;
import io.confluent.ksql.metastore.KsqlStream;
import io.confluent.ksql.metastore.KsqlTable;
import io.confluent.ksql.metastore.KsqlTopic;
import io.confluent.ksql.serde.json.KsqlJsonTopicSerDe;
import io.confluent.ksql.services.TestServiceContext;
import io.confluent.ksql.services.ServiceContext;
import io.confluent.ksql.structured.SchemaKStream;
import io.confluent.ksql.structured.SchemaKTable;
import io.confluent.ksql.util.KafkaTopicClient;
import io.confluent.ksql.util.KsqlConfig;
import io.confluent.ksql.util.timestamp.LongColumnTimestampExtractionPolicy;
import io.confluent.ksql.util.timestamp.MetadataTimestampExtractionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KsqlStructuredDataOutputNodeTest {
  private final KafkaTopicClient topicClient = EasyMock.createNiceMock(KafkaTopicClient.class);
  private static final String MAPVALUES_OUTPUT_NODE = "KSTREAM-MAPVALUES-0000000003";
  private static final String OUTPUT_NODE = "KSTREAM-SINK-0000000004";

  private final Schema schema = SchemaBuilder.struct()
      .field("field1", Schema.OPTIONAL_STRING_SCHEMA)
      .field("field2", Schema.OPTIONAL_STRING_SCHEMA)
      .field("field3", Schema.OPTIONAL_STRING_SCHEMA)
      .field("timestamp", Schema.OPTIONAL_INT64_SCHEMA)
      .field("key", Schema.OPTIONAL_STRING_SCHEMA)
      .build();

  private final KsqlStream dataSource = new KsqlStream<>("sqlExpression", "datasource",
      schema,
      schema.field("key"),
      new LongColumnTimestampExtractionPolicy("timestamp"),
      new KsqlTopic("input", "input",
          new KsqlJsonTopicSerDe(), false), Serdes.String());
  private final StructuredDataSourceNode sourceNode = new StructuredDataSourceNode(
      new PlanNodeId("0"),
      dataSource,
      schema);

  private final KsqlConfig ksqlConfig =  new KsqlConfig(new HashMap<>());
  private StreamsBuilder builder = new StreamsBuilder();
  private KsqlStructuredDataOutputNode outputNode;

  private SchemaKStream stream;
  private ServiceContext serviceContext;

  @Before
  public void before() {
    final TopicPartitionInfo topicPartitionInfo = niceMock(TopicPartitionInfo.class);
    expect(topicPartitionInfo.replicas()).andReturn(Collections.singletonList(niceMock(Node.class))).anyTimes();
    final TopicPartition topicPartition = new TopicPartition("input", 1);
    final TopicDescription topicDescription = niceMock(TopicDescription.class);
    expect(topicDescription.partitions()).andReturn(Collections.singletonList(topicPartitionInfo)).anyTimes();
    expect(topicClient.describeTopics(anyObject())).andStubReturn(Collections.singletonMap("input", topicDescription));

    final Map<String, Object> props = new HashMap<>();
    props.put(KsqlConfig.SINK_NUMBER_OF_PARTITIONS_PROPERTY, 4);
    props.put(KsqlConfig.SINK_NUMBER_OF_REPLICAS_PROPERTY, (short) 3);
    createOutputNode(props);
    topicClient.createTopic(eq("output"), eq(4), eq((short) 3), anyBoolean(), eq(Collections.emptyMap()));
    EasyMock.expectLastCall();
    EasyMock.replay(topicClient);
    serviceContext = TestServiceContext.create(topicClient);
    stream = buildStream();
  }

  @After
  public void tearDown() {
    serviceContext.close();
  }

  private void createOutputNode(final Map<String, Object> props) {
    outputNode = new KsqlStructuredDataOutputNode(new PlanNodeId("0"),
        sourceNode,
        schema,
        new LongColumnTimestampExtractionPolicy("timestamp"),
        schema.field("key"),
        new KsqlTopic("output", "output", new KsqlJsonTopicSerDe(), true),
        "output",
        props,
        Optional.empty(),
        true);
  }

  @Test
  public void shouldBuildSourceNode() {
    final TopologyDescription.Source node = (TopologyDescription.Source) getNodeByName(builder.build(), SOURCE_NODE);
    final List<String> successors = node.successors().stream().map(TopologyDescription.Node::name).collect(Collectors.toList());
    assertThat(node.predecessors(), equalTo(Collections.emptySet()));
    assertThat(successors, equalTo(Collections.singletonList(MAPVALUES_NODE)));
    assertThat(node.topicSet(), equalTo(ImmutableSet.of("input")));
  }


  @Test
  public void shouldBuildMapNodePriorToOutput() {
    verifyProcessorNode((TopologyDescription.Processor) getNodeByName(builder.build(), MAPVALUES_OUTPUT_NODE),
        Collections.singletonList(TRANSFORM_NODE),
        Collections.singletonList(OUTPUT_NODE));
  }

  @Test
  public void shouldBuildOutputNode() {
    final TopologyDescription.Sink sink = (TopologyDescription.Sink) getNodeByName(builder.build(), OUTPUT_NODE);
    final List<String> predecessors = sink.predecessors().stream().map(TopologyDescription.Node::name).collect(Collectors.toList());
    assertThat(sink.successors(), equalTo(Collections.emptySet()));
    assertThat(predecessors, equalTo(Collections.singletonList(MAPVALUES_OUTPUT_NODE)));
    assertThat(sink.topic(), equalTo("output"));
  }

  @Test
  public void shouldSetOutputNodeOnStream() {
    assertThat(stream.outputNode(), instanceOf(KsqlStructuredDataOutputNode.class));
  }

  @Test
  public void shouldHaveCorrectOutputNodeSchema() {
    final List<Field> expected = Arrays.asList(
        new Field("ROWTIME", 0, Schema.OPTIONAL_INT64_SCHEMA),
        new Field("ROWKEY", 1, Schema.OPTIONAL_STRING_SCHEMA),
        new Field("field1", 2, Schema.OPTIONAL_STRING_SCHEMA),
        new Field("field2", 3, Schema.OPTIONAL_STRING_SCHEMA),
        new Field("field3", 4, Schema.OPTIONAL_STRING_SCHEMA),
        new Field("timestamp", 5, Schema.OPTIONAL_INT64_SCHEMA),
        new Field("key", 6, Schema.OPTIONAL_STRING_SCHEMA));
    final List<Field> fields = stream.outputNode().getSchema().fields();
    assertThat(fields, equalTo(expected));
  }

  @Test
  public void shouldPartitionByFieldNameInPartitionByProperty() {
    createOutputNode(Collections.singletonMap(DdlConfig.PARTITION_BY_PROPERTY, "field2"));
    final SchemaKStream schemaKStream = outputNode.buildStream(builder,
        ksqlConfig,
        serviceContext,
        new InternalFunctionRegistry(),
        Collections.singletonMap(DdlConfig.PARTITION_BY_PROPERTY, "field2"));
    final Field keyField = schemaKStream.getKeyField();
    assertThat(keyField, equalTo(new Field("field2", 1, Schema.OPTIONAL_STRING_SCHEMA)));
    assertThat(schemaKStream.getSchema().fields(), equalTo(schema.fields()));
  }

  @Test
  public void shouldCreateSinkTopic() {
    EasyMock.verify(topicClient);
  }

  private SchemaKStream buildStream() {
    builder = new StreamsBuilder();

    return outputNode.buildStream(builder,
        ksqlConfig,
        serviceContext,
        new InternalFunctionRegistry(),
        new HashMap<>());
  }

  @Test
  public void shouldCreateSinkWithCorrectCleanupPolicyNonWindowedTable() {
    outputNode = getKsqlStructuredDataOutputNode(Serdes.String());

    final KafkaTopicClient topicClientForNonWindowTable = EasyMock.mock(KafkaTopicClient.class);
    final Map<String, String> topicConfig = ImmutableMap.of(
        TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
    topicClientForNonWindowTable.createTopic("output", 4, (short) 3, false, topicConfig);
    EasyMock.replay(topicClientForNonWindowTable);

    final SchemaKStream schemaKStream = buildStream();

    assertThat(schemaKStream, instanceOf(SchemaKTable.class));
  }

  @Test
  public void shouldCreateSinkWithCorrectCleanupPolicyWindowedTable() {
    outputNode = getKsqlStructuredDataOutputNode(
        WindowedSerdes.timeWindowedSerdeFrom(String.class));

    final KafkaTopicClient topicClientForWindowTable = EasyMock.mock(KafkaTopicClient.class);
    topicClientForWindowTable.createTopic("output", 4, (short) 3, false, Collections.emptyMap());
    EasyMock.replay(topicClientForWindowTable);

    final SchemaKStream schemaKStream = buildStream();

    assertThat(schemaKStream, instanceOf(SchemaKTable.class));
  }

  @Test
  public void shouldCreateSinkWithCorrectCleanupPolicyStream() {
    final KafkaTopicClient topicClientForWindowTable = EasyMock.mock(KafkaTopicClient.class);
    topicClientForWindowTable.createTopic("output", 4, (short) 3, false, Collections.emptyMap());
    EasyMock.replay(topicClientForWindowTable);

    final SchemaKStream schemaKStream = buildStream();

    assertThat(schemaKStream, instanceOf(SchemaKStream.class));
  }

  @Test
  public void shouldCreateSinkWithTheSourcePartititionReplication() {
    final KafkaTopicClient topicClientForStream = getTopicClient();
    topicClientForStream.createTopic("output", 1, (short) 1, false, Collections.emptyMap());
    expectLastCall();
    EasyMock.replay(topicClientForStream);
    final StreamsBuilder streamsBuilder = new StreamsBuilder();
    createOutputNode(Collections.emptyMap());
    final SchemaKStream schemaKStream = outputNode.buildStream(
        streamsBuilder,
        new KsqlConfig(Collections.emptyMap()),
        serviceContext,
        new InternalFunctionRegistry(),
        new HashMap<>());
    assertThat(schemaKStream, instanceOf(SchemaKStream.class));
    EasyMock.verify();
  }

  private KsqlStructuredDataOutputNode getKsqlStructuredDataOutputNode(final Serde<?> keySerde) {
    final Map<String, Object> props = new HashMap<>();
    props.put(KsqlConfig.SINK_NUMBER_OF_PARTITIONS_PROPERTY, 4);
    props.put(KsqlConfig.SINK_NUMBER_OF_REPLICAS_PROPERTY, (short)3);

    final StructuredDataSourceNode tableSourceNode = new StructuredDataSourceNode(
        new PlanNodeId("0"),
        new KsqlTable<>(
            "sqlExpression", "datasource",
            schema,
            schema.field("key"),
            new MetadataTimestampExtractionPolicy(),
            new KsqlTopic("input", "input", new KsqlJsonTopicSerDe(), false),
            "TableStateStore",
            keySerde),
        schema);

    return new KsqlStructuredDataOutputNode(
        new PlanNodeId("0"),
        tableSourceNode,
        schema,
        new MetadataTimestampExtractionPolicy(),
        schema.field("key"),
        new KsqlTopic("output", "output", new KsqlJsonTopicSerDe(), true),
        "output",
        props,
        Optional.empty(),
        true);
  }

  private KafkaTopicClient getTopicClient() {
    final KafkaTopicClient topicClient = EasyMock.mock(KafkaTopicClient.class);
    final TopicPartitionInfo topicPartitionInfo = niceMock(TopicPartitionInfo.class);
    expect(topicPartitionInfo.replicas()).andReturn(Collections.singletonList(niceMock(Node.class))).anyTimes();

    final TopicPartition topicPartition = new TopicPartition("FOO", 1);
    final TopicDescription topicDescription = niceMock(TopicDescription.class);
    expect(topicDescription.partitions()).andReturn(Collections.singletonList(topicPartitionInfo)).anyTimes();
    expect(topicClient.describeTopics(anyObject())).andReturn(Collections.singletonMap("input", topicDescription)).anyTimes();
    replay(topicPartitionInfo, topicDescription);
    return topicClient;
  }

}