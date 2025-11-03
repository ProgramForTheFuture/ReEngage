import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TelnetClient {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    public TelnetClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream())
        );
        writer = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream()), 
            true
        );
        System.out.println("Connected to " + host + ":" + port);
    }
    
    public void sendMessage(String message) {
        writer.println(message);
    }
    
    public String receiveMessage() throws IOException {
        return reader.readLine();
    }
    
    public void close() throws IOException {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
        if (socket != null) socket.close();
    }
    
    public static void main(String[] args) {
        TelnetClient client = new TelnetClient("localhost", 9000);
        Scanner scanner = new Scanner(System.in);
        
        try {
            client.connect();
            
            // Read welcome message
            String welcome = client.receiveMessage();
            System.out.println(welcome);
            
            // Interactive loop
            while (true) {
                System.out.print("Enter message (or 'quit' to exit): ");
                String input = scanner.nextLine();
                
                // Send message to server
                client.sendMessage(input);
                
                // Receive response
                String response = client.receiveMessage();
                System.out.println("Server: " + response);
                
                if (input.equalsIgnoreCase("quit")) {
                    break;
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                client.close();
                scanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}