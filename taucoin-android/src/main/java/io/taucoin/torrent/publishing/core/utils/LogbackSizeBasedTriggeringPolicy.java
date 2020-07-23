package io.taucoin.torrent.publishing.core.utils;

import java.io.File;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;

public class LogbackSizeBasedTriggeringPolicy<E> extends
        SizeBasedTriggeringPolicy<E> {

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        return activeFile.length() >= FileSize.valueOf(getMaxFileSize())
                .getSize();
    }
}
