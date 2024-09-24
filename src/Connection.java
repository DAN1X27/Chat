import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

public class Connection implements Closeable {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        OutputStream outputStream = socket.getOutputStream();
        out = new ObjectOutputStream(outputStream);
        InputStream inputStream = socket.getInputStream();
        in = new ObjectInputStream(inputStream);

    }
    public void send(Message message) throws IOException {
        synchronized (out) {
            out.writeObject(message);
        }
    }
    public Message receive() throws IOException, ClassNotFoundException{
        synchronized (in) {
            Message message;
            return message = (Message) in.readObject();
        }
    }
    public SocketAddress getRemoteSocketAddress(){
        return socket.getRemoteSocketAddress();
    }
    public void close() throws IOException{
        socket.close();
        out.close();
        in.close();
    }
}
