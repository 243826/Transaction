package com.celeral.transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.celeral.transaction.fileupload.UploadPayload;
import com.celeral.transaction.fileupload.UploadPayloadIterator;
import com.celeral.transaction.fileupload.UploadTransaction;
import com.celeral.transaction.fileupload.UploadTransactionHeader;
import com.celeral.transaction.processor.AbstractSerialTransactionProcessor;

public class TransactionTest
{
  static class FileUploadTransaction extends UploadTransaction<FileUploadTransaction> implements UploadTransaction.Document {
    File root;
    File tempFile;

    public FileUploadTransaction(File root) {
      this.root = root;
    }

    @Override public FileUploadTransaction createTemporaryDocument(String path) throws IOException
    {
      tempFile = File.createTempFile(new File(path).getName(), null, root);
      return this;
    }

    @Override public OutputStream openOutputStream() throws IOException
    {
      return new FileOutputStream(tempFile);
    }

    @Override public boolean delete() throws IOException
    {
      return tempFile.delete();
    }

    @Override public boolean renameTo(String path) throws IOException
    {
      File destination = new File(root, path);
          if (!destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
          }
          return tempFile.renameTo(destination);
    }
  }

  static class SerialTransactionProcessor extends AbstractSerialTransactionProcessor
  {
    @Override public Transaction<?, ?> newTransaction()
    {
      String tmpdir = System.getProperty("java.io.tmpdir");
      return new FileUploadTransaction(new File(tmpdir));
    }
  }


  public UploadTransactionHeader getHeader()
  {
    return new UploadTransactionHeader("target/classes/" + UploadTransaction.class.getCanonicalName().replace('.',
                                                                                                              '/') +
                                       ".class");
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
        final UploadPayload next = iterator.next();
        process = processor.process(init.getTransactionId(), next);
        Assert.assertNotEquals("Never Aborted!", Transaction.Result.ABORT, process.getResult());
      }

      Assert.assertNotNull("Process invoked!", process);
      Assert.assertEquals("Process Committed!", Transaction.Result.COMMIT, process.getResult());
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

  public static final Logger logger = LogManager.getLogger(TransactionTest.class);
}