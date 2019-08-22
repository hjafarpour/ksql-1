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

package io.confluent.ksql.test.utils;

import io.confluent.ksql.function.FunctionRegistry;
import io.confluent.ksql.function.InternalFunctionRegistry;
import io.confluent.ksql.function.UdfCompiler;
import io.confluent.ksql.function.UdfLoader;
import io.confluent.ksql.test.tools.KsqlTestingTool;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

public final class TestRunnerFunctionRegistryUtil {

  private TestRunnerFunctionRegistryUtil() {
  }

  public static FunctionRegistry getFunctionRegistry(final File statementFileObject) {
    Objects.requireNonNull(statementFileObject, "statementFileObject");
    final InternalFunctionRegistry mutableFR = new InternalFunctionRegistry();
    new UdfLoader(mutableFR,
        statementFileObject.getParentFile(),
        KsqlTestingTool.class.getClassLoader(),
        value -> false, new UdfCompiler(Optional.empty()), Optional.empty(), true
    ).load();
    return mutableFR;
  }

}
