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

import com.celeral.utils.Throwables;

import com.celeral.transaction.ExecutionContext;
import com.celeral.transaction.Transaction;
import com.celeral.transaction.TransactionProcessor;

public abstract class AbstractTransactionProcessor implements TransactionProcessor {
  private static final UUID TENANT_ID = UUID.randomUUID();

  public abstract long store(Transaction<?> transaction);

  public abstract Transaction<?> retrieve(long transactionId);

  public abstract Transaction<?> remove(long transactionId);

  @Override
  public Transaction.ReturnValue init(Transaction<?> transaction) {
    final Transaction.ReturnValue returnValue;
    try {
      returnValue =
          transaction.init(
              new ExecutionContext() {
                @Override
                public UUID getTenantId() {
                  return TENANT_ID;
                }

                @Override
                public File getStorageRoot() {
                  return SystemUtils.getJavaIoTmpDir();
                }
              });
    } catch (Exception ex) {
      try {
        transaction.abort();
      } catch (Exception ex1) {
        ex.addSuppressed(ex1);
      }

      throw Throwables.throwSneaky(ex);
    }

    switch (returnValue.getResult()) {
      case CONTINUE:
        store(transaction);
        break;

      case ABORT:
        try {
          transaction.abort();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        }
        break;

      case COMMIT:
        try {
          transaction.commit();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        }
        break;
    }

    return returnValue;
  }

  @Override
  public Transaction.ReturnValue process(long transactionId, Object payload) {
    @SuppressWarnings("unchecked")
    Transaction<Object> transaction = (Transaction<Object>) retrieve(transactionId);
    final Transaction.ReturnValue returnValue;
    try {
      returnValue = transaction.process(payload);
    } catch (Exception ex) {
      try {
        transaction.abort();
      } catch (Exception ex1) {
        ex.addSuppressed(ex1);
      }

      throw Throwables.throwSneaky(ex);
    }

    switch (returnValue.getResult()) {
      case CONTINUE:
        break;

      case ABORT:
        try {
          transaction.abort();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        } finally {
          remove(transactionId);
        }
        break;

      case COMMIT:
        try {
          transaction.commit();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        } finally {
          remove(transactionId);
        }
        break;
    }

    return returnValue;
  }
}
