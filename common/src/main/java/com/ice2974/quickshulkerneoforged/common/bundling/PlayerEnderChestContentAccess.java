package com.ice2974.quickshulkerneoforged.common.bundling;

import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.List;

public interface PlayerEnderChestContentAccess<P, S> {
    List<S> readPlayerEnderChestContents(P playerHandle);

    ContentWriteResult writePlayerEnderChestContents(P playerHandle, List<S> contents);
}
