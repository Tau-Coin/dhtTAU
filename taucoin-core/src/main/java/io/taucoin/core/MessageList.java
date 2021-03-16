package io.taucoin.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.types.Message;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class MessageList {
    private List<Message> messageList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public MessageList(List<Message> messageList) {
        this.messageList = messageList;

        this.parsed = true;
    }

    public MessageList(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public List<Message> getMessageList() {
        if (!parsed) {
            parseRLP();
        }

        return messageList;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.messageList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            byte[] msgBytes = list.get(i).getRLPData();
            if (null != msgBytes) {
                this.messageList.add(new Message(msgBytes));
            }
        }

        this.parsed = true;
    }

    /**
     * get encoded
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            if (null != this.messageList) {
                byte[][] encodeList = new byte[this.messageList.size()][];

                int i = 0;
                for (Message message : this.messageList) {
                    encodeList[i] = RLP.encodeElement(message.getEncoded());
                    i++;
                }

                rlpEncoded = RLP.encodeList(encodeList);
            }
        }

        return rlpEncoded;
    }
}
