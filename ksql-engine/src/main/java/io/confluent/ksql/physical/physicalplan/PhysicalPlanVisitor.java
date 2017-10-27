package io.confluent.ksql.physical.physicalplan;

/**
 * Created by hojjat on 10/26/17.
 */
public class PhysicalPlanVisitor<C, R> {
  protected R visitPhysicalPlanNode(PhysicalPlanNode node, C context) {
    return null;
  }

  public R process(PhysicalPlanNode node, C context) {
    return node.accept(this, context);
  }

  public R visitAggregatePhysicalPlanNode(AggregatePhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitBroadcastJoinPhysicalPlanNode(BroadcastJoinPhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitHashJoinPhysicalPlanNode(HashJoinPhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitOutputPhysicalPlanNode(OutputPhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitFilterPhysicalPlanNode(FilterPhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitProjectPhysicalPlanNode(ProjectPhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitStreamPhysicalPlanNode(StreamPhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

  public R visitTablePhysicalPlanNode(TablePhysicalPlanNode node, C context) {
    return visitPhysicalPlanNode(node, context);
  }

}
