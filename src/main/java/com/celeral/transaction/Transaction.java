/*
 * Copyright © 2020 Celeral.
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
 */
package com.celeral.transaction;

public interface Transaction<T> {
  enum Result {
    CONTINUE,
    COMMIT,
    ABORT
  }

  interface ReturnValue {
    Result getResult();
  }

  static class MinimalistReturnValue implements ReturnValue {
    private final Result result;

    MinimalistReturnValue() {
      result = null;
    }

    MinimalistReturnValue(Result result) {
      this.result = result;
    }

    @Override
    public Result getResult() {
      return result;
    }
  }

  ReturnValue CONTINUE = new MinimalistReturnValue(Result.CONTINUE);
  ReturnValue COMMIT = new MinimalistReturnValue(Result.COMMIT);
  ReturnValue ABORT = new MinimalistReturnValue(Result.ABORT);

  long getId();

  ReturnValue init(ExecutionContext context) throws Exception;

  ReturnValue process(T payload) throws Exception;

  void commit() throws Exception;

  void abort() throws Exception;
}
