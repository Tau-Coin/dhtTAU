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
package io.taucoin.types;

/**
 * transaction type used to create transaction.
 */
public enum MsgType {
    TorrentPublish(0),
    Wiring(1),
    BootStrapNodeAnnouncement(2),
    CommunityAnnouncement(3);
    int index;
    MsgType(int value){
        this.index = value;
    }
    public byte getVaLue(){
       return (byte)index;
    }
    public static MsgType setValue(byte value){
        if (value == 0){
            return TorrentPublish;
        }else if (value == 1){
            return Wiring;
        }else if (value == 2){
            return BootStrapNodeAnnouncement;
        }else if (value == 3){
            return CommunityAnnouncement;
        }
        return null;
    }
}
