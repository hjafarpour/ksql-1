package io.confluent.ksql.codegen;

import static io.confluent.ksql.testutils.AnalysisTestUtil.analyzeQuery;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.confluent.ksql.analyzer.Analysis;
import io.confluent.ksql.function.InternalFunctionRegistry;
import io.confluent.ksql.function.UdfLoaderUtil;
import io.confluent.ksql.metastore.MetaStore;
import io.confluent.ksql.util.MetaStoreFixture;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.junit.Before;
import org.junit.Test;

public class SqlToJavaVisitorTest {

  private MetaStore metaStore;
  private Schema schema;
  private final InternalFunctionRegistry functionRegistry = new InternalFunctionRegistry();

  @Before
  public void init() {
    metaStore = MetaStoreFixture.getNewMetaStore(functionRegistry);
    // load udfs that are not hardcoded
    UdfLoaderUtil.load(metaStore);

    final Schema addressSchema = SchemaBuilder.struct()
        .field("NUMBER",Schema.OPTIONAL_INT64_SCHEMA)
        .field("STREET", Schema.OPTIONAL_STRING_SCHEMA)
        .field("CITY", Schema.OPTIONAL_STRING_SCHEMA)
        .field("STATE", Schema.OPTIONAL_STRING_SCHEMA)
        .field("ZIPCODE", Schema.OPTIONAL_INT64_SCHEMA)
        .optional().build();

    schema = SchemaBuilder.struct()
        .field("TEST1.COL0", SchemaBuilder.OPTIONAL_INT64_SCHEMA)
        .field("TEST1.COL1", SchemaBuilder.OPTIONAL_STRING_SCHEMA)
        .field("TEST1.COL2", SchemaBuilder.OPTIONAL_STRING_SCHEMA)
        .field("TEST1.COL3", SchemaBuilder.OPTIONAL_FLOAT64_SCHEMA)
        .field("TEST1.COL4", SchemaBuilder.array(Schema.OPTIONAL_FLOAT64_SCHEMA).optional().build())
        .field("TEST1.COL5", SchemaBuilder.map(Schema.OPTIONAL_STRING_SCHEMA, Schema.OPTIONAL_FLOAT64_SCHEMA).optional().build())
        .field("TEST1.COL6", addressSchema)
        .build();
  }

  @Test
  public void shouldProcessBasicJavaMath() {
    final String simpleQuery = "SELECT col0+col3, col2, col3+10, col0*25, 12*4+2 FROM test1 WHERE col0 > 100;";
    final Analysis analysis = analyzeQuery(simpleQuery, metaStore);

    final String javaExpression = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(0));

    assertThat(javaExpression, equalTo("(TEST1_COL0 + TEST1_COL3)"));
  }

  @Test
  public void shouldProcessArrayExpressionCorrectly() {
    final String simpleQuery = "SELECT col4[1] FROM test1 WHERE col0 > 100;";
    final Analysis analysis = analyzeQuery(simpleQuery, metaStore);

    final String javaExpression = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(0));

    assertThat(javaExpression,
        equalTo("((Double) ((java.util.List)TEST1_COL4).get(((int)(Integer.parseInt(\"1\"))) - 1))"));
  }

  @Test
  public void shouldProcessMapExpressionCorrectly() {
    final String simpleQuery = "SELECT col5['key1'] FROM test1 WHERE col0 > 100;";
    final Analysis analysis = analyzeQuery(simpleQuery, metaStore);

    final String javaExpression = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(0));

    assertThat(javaExpression, equalTo("((Double) ((java.util.Map)TEST1_COL5).get(\"key1\"))"));
  }

  @Test
  public void shouldCreateCorrectCastJavaExpression() {

    final String simpleQuery = "SELECT cast(col0 AS INTEGER), cast(col3 as BIGINT), cast(col3 as "
        + "varchar) FROM "
        + "test1 WHERE "
        + "col0 > 100;";
    final Analysis analysis = analyzeQuery(simpleQuery, metaStore);

    final String javaExpression0 = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(0));
    final String javaExpression1 = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(1));
    final String javaExpression2 = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(2));

    assertThat(javaExpression0, equalTo("(new Long(TEST1_COL0).intValue())"));
    assertThat(javaExpression1, equalTo("(new Double(TEST1_COL3).longValue())"));
    assertThat(javaExpression2, equalTo("String.valueOf(TEST1_COL3)"));
  }

  @Test
  public void shouldPostfixFunctionInstancesWithUniqueId() {
    final Analysis analysis = analyzeQuery(
        "SELECT CONCAT(SUBSTRING(col1,1,3),CONCAT('-',SUBSTRING(col1,4,5))) FROM test1;",
        metaStore);

    final String javaExpression = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getSelectExpressions().get(0));

    assertThat(javaExpression, is(
        "((String) CONCAT_0.evaluate("
            + "((String) SUBSTRING_1.evaluate(TEST1_COL1, Integer.parseInt(\"1\"), Integer.parseInt(\"3\"))), "
            + "((String) CONCAT_2.evaluate(\"-\","
            + " ((String) SUBSTRING_3.evaluate(TEST1_COL1, Integer.parseInt(\"4\"), Integer.parseInt(\"5\")))))))"));
  }

  @Test
  public void shouldGenerateCorrectCodeForComparisonWithNegativeNumbers() {
    final Analysis analysis = analyzeQuery(
        "SELECT * FROM test1 WHERE col3 > -10.0;", metaStore);

    final String javaExpression = new SqlToJavaVisitor(schema, functionRegistry)
        .process(analysis.getWhereExpression());
    assertThat(javaExpression, equalTo("((((Object)(TEST1_COL3)) == null || ((Object)(-10.0)) == null) ? false : (TEST1_COL3 > -10.0))"));
  }

}