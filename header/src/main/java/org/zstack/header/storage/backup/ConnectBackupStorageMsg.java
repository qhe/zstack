package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class ConnectBackupStorageMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String backupStorageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
