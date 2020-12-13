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
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.celeral.transaction.Transaction;

import static com.celeral.transaction.Transaction.Result.COMMIT;
import static com.celeral.transaction.Transaction.Result.CONTINUE;

public class UploadTransaction implements Transaction<UploadTransactionHeader, UploadPayload> {

  protected final File root;
  private RandomAccessFile channel;
  private File tempFile;
  private long size;
  private String path;

  public UploadTransaction(File root) {
    this.root = root;
  }

  @Override
  public Result init(UploadTransactionHeader header, Consumer<Object> details) throws IOException {
    size = header.getSize();
    path = header.getPath();

    File file = new File(header.getPath());
    String filename = file.getName();

    tempFile = File.createTempFile(filename, null, root);
    channel = new RandomAccessFile(tempFile, "rw");
    return size == 0 ? COMMIT : CONTINUE;
  }

  @Override
  public Result process(UploadPayload payload, Consumer<Object> details) throws IOException {
    channel.seek(payload.offset);
    channel.write(payload.data);

    return payload.offset + payload.data.length == size ? COMMIT : CONTINUE;
  }

  @Override
  public void abort() throws IOException {
    logger.debug("Deleting file {}", tempFile);

    try (Closeable unused = tempFile::delete) {
      channel.close();
    }
  }

  public void commit() throws IOException {
    channel.close();

    File destination = new File(root, path);
    if (!destination.getParentFile().exists()) {
      destination.getParentFile().mkdirs();
    }
    tempFile.renameTo(destination);
    logger.debug("Creating file {}", destination);
  }

  private static final Logger logger = LogManager.getLogger(UploadTransaction.class);
}
