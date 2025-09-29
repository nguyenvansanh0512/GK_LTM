package socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import ui.ChatWindow;

public class SocketConnector {
    // File này không còn cần thiết, logic đã được chuyển sang ChatApp để xử lý đa kết nối.
    // Các phương thức sau chỉ là giữ chỗ để không gây lỗi biên dịch nếu vẫn được gọi.
    
    public void startServer(int port, ChatWindow chatWindow) throws IOException {
        throw new UnsupportedOperationException("Sử dụng logic ServerSocket trong ChatApp.");
    }

    public void startClient(String host, int port, ChatWindow chatWindow) throws IOException {
        throw new UnsupportedOperationException("Sử dụng logic Socket trong ChatApp.");
    }
}