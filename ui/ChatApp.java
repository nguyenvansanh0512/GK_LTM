package ui; 

import ui.ChatWindow;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import chat.ChatHandler;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

// Triển khai interface để nhận thông báo khi một kết nối bị đóng
public class ChatApp implements ChatHandler.ConnectionClosedListener {
    public static final int PORT = 12345;
    // Danh sách để quản lý tất cả các kết nối đang hoạt động
    private final List<ChatHandler> activeHandlers = Collections.synchronizedList(new LinkedList<>());
    private ChatWindow chatWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatApp().start());
    }

    private void start() {
        chatWindow = new ChatWindow();
        
        // 1. Luôn khởi chạy Server lắng nghe trong luồng riêng
        startServerListener();

        // 2. Hỏi người dùng có muốn kết nối Client không
        String host = JOptionPane.showInputDialog(chatWindow, "Nhập IP của peer muốn kết nối (bỏ trống nếu chỉ chờ):", "Thiết lập Client", JOptionPane.QUESTION_MESSAGE);
        
        if (host != null && !host.trim().isEmpty()) {
            startClientConnection(host.trim());
        }

        // 3. Gắn hành động gửi tin nhắn / gửi file (sẽ gửi đi TẤT CẢ các handler)
        attachActions();
    }
    
    private void startServerListener() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                chatWindow.appendMessage("System", "Server đang lắng nghe trên cổng " + PORT + "...");
                while (true) {
                    Socket socket = serverSocket.accept();
                    String peerAddress = socket.getInetAddress().getHostAddress();
                    SwingUtilities.invokeLater(() -> chatWindow.appendMessage("System", "Peer đã kết nối đến: " + peerAddress));
                    
                    // Tạo và khởi động Handler mới
                    ChatHandler handler = new ChatHandler(socket, chatWindow, null);
                    activeHandlers.add(handler);
                    handler.start();
                    
                    SwingUtilities.invokeLater(() -> chatWindow.appendMessage("System", "Tổng kết nối: " + activeHandlers.size()));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatWindow.appendMessage("System", "Server lỗi: " + e.getMessage()));
            }
        }).start();
    }
    
    private void startClientConnection(String host) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(host, PORT);
                SwingUtilities.invokeLater(() -> chatWindow.appendMessage("System", "Đã kết nối Client tới peer: " + host));
                
                // Tạo và khởi động Handler mới
                ChatHandler handler = new ChatHandler(socket, chatWindow, this);
                activeHandlers.add(handler);
                handler.start();
                
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatWindow.appendMessage("System", "Không thể kết nối Client tới " + host + "!"));
            }
        }).start();
    }
    
    // Gắn hành động gửi tin nhắn / gửi file cho TẤT CẢ handlers
    private void attachActions() {
        chatWindow.addSendAction(ev -> {
            String msg = chatWindow.getInputText();
            if (!msg.isEmpty()) {
                // Hiển thị tin nhắn của mình một lần duy nhất
                chatWindow.appendMessage("Me", msg);
                
                // Gửi tin nhắn đến TẤT CẢ các peer
                sendMessageToAll(msg);
                chatWindow.clearInput();
            }
        });
        
        chatWindow.addSendFileAction(ev -> {
            if (activeHandlers.isEmpty()) {
                 chatWindow.appendMessage("System", "Không có kết nối nào để gửi file!");
                 return;
            }
            
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(chatWindow) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                chatWindow.appendMessage("System", "Đang gửi file: " + file.getName() + " đến tất cả peers...");
                sendFileToAll(file);
            }
        });
    }
    
    private void sendMessageToAll(String message) {
        // Dùng Iterator để có thể xóa phần tử an toàn trong khi duyệt
        synchronized (activeHandlers) {
            Iterator<ChatHandler> iterator = activeHandlers.iterator();
            while (iterator.hasNext()) {
                ChatHandler handler = iterator.next();
                if (!handler.sendMessage(message)) {
                    // Nếu gửi thất bại, xóa handler khỏi danh sách (vì nó đã đóng)
                    iterator.remove(); 
                }
            }
        }
    }
    
    private void sendFileToAll(File file) {
        // Gửi file đến TẤT CẢ các peer
        synchronized (activeHandlers) {
            for (ChatHandler handler : activeHandlers) {
                handler.sendFile(file);
            }
        }
    }

    // Triển khai interface ConnectionClosedListener để xóa handler bị đóng
    @Override
    public void onClosed(ChatHandler handler) {
        synchronized (activeHandlers) {
            activeHandlers.remove(handler);
            SwingUtilities.invokeLater(() -> chatWindow.appendMessage("System", "Tổng số kết nối còn lại: " + activeHandlers.size()));
        }
    }
}