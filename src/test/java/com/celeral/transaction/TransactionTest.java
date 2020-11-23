package com.celeral.transaction;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.celeral.transaction.fileupload.UploadPayloadIterator;
import com.celeral.transaction.fileupload.UploadTransaction;
import com.celeral.transaction.fileupload.UploadTransactionHeader;
import com.celeral.transaction.processor.AbstractSerialTransactionProcessor;

public class TransactionTest
{

  static class SerialTransactionProcessor extends AbstractSerialTransactionProcessor
  {
    @Override public Transaction<?, ?> newTransaction()
    {
      String tmpdir = System.getProperty("java.io.tmpdir");
      return new UploadTransaction(new File(tmpdir));
    }
  }


  public UploadTransactionHeader getHeader()
  {
    return new UploadTransactionHeader("target/classes/" + UploadTransaction.class.getCanonicalName().replace('.',
                                                                                                              '/') +
                                       ".class", 1024);
  }

  public UploadPayloadIterator getPayloadIterator(UploadTransactionHeader header)
  {
    return new UploadPayloadIterator(header, 1024);
  }


  @Test
  public void testSuccessfulTransaction() throws IOException
  {
    SerialTransactionProcessor processor = new SerialTransactionProcessor();

    final UploadTransactionHeader header = getHeader();
    final TransactionProcessor.InitializationResult init = processor.init(header);
    Assert.assertEquals("Initialization Successful!", init.getResult(), Transaction.Result.CONTINUE );
    Assert.assertTrue("Assigned TransactionId!", init.getTransactionId() != 0);

    try (UploadPayloadIterator iterator = getPayloadIterator(header)) {

      TransactionProcessor.ProcessResult process = null;
      while (iterator.hasNext()) {
        process = processor.process(init.getTransactionId(), iterator.next());
        Assert.assertTrue("Never Aborted!", process.getResult() != Transaction.Result.ABORT);
      }

      Assert.assertNotNull("Process invoked!", process);
      Assert.assertTrue("Process Committed!", process.getResult() == Transaction.Result.COMMIT);
    }
  }

  @Test
  public void testTransactionInitializationFailure()
  {

  }

  @Test
  public void testTransactionPayloadFailure()
  {

  }

  @Test
  public void testTransactionCommitFailure()
  {

  }

  @Test
  public void testTransactionAbortFailure()
  {

  }

}