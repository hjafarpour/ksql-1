/**
 * Copyright 2018 Confluent Inc.
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

package io.confluent.ksql.parser.rewrite;

import com.google.common.collect.ImmutableList;
import io.confluent.ksql.parser.tree.SubscriptExpression;
import java.util.Objects;

import io.confluent.ksql.parser.tree.DereferenceExpression;
import io.confluent.ksql.parser.tree.Expression;
import io.confluent.ksql.parser.tree.FunctionCall;
import io.confluent.ksql.parser.tree.Node;
import io.confluent.ksql.parser.tree.QualifiedName;
import io.confluent.ksql.parser.tree.QualifiedNameReference;
import io.confluent.ksql.parser.tree.Statement;
import io.confluent.ksql.parser.tree.StringLiteral;
import io.confluent.ksql.util.DataSourceExtractor;

public class StatementRewriteForStruct {

  private final Statement statement;

  public StatementRewriteForStruct(
      final Statement statement,
      final DataSourceExtractor dataSourceExtractor
  ) {
    this.statement = Objects.requireNonNull(statement, "statement");
  }

  public Statement rewriteForStruct() {
    return (Statement) new RewriteWithStructFieldExtractors().process(statement, null);
  }


  private class RewriteWithStructFieldExtractors extends StatementRewriter {

    @Override
    protected Node visitDereferenceExpression(
        final DereferenceExpression node,
        final Object context
    ) {
      return createFetchFunctionNodeIfNeeded(node, context);
    }

    private Expression createFetchFunctionNodeIfNeeded(
        final DereferenceExpression dereferenceExpression,
        final Object context
    ) {
      if (dereferenceExpression.getBase() instanceof QualifiedNameReference) {
        return getNewDereferenceExpression(dereferenceExpression, context);
      } else if (dereferenceExpression.getBase() instanceof SubscriptExpression) {
        return new FunctionCall(
            QualifiedName.of("FETCH_FIELD_FROM_STRUCT"),
            ImmutableList.of(
                dereferenceExpression.getBase(),
                new StringLiteral(dereferenceExpression.getFieldName())
            ));
      }
      return getNewFunctionCall(dereferenceExpression, context);
    }

    private FunctionCall getNewFunctionCall(
        final DereferenceExpression dereferenceExpression,
        final Object context
    ) {
      final Expression createFunctionResult = createFetchFunctionNodeIfNeeded(
          (DereferenceExpression) dereferenceExpression.getBase(), context);

      final String fieldName = dereferenceExpression.getFieldName();
      return new FunctionCall(
          QualifiedName.of("FETCH_FIELD_FROM_STRUCT"),
          ImmutableList.of(createFunctionResult, new StringLiteral(fieldName)));
    }

    private DereferenceExpression getNewDereferenceExpression(
        final DereferenceExpression dereferenceExpression,
        final Object context
    ) {
      return dereferenceExpression.getLocation()
          .map(location ->
              new DereferenceExpression(
                  dereferenceExpression.getLocation().get(),
                  (Expression) process(dereferenceExpression.getBase(), context),
                  dereferenceExpression.getFieldName())
          )
          .orElse(
              new DereferenceExpression(
                  (Expression) process(dereferenceExpression.getBase(), context),
                  dereferenceExpression.getFieldName()
              )
          );
    }
  }

}