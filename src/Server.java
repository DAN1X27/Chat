import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message){
        try {
            for (Connection connection : connectionMap.values()) {
                connection.send(message);
            }
        }catch (IOException e){
            System.out.println("Произошла ошибка при отправке сообщения");
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Сервер запущен.");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket){
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            while (true){
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if(message.getType() != MessageType.USER_NAME){
                    ConsoleHelper.writeMessage("Неверно введено имя пользователя, пожалуйста введите заново: ");
                    continue;
                }
                String userName = message.getData();
                if(userName.isEmpty()){
                    ConsoleHelper.writeMessage("Имя не может быть пустым");
                    continue;
                }
                if(connectionMap.containsKey(userName)){
                    ConsoleHelper.writeMessage("Имя занято");
                    continue;
                }
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }
        private void notifyUsers(Connection connection, String userName) throws IOException{
            for (String name : connectionMap.keySet()){
                if(name.equals(userName)){
                    continue;
                }
                connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if(message.getType() == MessageType.TEXT){
                    String data = message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + data));
                }else {
                    ConsoleHelper.writeMessage("Ошибка при отправке сообщения.");

                }

            }
        }
        public void run(){
            ConsoleHelper.writeMessage("Установленно новое соединение по адресу: " + socket.getRemoteSocketAddress());
            String userName = null;
            try(Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);


            }catch (IOException e){
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом: " + e.getMessage());
            }catch (ClassNotFoundException e){
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом: " + e.getMessage());
            }
            if(userName != null){
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));

            }
            ConsoleHelper.writeMessage("Соединение с сервером закрыто по адресу " + socket.getRemoteSocketAddress());
        }
    }
}
