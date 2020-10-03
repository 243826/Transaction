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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.celeral.transaction.ExecutionContext;
import com.celeral.transaction.Transaction;
import com.celeral.utils.Throwables;

import static com.celeral.utils.Throwables.throwFormatted;

public class UploadTransaction implements Transaction<UploadPayload> {
  static int DEFAULT_CHUNK_SIZE = 1024 * 1024;
  private static final AtomicLong transactionIdGenerator = new AtomicLong();
  private final long id;

  @FieldSerializer.Bind(JavaSerializer.class)
  private final File path;

  private final long size;
  private final long mtime;

  transient UploadTransactionData data;
  private ExecutionContext context;

  protected UploadTransaction() {
    id = 0;
    path = null;
    size = 0;
    mtime = 0;
  }

  public UploadTransaction(String path, int chunkSize) {
    this.id = transactionIdGenerator.incrementAndGet();
    this.path = new File(path);

    if (this.path.exists()) {
      if (this.path.isFile()) {
        if (this.path.canRead()) {
          this.size = this.path.length();
          this.mtime = this.path.lastModified();
        } else {
          throw throwFormatted(
              IllegalArgumentException.class,
              "Unable to read file {} to create fileupload transaction!",
              this.path);
        }
      } else {
        throw throwFormatted(
            IllegalArgumentException.class,
            "Unable to create fileupload transaction with non-regular file {}!",
            this.path);
      }
    } else {
      throw throwFormatted(
          IllegalArgumentException.class,
          "Unable to create fileupload transaction with non existent file {}!",
          this.path);
    }
  }

  public UploadPayloadIterator getPayloadIterator() {
    return new UploadPayloadIterator(DEFAULT_CHUNK_SIZE);
  }

  public class UploadPayloadIterator implements Iterator<UploadPayload>, Closeable {
    private final FileInputStream is;
    private final int blockSize;
    long offset;

    public UploadPayloadIterator(int blockSize) {
      this.blockSize = blockSize;
      try {
        is = new FileInputStream(path);
      } catch (FileNotFoundException ex) {
        throw Throwables.throwFormatted(
            ex,
            RuntimeException.class,
            "Unable to open {} for transaction {}",
            path,
            UploadTransaction.this.getId());
      }
    }

    @Override
    public boolean hasNext() {
      return offset < size;
    }

    @Override
    public UploadPayload next() {
      long nextOffset = offset + blockSize;
      byte[] bytes = new byte[nextOffset < size ? blockSize : (int) (size - offset)];
      try {
        is.read(bytes);
        return new UploadPayload(offset, bytes);
      } catch (IOException ex) {
        throw Throwables.throwFormatted(
            ex,
            RuntimeException.class,
            "Unable to read chunk at offset {} for transaction {} from file {}!",
            offset,
            UploadTransaction.this.getId(),
            path);
      } finally {
        offset = nextOffset;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
      is.close();
    }
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public ReturnValue init(ExecutionContext context) throws IOException {
    this.context = context;

    File tempFile = File.createTempFile(path.getName(), null, context.getStorageRoot());
    RandomAccessFile rw = new RandomAccessFile(tempFile, "rw");
    data = new UploadTransactionData(tempFile, rw);

    return size == 0 ? COMMIT : CONTINUE;
  }

  @Override
  public ReturnValue process(UploadPayload payload) throws IOException {
    RandomAccessFile channel = data.channel;
    channel.seek(payload.offset);
    channel.write(payload.data);

    return payload.offset + payload.data.length == size ? COMMIT : CONTINUE;
  }

  @Override
  public void abort() throws IOException {
    logger.debug("Deleting file {}", data.tempFile);

    try (Closeable unused = data.tempFile::delete){
      data.channel.close();
    }
  }

  public void commit() throws IOException {
    data.channel.close();

    File dpath = new File(context.getStorageRoot(), path.getName());
    data.tempFile.renameTo(dpath);
    logger.debug("Creating file {}", dpath);
  }

  @Override
  public String toString() {
    return "UploadTransaction{"
        + "id="
        + id
        + ", path="
        + path
        + ", size ="
        + size
        + ", mtime="
        + mtime
        + '}';
  }

  private static final Logger logger = LogManager.getLogger(UploadTransaction.class);
}
