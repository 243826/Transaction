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
package com.celeral.transaction.fileupload;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.celeral.transaction.Transaction;

import static com.celeral.transaction.Transaction.Result.COMMIT;
import static com.celeral.transaction.Transaction.Result.CONTINUE;

public abstract class UploadTransaction<D extends UploadTransaction.Document> implements Transaction<UploadTransactionHeader,
                                                                                       UploadPayload> {

  public interface Document {

    OutputStream openOutputStream() throws IOException;

    boolean delete() throws IOException;

    boolean renameTo(String path) throws IOException;
  }

  private OutputStream channel;
  private D tempFile;
  private long size;
  private String path;
  Closeable aborter = () -> {};

  public abstract D createTemporaryDocument(String path) throws IOException;

  @Override
  public Result init(UploadTransactionHeader header, Consumer<Object> details) throws IOException {
    size = header.getSize();
    path = header.getPath();

    tempFile = createTemporaryDocument(path);
    try {
      channel = tempFile.openOutputStream();
      aborter = () -> { try (Closeable unused = tempFile::delete) {channel.close();}};
    }
    catch (Throwable th) {
      aborter = () -> channel.close();
    }
    return size == 0 ? COMMIT : CONTINUE;
  }

  @Override
  public Result process(UploadPayload payload, Consumer<Object> details) throws IOException {
    channel.write(payload.data);

    return payload.offset + payload.data.length == size ? COMMIT : CONTINUE;
  }

  @Override
  public void abort() throws IOException {
    logger.debug("Deleting file {}", tempFile);
    aborter.close();
  }

  public void commit() throws IOException {
    channel.close();
    tempFile.renameTo(path);
    logger.debug("Creating file {}", path);
  }

  private static final Logger logger = LogManager.getLogger(UploadTransaction.class);
}
