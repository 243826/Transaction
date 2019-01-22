package com.celeral.transaction;

public interface TransactionProcessor
{
  void process(Transaction transaction);

  void process(Payload<Transaction> payload);
}
