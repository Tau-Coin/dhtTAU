## CHANNEL

​		User A: PubKey A

1. #### Personal Info Channel

   ​	Salt: "Personal_Channel"

   ​	Mutable Item -> Personal Info Struct:

      {	

   ​		UserName,

   ​		IcoRootHash,

   ​    	PeerListRoot

   ​	}



​			PeerList Struct:

​			{

​				NextPeerListRoot,

​				Peer1,

​				...,

​				Peern

​			}



2. #### Msg Channel

   ​	Salt: PubKey B

   ​	Mutable Item -> Msg Root Hash



## DATA INTERFACE

#### Personal Info频道数据的请求与回应

1. void RequestPersonalInfoFromPeer(byte[] peer)

2. void OnPersonalInfo(PersonalInfo personalInfo, byte[] peer)

#### Msg频道数据的请求与回应

3. void RequestMsgRootHashFromPeer(byte[] peer)

4. void OnMsgRootHash(byte[] msgRootHash, byte[] peer)

#### 各种immutable数据的请求与回应

5. void RequestRawDataByHash(byte[] hash, Object cbData)

6. void OnRawData(byte[] data, Object cbData)

#### 发布信息相关接口

7. void PublishPersonalInfo(PersonalInfo personalInfo)

8. void PublishMsgRootHash(byte[] msgRootHash)

9. void PublishRawData(byte[] data)
