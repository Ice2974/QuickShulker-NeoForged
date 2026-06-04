package com.ice2974.quickshulkerneoforged.common.content;

public interface ContainerContentAccess<H> {
    ContainerContentSnapshot readContents(H hostHandle);

    ContentWriteResult writeContents(H hostHandle, ContainerContentSnapshot snapshot);
}
