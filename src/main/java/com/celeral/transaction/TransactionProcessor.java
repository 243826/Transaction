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

public interface TransactionProcessor {

  interface ProcessResult {
    Transaction.Result getResult();
    Object getDetails();

    ProcessResult ABORTED = new ProcessResult() {
      @Override public Transaction.Result getResult()
      {
        return Transaction.Result.ABORT;
      }

      @Override public Object getDetails()
      {
        return null;
      }
    };

    ProcessResult COMMITTED = new ProcessResult() {
      @Override public Transaction.Result getResult()
      {
        return Transaction.Result.COMMIT;
      }

      @Override public Object getDetails()
      {
        return null;
      }
    };
  }

  interface InitializationResult extends ProcessResult {
    long getTransactionId();

    InitializationResult ABORTED = new InitializationResult() {
      @Override public long getTransactionId()
      {
        return 0;
      }

      @Override public Transaction.Result getResult()
      {
        return Transaction.Result.ABORT;
      }

      @Override public Object getDetails()
      {
        return null;
      }
    };


    InitializationResult COMMITTED = new InitializationResult() {
      @Override public long getTransactionId()
      {
        return 0;
      }

      @Override public Transaction.Result getResult()
      {
        return Transaction.Result.COMMIT;
      }

      @Override public Object getDetails()
      {
        return null;
      }
    };
  }



  class ProcessResultImpl implements ProcessResult {
    private final Transaction.Result result;
    private final Object details;

    public ProcessResultImpl(Transaction.Result result, Object details) {
      this.result = result;
      this.details = details;
    }

    public Transaction.Result getResult() {
      return result;
    }

    public Object getDetails() {
      return details;
    }
  }

  class InitializationResultImpl extends ProcessResultImpl implements InitializationResult {
    long transactionId;
    public InitializationResultImpl(long transactionId, Transaction.Result result, Object details)
    {
      super(result, details);
      this.transactionId = transactionId;
    }

    public long getTransactionId() {
      return transactionId;
    }

  }


  InitializationResult init(Object header);

  ProcessResult process(long transactionId, Object payload);
}
