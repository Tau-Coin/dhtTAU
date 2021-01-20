package io.taucoin.types;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

public class ExchangeMutableData {
    private List<Message> messageList = new CopyOnWriteArrayList<>();

    private byte[] rlpEncoded;
    private boolean parsed = false;

    public ExchangeMutableData(List<Message> messageList) {
        this.messageList = messageList;

        this.parsed = true;
    }

    public ExchangeMutableData(byte[] encode) {
        this.rlpEncoded = encode;
    }

    public List<Message> getMessageList() {
        if (!parsed) {
            parseRLP();
        }

        return messageList;
    }

    private void parseList(RLPList list) {
        for (int i = 0; i < list.size(); i++) {
            byte[] encode = list.get(i).getRLPData();
            this.messageList.add(new Message(encode));
        }
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        parseList((RLPList) list.get(0));

        this.parsed = true;
    }

    public byte[] getMessageListEncoded() {
        byte[][] messageListEncoded = new byte[this.messageList.size()][];
        int i = 0;
        for (Message message : this.messageList) {
            messageListEncoded[i] = message.getEncoded();
            ++i;
        }
        return RLP.encodeList(messageListEncoded);
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] messageListEncoded = getMessageListEncoded();

            this.rlpEncoded = RLP.encodeList(messageListEncoded);
        }

        return rlpEncoded;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();

        List<Message> messageList = getMessageList();
        if (null != messageList) {
            for (Message message: messageList) {
                list.add(message.toString());
            }
        }

        return "ExchangeMutableData{"  + list + '}';
    }

}
