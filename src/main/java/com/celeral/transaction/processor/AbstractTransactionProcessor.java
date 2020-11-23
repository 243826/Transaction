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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.celeral.transaction.Transaction;
import com.celeral.transaction.TransactionProcessor;
import com.celeral.utils.Throwables;

public abstract class AbstractTransactionProcessor implements TransactionProcessor {

  AtomicLong TRANSACTION_ID_GENERATOR = new AtomicLong();

  static class DetailsConsumer implements Consumer<Object> {
    Object o;
    boolean set;

    @Override public void accept(Object o)
    {
      set = true;
      this.o = o;
    }
  }

  protected long getNextTransactionId() {
    return TRANSACTION_ID_GENERATOR.incrementAndGet();
  }

  public abstract long store(Transaction<?,?> transaction);

  public abstract Transaction<?,?> retrieve(long transactionId);

  public abstract Transaction<?,?> remove(long transactionId);

  public abstract Transaction<?,?> newTransaction();

  @Override
  public InitializationResult init(Object header) {
    @SuppressWarnings("rawtypes")
    Transaction transaction = newTransaction();

    final DetailsConsumer details = new DetailsConsumer();
    final Transaction.Result result;
    try {
      result = transaction.init(header, details);
    } catch (Exception ex) {
      try {
        transaction.abort();
      } catch (Exception ex1) {
        ex.addSuppressed(ex1);
      }

      throw Throwables.throwSneaky(ex);
    }

    switch (result) {
      case CONTINUE:
        long transactionid = store(transaction);
        return new TransactionProcessor.InitializationResultImpl(transactionid, result, details.o);

      case ABORT:
        try {
          transaction.abort();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        }

        return details.set ? new TransactionProcessor.InitializationResultImpl(0, Transaction.Result.ABORT, details.o) :
               InitializationResult.ABORTED;

      case COMMIT:
        try {
          transaction.commit();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        }

        return details.set ? new TransactionProcessor.InitializationResultImpl(0, Transaction.Result.COMMIT, details.o) :
        InitializationResult.COMMITTED;
    }

    throw Throwables.throwFormatted(RuntimeException::new, "Unreachable Statement with result = {}!", result);
  }

  @Override
  public ProcessResult process(long transactionId, Object payload) {
    @SuppressWarnings("rawtypes")
    Transaction transaction = retrieve(transactionId);

    final DetailsConsumer details = new DetailsConsumer();
    final Transaction.Result result;
    try {
      result = transaction.process(payload, details);
    } catch (Exception ex) {
      try {
        transaction.abort();
      } catch (Exception ex1) {
        ex.addSuppressed(ex1);
      }

      throw Throwables.throwSneaky(ex);
    }

    switch (result) {
      case CONTINUE:
        return new ProcessResultImpl(Transaction.Result.CONTINUE, details.o);

      case ABORT:
        try {
          transaction.abort();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        } finally {
          remove(transactionId);
        }
        return details.set ? new ProcessResultImpl(Transaction.Result.ABORT, details.o) : ProcessResult.ABORTED;

      case COMMIT:
        try {
          transaction.commit();
        } catch (Exception ex) {
          throw Throwables.throwSneaky(ex);
        } finally {
          remove(transactionId);
        }
        return details.set ? new ProcessResultImpl(Transaction.Result.CONTINUE, details.o) : ProcessResult.COMMITTED;
    }

    throw Throwables.throwFormatted(RuntimeException::new, "Unreachable Statement with result = {}!", result);
  }
}
