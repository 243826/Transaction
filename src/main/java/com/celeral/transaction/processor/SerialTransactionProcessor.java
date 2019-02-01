package com.celeral.transaction.processor;

import java.io.File;
import java.util.UUID;

import com.celeral.transaction.ExecutionContext;
import com.celeral.transaction.Payload;
import com.celeral.transaction.Transaction;
import com.celeral.transaction.TransactionProcessor;

import org.apache.commons.lang3.SystemUtils;

public class SerialTransactionProcessor implements TransactionProcessor
{
  private static final UUID TENANT_ID = UUID.randomUUID();
  private ExecutionContext context;
  private Transaction transaction;

  @Override
  public void process(Transaction transaction)
  {
    initExecutionContext(transaction);
    if (transaction.begin(context)) {
      transaction.end(context);
      this.transaction = null;
    }
  }

  protected void initExecutionContext(final Transaction transaction)
  {
    if (this.transaction != null) {
      this.transaction.end(context);
    }

    this.transaction = transaction;
    context = new ExecutionContext()
    {
      @Override
      public UUID getTenantId()
      {
        return TENANT_ID;
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
