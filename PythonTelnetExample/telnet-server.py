import socket
import threading

class TelnetServer:
    def __init__(self, host='localhost', port=23):
        self.host = host
        self.port = port
        self.server_socket = None
        
    def handle_client(self, client_socket, address):
        """Handle individual client connections"""
        print(f"New connection from {address}")
        
        try:
            # Send welcome message
            welcome = "Welcome to Python Telnet Server\r\n"
            client_socket.send(welcome.encode('utf-8'))
            
            while True:
                # Receive data from client
                data = client_socket.recv(1024)
                if not data:
                    break
                
                # Decode the received data
                message = data.decode('utf-8').strip()
                print(f"Received from {address}: {message}")
                
                # Process the request and send response
                if message.lower() == 'quit':
                    response = "Goodbye!\r\n"
                    client_socket.send(response.encode('utf-8'))
                    break
                elif message.lower() == 'hello':
                    response = "Hello from Python server!\r\n"
                elif message.lower().startswith('echo '):
                    response = f"Echo: {message[5:]}\r\n"
                else:
                    response = f"You sent: {message}\r\n"
                
                client_socket.send(response.encode('utf-8'))
                
        except Exception as e:
            print(f"Error handling client {address}: {e}")
        finally:
            client_socket.close()
            print(f"Connection closed: {address}")
    
    def start(self):
        """Start the telnet server"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            print(f"Telnet server listening on {self.host}:{self.port}")
            
            while True:
                client_socket, address = self.server_socket.accept()
                
                # Handle each client in a separate thread
                client_thread = threading.Thread(
                    target=self.handle_client,
                    args=(client_socket, address)
                )
                client_thread.daemon = True
                client_thread.start()
                
        except KeyboardInterrupt:
            print("\nShutting down server...")
        finally:
            if self.server_socket:
                self.server_socket.close()

if __name__ == "__main__":
    # Create and start the server
    server = TelnetServer(host='localhost', port=9000)
    server.start()