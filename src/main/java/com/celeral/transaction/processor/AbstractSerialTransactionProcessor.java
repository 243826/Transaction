/*
 * Copyright © 2021 Celeral.
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

import com.celeral.transaction.Transaction;

public abstract class AbstractSerialTransactionProcessor extends AbstractTransactionProcessor {
  Transaction<?, ?> currentTransaction;

  @Override
  public long store(Transaction<?, ?> transaction) {
    currentTransaction = transaction;
    return getNextTransactionId();
  }

  @Override
  public Transaction<?, ?> retrieve(long transactionId) {
    return currentTransaction;
  }

  @Override
  public Transaction<?, ?> remove(long transactionId) {
    try {
      return currentTransaction;
    } finally {
      currentTransaction = null;
    }
  }
}
