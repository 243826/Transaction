package com.celeral.transaction.processor;

import java.io.File;

import com.celeral.transaction.ExecutionContext;
import com.celeral.transaction.Payload;
import com.celeral.transaction.Transaction;
import com.celeral.transaction.TransactionProcessor;

import org.apache.commons.lang3.SystemUtils;

public class SerialTransactionProcessor implements TransactionProcessor
{
  private ExecutionContext context;
  private Transaction transaction;

  @Override
  public void process(Transaction transaction)
  {
    initExecutionContext(transaction);
    if (transaction.begin(context)) {
      transaction.end(context);
    }
    else {
      this.transaction = transaction;
    }
  }

  private void initExecutionContext(final Transaction transaction)
  {
    if (this.transaction != null) {
      this.transaction.end(context);
    }

    context = new ExecutionContext()
    {
      @Override
      public long getTenantId()
      {
        return 0;
      }

      @Override
      public File getStorageRoot()
      {
        return SystemUtils.getJavaIoTmpDir();
      }
    };
  }

  @Override
  public void process(Payload<Transaction> payload)
  {
    if (payload.execute(context, transaction)) {
      transaction.end(context);
      transaction = null;
    }
  }
}
