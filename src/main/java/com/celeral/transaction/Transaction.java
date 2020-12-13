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

import java.util.function.Consumer;

public interface Transaction<H, P> {
  enum Result {
    CONTINUE,
    COMMIT,
    ABORT,
    SKIP
  }

  /**
   * Initializes the transaction by optionally sending the header information.
   * The header information is used to do pre-checks and sending the response
   * back which the transaction initiator could analyze to plan out the subsequent
   * actions related to the transaction.
   *
   * @param header  information useful for pre-qualifying the transaction
   * @return information useful to plan out the subsequent transaction action
   * @throws Exception exceptions received while pre-qualifying the transaction
   */
  Result init(H header, Consumer<Object> details) throws Exception;

  /**
   * Processes the payloads associated with the transaction. This call could
   * be made multiple times if there are many payloads associated with a single
   * transaction
   * @param payload useful to carry out the transaction
   * @return status of the payload processing
   * @throws Exception exceptions received while processing the payload
   */
  Result process(P payload, Consumer<Object> details) throws Exception;

  /**
   * Commits the transaction
   * @throws Exception
   */
  void commit() throws Exception;

  /**
   * Performs the rollback of the transaction
   * @throws Exception
   */
  void abort() throws Exception;
}
