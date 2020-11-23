package com.celeral.transaction.fileupload;

import java.io.File;

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import static com.celeral.utils.Throwables.throwFormatted;

public class UploadTransactionHeader
{
  @FieldSerializer.Bind(JavaSerializer.class)
  private final File path;

  private final long size;
  private final long mtime;

  protected UploadTransactionHeader()
  {
    path = null;
    size = 0;
    mtime = 0;
  }

  public UploadTransactionHeader(String path, int chunkSize)
  {
    this.path = new File(path);

    if (this.getPath().exists()) {
      if (this.getPath().isFile()) {
        if (this.getPath().canRead()) {
          this.size = this.getPath().length();
          this.mtime = this.getPath().lastModified();
        }
        else {
          throw throwFormatted(IllegalArgumentException.class,
                               "Unable to read file {} to create fileupload transaction!", this.getPath());
        }
      }
      else {
        throw throwFormatted(IllegalArgumentException.class,
                             "Unable to create fileupload transaction with non-regular file {}!", this.getPath());
      }
    }
    else {
      throw throwFormatted(IllegalArgumentException.class,
                           "Unable to create fileupload transaction with non existent file {}!", this.getPath());
    }
  }

  public File getPath()
  {
    return path;
  }

  public long getSize()
  {
    return size;
  }

  public long getModifiedTime()
  {
    return mtime;
  }

  @Override
  public String toString() {
    return "UploadTransaction{"
           + "path="
           + getPath()
           + ", size ="
           + getSize()
           + ", mtime="
           + getModifiedTime()
           + '}';
  }
}
