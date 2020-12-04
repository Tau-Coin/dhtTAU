## 数据编解码

内存中的数据对象（基本类型，array, list, map等等）只有转换为二进制流才可以进行数据持久化和网络传输，是每个软件以及软件工程师必须面对和解决的基本问题。

序列化需要保留充分的信息以恢复数据对象，但是为了节约存储空间和网络带宽，序列化后的二进制流又要尽可能小。

### 编解码考虑的出发点
在dhtTAU系统中，我们是基于libtorrent的DHT ITEM系统进行数据的网络收发的。在libtorrent dht系统中，[bencode](http://bittorrent.org/beps/bep_0005.html)是既定的编码方式，但是在应用层并不能直接调用bencode方法，因为在DHT系统中，引入了一个核心的中间类型-`entry`，收发的数据需要预先构建`entry`，然后进入libtorrent-dht系统中，构建好的`entry`会根据之前送入数据的形式来进行相应的becode编码，详见 `libtorrent/include/libtorrent/bencode.hpp-bencode_recursive`。
其中，我们需要注意的是，尽管entry支持如下多种类型：
- entry::int_t
 > write_integer中将int转换为char来序列化；
- entry::string_t
 > write_string中调用c_str()
- entry::list_t, entry::dictionary_t
 > 以int_t，string_t为基础
- entry::preformatted_t
 > 这个数据类型并没有decode方法与之配套，如果用该数据类型来构建entry会存在问题，为此我们还和libtorrent的作者进行了交流，https://github.com/arvidn/libtorrent/issues/5250
- entry::undefined_t
 > 不支持的数据类型，正常情况下用不到。

最终编码的数据都是以`char`形式来呈现的，我们知道`char`在c/c++中是一个字节；

为了解决编程语言间的调用问题，我们引入了[jlibtorrent](https://github.com/frostwire/frostwire-jlibtorrent)，在jlibtorrent中，构建entry的公共接口我们建议使用`jlibtorrent/swig/entry.java`提供的构造方法或者静态构建方法，原因有如下几点：
- `jlibtorrent/Entry.java`中的构造方法或者静态构建方法是以`jlibtorrent/swig/entry.java`为基础的，且形式较为单一
- java中默认编码是unicode，或者和系统一致(基本为UTF-8)，如果直接利用string来构建entry，会引发数据的数倍扩增，这部分代码要追踪到`swig/libtorrent_jni.cpp`
> jenv->ReleaseStringUTFChars //引用了编码方式，当码值大于128时会引发扩增；
- 如果利用`int`, `long`来构建`entry`，会有如下问题。在`entry`内部运用了benode的编码方式，对应于`integer`，会转换为`char`来处理，也就是说对于大小为`10000`的int值编码，最终会为`"10000"`，变为5个字节，这样会引发数据的进一步扩增；
- 对于应用层来说，不要引入语言层面的编码方式，直接送入二进制流(字节流)是最好的方式，我们知道java的`byte`等价于c++中的`char`，都为1个字节，为此我们采用了如下方法来构建`entry`:

		1. public static byte_vector bytes2byte_vector(byte[] arr) //byte[] -> byte_vector
		
        2. public static entry from_string_bytes(byte_vector string_bytes) // byte_vector -> entry

综上所述，dhtTAU中的编码问题变为如何将数据转换为`byte[]`，以及从`byte[]`到目标数据，这个和数据编解码面对的问题是一致的，只不过我们现在更加明确数据在libtorrent dht系统中是如何运作的。

### Why RLP
常见的序列化/反序列化方法有[json](http://json.com/), [protobuf](https://github.com/protocolbuffers/protobuf), xml等等。在开源项目以太坊中，采用了[RLP](https://github.com/ethereum/wiki/wiki/RLP)编解码方式。

相比于protobuf而言, RLP简单，高效，一致性好。[Ethereum选择RLP的理由](https://eth.wiki/en/fundamentals/design-rationale)。

对应于dhtTAU中的数据测试，以Block数据为例，我们对比了protobuf和RLP在效率以及序列化大小两方面，RLP都是优于protobuf的。

- 序列化效率


	RLP encoded time: 1,070,965 ns
    Protobuf encoded time: 45,338,164 ns

- 序列化大小


    RLP encoded bytes size: 227 bytes
    Protobuf encoded bytes size: 235 bytes

- 反序列化效率

 
	RLP decoded time: 1,038,807 ns
    Protobuf decoded time: 1,288,139 ns




