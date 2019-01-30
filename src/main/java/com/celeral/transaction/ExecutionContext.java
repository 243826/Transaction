package com.celeral.transaction;

import java.io.File;
import java.util.UUID;

public interface ExecutionContext
{
  /**
   * Gets id of the tenant who initiated the transaction
   * @return id of the initiating tenant
   */
  UUID getTenantId();

  /**
   * Gets the storage root specific to the tenant
   * @return
   */
  File getStorageRoot();
}
