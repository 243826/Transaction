package com.celeral.transaction.fileupload;

import java.io.File;

import static com.celeral.utils.Throwables.throwFormatted;

public class UploadTransactionHeader
{
  private final String path;
  private final long size;
  private final long mtime;

  protected UploadTransactionHeader()
  {
    path = null;
    size = 0;
    mtime = 0;
  }

  public UploadTransactionHeader(String path)
  {
    this.path = path;

    File file = new File(path);
    if (file.exists()) {
      if (file.isFile()) {
        if (file.canRead()) {
          this.size = file.length();
          this.mtime = file.lastModified();
        }
        else {
          throw throwFormatted(IllegalArgumentException.class,
                               "Unable to read file {} to create fileupload transaction!", file);
        }
      }
      else {
        throw throwFormatted(IllegalArgumentException.class,
                             "Unable to create fileupload transaction with non-regular file {}!", file);
      }
    }
    else {
      throw throwFormatted(IllegalArgumentException.class,
                           "Unable to create fileupload transaction with non existent file {}!", file);
    }
  }

  public String getPath()
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
