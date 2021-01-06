package io.taucoin.torrent.publishing.core.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天消息拆分工具
 */
public class MsgSplitUtil {
    private static final int BYTE_LIMIT = 900; // 消息拆分字节限制

    /**
     * 拆分文本消息
     * @param textMsg 文本消息
     * @return 拆分的消息列表
     */
    public static List<byte[]> splitTextMsg(String textMsg) {
        List<byte[]> list = new ArrayList<>();
        int msgSize = textMsg.length();
        int statPos = 0;
        int endPos = 1;
        byte[] lastFragmentBytes = null;
        do {
            if (endPos >= msgSize) {
                endPos = msgSize;
            }
            String fragment = textMsg.substring(statPos, endPos);
            byte[] fragmentBytes = fragment.getBytes(StandardCharsets.UTF_8);
            if (fragmentBytes.length > BYTE_LIMIT) {
                // 切片字节大于限制，上一次的切片作为最新切片
                statPos = endPos - 1;
                if (lastFragmentBytes != null) {
                    list.add(lastFragmentBytes);
                }
            } else if (fragmentBytes.length == BYTE_LIMIT || endPos == msgSize) {
                // 切片字节等于限制或者消息结束，当前切片为最新切片
                statPos = endPos;
                list.add(fragmentBytes);
            } else {
                // 切片字节小于限制，直接跳到下一切片
                endPos += 1;
            }
            lastFragmentBytes = fragmentBytes;
        } while (statPos < msgSize);
        return list;
    }

    public static String textMsgToString(byte[] msg) {
        return new String(msg, StandardCharsets.UTF_8);
    }
}