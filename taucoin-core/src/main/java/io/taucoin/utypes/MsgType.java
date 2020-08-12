/**
Copyright 2020 taucoin developer

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.taucoin.utypes;

/**
 * transaction type used to create transaction.
 * 0:community genesis message.
 * 1:Regular forum message.
 * 2:forum message comment.
 * 3:Community Announcement message.
 * 4:wire transaction.
 * 5:Identity Announcement.
 * 6:bootstrap node Announcement.
 */
public enum MsgType {

    GenesisMsgTx(0), //genesis transaction
    ForumNoteTx(1),       //forum note transaction
    WiringTx(2);     //wiring transaction

    int index;

    MsgType(int value){
        this.index = value;
    }

    public byte getVaLue(){
       return (byte)index;
    }

    public static MsgType setValue(byte value){
    
        if (value == 0){
            return GenesisMsgTx;
        }else if (value == 1){
            return ForumNoteTx;
        }else if (value == 2){
            return WiringTx;
        }
        return null;
    }

    public static MsgType setValue(long value){
        if (value == '0'){
            return GenesisMsgTx;
        }else if (value == '1'){
            return ForumNoteTx;
        }else if (value == '2'){
            return WiringTx;
        }
        return null;
    }
}
