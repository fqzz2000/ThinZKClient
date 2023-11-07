// import connectRequest fron ConnectRequest.java;
package ThinZKClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.proto.RequestHeader;
import org.apache.zookeeper.proto.ReplyHeader;
import org.apache.zookeeper.proto.CreateRequest;
import org.apache.zookeeper.proto.CreateResponse;
import org.apache.zookeeper.proto.ConnectRequest;
import org.apache.zookeeper.server.Request;
import org.apache.zookeeper.server.ByteBufferInputStream;


public class App {
    static volatile long lastZxid;
    static int sessionTimeout;
    static int sessionId;
    static byte[] sessionPasswd;
    static SocketChannel sock;
    public static void main(String[] args) throws IOException, InterruptedException {
        lastZxid = 0;
        sessionTimeout = 30000;
        sessionId = 0;
        sessionPasswd = new byte[16];
        initSocket("localhost");
        connect();
        createEphemeral("/transient", null);
        while (true) {
            Thread.sleep(1000);
        }
    }
    // establish a tcp connection
    public static void initSocket(String addrStr) throws IOException, InterruptedException {
        // create socket
        sock = SocketChannel.open();
        sock.configureBlocking(true);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        sock.socket().setKeepAlive(true);
        InetSocketAddress addr = new InetSocketAddress(addrStr, 2181);
        sock.socket().connect(addr);

    }
    // establish a zookeeper session
    public static void connect() throws IOException, InterruptedException {
        // send connect request
        ConnectRequest conReq = new ConnectRequest(0, lastZxid,
                    sessionTimeout, sessionId, sessionPasswd);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
        boa.writeInt(-1, "len");
        conReq.serialize(boa, "connect");
        baos.close();
        ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());
        bb.putInt(bb.capacity() - 4);
        bb.rewind();
        // bb is the payload to be sent
        sock.write(bb);

        // read response to be implemented
        ByteBuffer incomingBuffer = ByteBuffer.allocateDirect(1024);
        sock.read(incomingBuffer);
        // System.out.println(incomingBuffer);
        ByteBufferInputStream bbis = new ByteBufferInputStream(
                    incomingBuffer);
        BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
        ReplyHeader replyHdr = new ReplyHeader();

        replyHdr.deserialize(bbia, "header");
        System.out.println("receiving connect reply");
        System.out.println(replyHdr.toString());

    }   

    // send a create ephemeral node request
    public static void createEphemeral(final String path, byte data[]) throws IOException{
        final String clientPath = path;
        RequestHeader h = new RequestHeader();
                h.setType(ZooDefs.OpCode.create);
        CreateRequest request = new CreateRequest();
        CreateResponse response = new CreateResponse();
        request.setData(data);
        request.setFlags(1);
        request.setPath(clientPath);
        request.setAcl(Ids.OPEN_ACL_UNSAFE);
        ByteBuffer bb;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryOutputArchive boa = BinaryOutputArchive
                .getArchive(baos);
        boa.writeInt(-1, "len"); // We'll fill this in later
        h.serialize(boa, "header");
      
        request.serialize(boa, "request");
        
        baos.close();
        bb = ByteBuffer.wrap(baos.toByteArray());
        bb.putInt(bb.capacity() - 4);
        bb.rewind();
        sock.write(bb);
        ByteBuffer incomingBuffer = ByteBuffer.allocateDirect(1024);
        sock.read(incomingBuffer);

        ByteBufferInputStream bbis = new ByteBufferInputStream(
                    incomingBuffer);
        BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
        ReplyHeader replyHdr = new ReplyHeader();

        replyHdr.deserialize(bbia, "header");
        System.out.println("receiving create reply");
        System.out.println(replyHdr.toString());
    }
}
