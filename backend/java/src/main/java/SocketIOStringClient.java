import java.net.URISyntaxException;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * 使用socket.io-client实现的与对应{@link SocketIOStringServer}的客户端
 */
public class SocketIOStringClient {

    public static void main(String[] args) throws URISyntaxException {
        final Socket socket = IO.socket("http://localhost:3001");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

          @Override
          public void call(Object... args) {
              System.out.println(Socket.EVENT_CONNECT+": "+Arrays.toString(args));
            //socket.emit(Socket.EVENT_MESSAGE, "hello world!123");
            socket.send("hello world!456");
          }

        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {

          @Override
          public void call(Object... args) {
              System.out.println(Socket.EVENT_MESSAGE+": "+Arrays.toString(args));
          }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

          @Override
          public void call(Object... args) {
              System.out.println(Socket.EVENT_DISCONNECT+": "+Arrays.toString(args));
          }

        });
        socket.connect();
        System.out.println("socket.connect()");
        disconnect(socket);
    }
    
    private static void disconnect(final Socket socket){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socket.disconnect();
    }
    
}
