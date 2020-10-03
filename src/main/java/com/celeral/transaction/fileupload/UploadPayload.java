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

import java.util.zip.Adler32;

public class UploadPayload {
  static final Adler32 checksumComputer = new Adler32();

  static synchronized long computeChecksum(byte[] bytes) {
    checksumComputer.reset();
    checksumComputer.update(bytes, 0, bytes.length);
    return checksumComputer.getValue();
  }

  long offset;
  byte[] data;
  long checksum;

  private UploadPayload() {
    /* for serialization */
  }

  public UploadPayload(long offset, byte[] data) {
    this.offset = offset;
    this.data = data;
    this.checksum = computeChecksum(data);
  }

  @Override
  public String toString() {
    return "UploadPayload{" + "data=" + data.length + ", sequenceId=" + offset + '}';
  }
}
