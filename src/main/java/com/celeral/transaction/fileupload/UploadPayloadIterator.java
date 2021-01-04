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
package com.celeral.transaction.fileupload;

import java.io.Closeable;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import com.celeral.utils.Throwables;

public class UploadPayloadIterator implements Iterator<UploadPayload>, Closeable {
  private final FileInputStream is;
  private final int blockSize;
  private final UploadTransactionHeader header;
  long offset;

  public UploadPayloadIterator(UploadTransactionHeader header, int blockSize) {
    this.blockSize = blockSize;
    this.header = header;
    try {
      is = new FileInputStream(header.getPath());
    } catch (FileNotFoundException ex) {
      throw Throwables.throwFormatted(
          ex, RuntimeException.class, "Unable to open {}!", header.getPath());
    }
  }

  @Override
  public boolean hasNext() {
    return offset < header.getSize();
  }

  @Override
  public UploadPayload next() {
    long nextOffset = offset + blockSize;
    int length = nextOffset < header.getSize() ? blockSize : (int) (header.getSize() - offset);
    byte[] bytes = new byte[length];
    try {
      int offset = 0;
      int read = is.read(bytes);
      if (read == -1) {
        throw new EOFException();
      }
      while (read < length) {
        length -= read;
        offset += read;
        read = is.read(bytes, offset, length);
        if (read == -1) {
          throw new EOFException();
        }
      }
      return new UploadPayload(this.offset, bytes);
    } catch (IOException ex) {
      throw Throwables.throwFormatted(
          ex,
          RuntimeException.class,
          "Unable to read chunk at offset {} from file {}!",
          offset,
          header.getPath());
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
