package ui; 

import ui.ChatWindow;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import chat.ChatHandler;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// Triển khai cả 2 interface để quản lý Tab và xử lý gửi tin nhắn
public class ChatApp implements ChatHandler.ConnectionClosedListener, ChatPanel.SendActionListener {
    public static final int PORT = 12345;
    // Danh sách để quản lý tất cả các ChatHandler đang hoạt động
    private final List<ChatHandler> activeHandlers = Collections.synchronizedList(new LinkedList<>());
    private ChatWindow chatWindow;
    // Map để ánh xạ ChatPanel (Tab UI) với ChatHandler (Luồng mạng)
    private final Map<ChatPanel, ChatHandler> panelToHandlerMap = Collections.synchronizedMap(new HashMap<>());


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
    }
    
    private void startServerListener() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                chatWindow.appendSystemMessage("Server đang lắng nghe trên cổng " + PORT + "...");
                while (true) {
                    Socket socket = serverSocket.accept();
                    String peerAddress = socket.getInetAddress().getHostAddress();
                    
                    // Tạo Tab và Handler mới cho kết nối đến
                    setupNewConnection(socket, peerAddress);
                }
            } catch (IOException e) {
                chatWindow.appendSystemMessage("Server lỗi: " + e.getMessage());
            }
        }).start();
    }
    
    private void startClientConnection(String host) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(host, PORT);
                
                // Tạo Tab và Handler mới cho kết nối đi
                setupNewConnection(socket, host);
                
            } catch (IOException e) {
                chatWindow.appendSystemMessage("Không thể kết nối Client tới " + host + "! Lỗi: " + e.getMessage());
            }
        }).start();
    }
    
    // Phương thức chung để thiết lập Handler và Panel
    private void setupNewConnection(Socket socket, String peerAddress) throws IOException {
        String peerName = peerAddress; // Sử dụng địa chỉ IP làm tên Tab
        
        // Tạo ChatPanel (Tab UI)
        ChatPanel chatPanel = new ChatPanel(peerName, this); // Gán 'this' làm SendActionListener
        
        // Tạo ChatHandler (Luồng mạng)
        ChatHandler handler = new ChatHandler(socket, chatWindow, chatPanel, this);
        
        // Thêm vào danh sách và Map
        activeHandlers.add(handler);
        panelToHandlerMap.put(chatPanel, handler);
        
        // Cập nhật UI và khởi động Thread
        SwingUtilities.invokeLater(() -> {
            chatWindow.addChatPanel(peerName, chatPanel);
            chatWindow.appendSystemMessage("Đã tạo kết nối mới với: " + peerName + ". Tổng kết nối: " + activeHandlers.size());
        });
        
        handler.start();
    }

    // Xử lý sự kiện GỬI từ một ChatPanel (ChatPanel.SendActionListener)
    @Override
    public void onSend(String message, ChatPanel sourcePanel) {
        // 1. Tìm Handler liên kết với Tab đang gửi
        ChatHandler handler = panelToHandlerMap.get(sourcePanel);
        
        if (handler != null) {
            // 2. Gửi tin nhắn qua Handler đó (chỉ gửi riêng 1 peer)
            if (handler.sendMessage(message)) {
                // 3. Hiển thị tin nhắn của mình trong Tab đó
                sourcePanel.appendMessage("Me", message);
            } else {
                sourcePanel.appendMessage("System", "Gửi thất bại. Kết nối đã đóng.");
            }
        } else {
            sourcePanel.appendMessage("System", "Không tìm thấy kết nối!");
        }
    }
    
    @Override
    public void onSendFile(File file, ChatPanel sourcePanel) {
        ChatHandler handler = panelToHandlerMap.get(sourcePanel);
        if (handler != null) {
            handler.sendFile(file);
        } else {
            sourcePanel.appendMessage("System", "Không tìm thấy kết nối!");
        }
    }

    // Triển khai ConnectionClosedListener để xóa Handler và Tab bị đóng
    @Override
    public void onClosed(ChatHandler handler) {
        // Xóa khỏi danh sách Handler
        synchronized (activeHandlers) {
            activeHandlers.remove(handler);
        }
        
        // Xóa ChatPanel khỏi UI và Map
        ChatPanel panelToRemove = handler.getChatPanel();
        if (panelToRemove != null) {
            SwingUtilities.invokeLater(() -> {
                chatWindow.removeChatPanel(panelToRemove);
            });
            panelToHandlerMap.remove(panelToRemove);
        }
        
        chatWindow.appendSystemMessage("Kết nối " + handler.getPeerAddress() + " đã bị đóng. Tổng số kết nối còn lại: " + activeHandlers.size());
    }
}