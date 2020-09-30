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
package com.celeral.transaction.processor;

import java.io.File;
import java.util.UUID;

import org.apache.commons.lang3.SystemUtils;

import com.celeral.transaction.ExecutionContext;
import com.celeral.transaction.Payload;
import com.celeral.transaction.Transaction;
import com.celeral.transaction.TransactionProcessor;

public class SerialTransactionProcessor implements TransactionProcessor<Void> {
  private static final UUID TENANT_ID = UUID.randomUUID();
  private ExecutionContext context;
  private Transaction transaction;

  @Override
  public Void process(Transaction transaction) {
    initExecutionContext(transaction);
    if (transaction.begin(context)) {
      transaction.end(context);
      this.transaction = null;
    }

    return null;
  }

  protected void initExecutionContext(final Transaction transaction) {
    /*
     * close the previous transaction if it was not closed already.
     */
    if (this.transaction != null) {
      this.transaction.end(context);
    }

    this.transaction = transaction;
    context =
        new ExecutionContext() {
          @Override
          public UUID getTenantId() {
            return TENANT_ID;
          }

          @Override
          public File getStorageRoot() {
            return SystemUtils.getJavaIoTmpDir();
          }
        };
  }

  @Override
  public Void process(Payload payload) {
    if (transaction.process(payload)) {
      transaction.end(context);
      transaction = null;
    }

    return null;
  }
}
