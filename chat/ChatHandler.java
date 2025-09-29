package chat;

import ui.ChatPanel; // Import lớp ChatPanel mới
import ui.ChatWindow;

import java.io.*;
import java.net.Socket;
import javax.swing.JFileChooser;

public class ChatHandler extends Thread { 
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private ChatPanel chatPanel; // Tham chiếu đến Tab Chat riêng
    private ChatWindow chatWindow; // Tham chiếu đến cửa sổ chính để log hệ thống
    private final String peerAddress;

    // Interface để thông báo cho ChatApp khi kết nối bị đóng
    public interface ConnectionClosedListener {
        void onClosed(ChatHandler handler);
    }
    
    private ConnectionClosedListener closedListener;

    // Constructor mới nhận ChatPanel
    public ChatHandler(Socket socket, ChatWindow chatWindow, ChatPanel chatPanel, ConnectionClosedListener listener) throws IOException {
        this.socket = socket;
        this.chatWindow = chatWindow; // Dùng cho System Log
        this.chatPanel = chatPanel; // Dùng cho hiển thị tin nhắn
        this.peerAddress = socket.getInetAddress().getHostAddress();
        this.closedListener = listener;

        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
    }
    
    @Override
    public void run() {
        receiveLoop();
    }

    // Gửi tin nhắn text (chỉ gửi)
    public boolean sendMessage(String message) {
        try {
            dataOut.writeUTF("TEXT:" + message);
            dataOut.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Gửi file (Logic giữ nguyên)
    public void sendFile(File file) {
        try {
            dataOut.writeUTF("FILE:" + file.getName());
            dataOut.writeLong(file.length());

            byte[] buffer = new byte[4096];
            FileInputStream fis = new FileInputStream(file);
            int read;
            // ... (Phần gửi file giữ nguyên logic)
            while ((read = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, read);
            }
            dataOut.flush();
            fis.close();
            chatPanel.appendMessage("System", "Đã gửi file: " + file.getName()); // Hiển thị trong Tab riêng
        } catch (IOException e) {
            chatPanel.appendMessage("System", "Gửi file thất bại!");
        }
    }

    private void receiveLoop() {
        try {
            while (true) {
                String header = dataIn.readUTF();
                if (header.startsWith("TEXT:")) {
                    handleText(header.substring(5));
                } else if (header.startsWith("FILE:")) {
                    handleFile(header);
                }
            }
        } catch (IOException e) {
            // Log lỗi ra System Log của cửa sổ chính
            chatWindow.appendSystemMessage("Kết nối tới " + peerAddress + " đã bị đóng!"); 
        } finally {
            try {
                socket.close();
            } catch (IOException e) { /* ignored */ }
            if (closedListener != null) {
                closedListener.onClosed(this);
            }
        }
    }

    // Xử lý text (Hiển thị trong Tab liên kết)
    private void handleText(String message) {
        chatPanel.appendMessage(peerAddress, message);
    }

    private void handleFile(String header) {
        // ... (Logic nhận file giữ nguyên)
        try {
            String fileName = header.substring(5);
            long fileLength = dataIn.readLong();
            
            // ... (Phần còn lại của handleFile, chỉ thay đổi nơi appendMessage thành chatPanel)
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(fileName));
            if (chooser.showSaveDialog(chatWindow) == JFileChooser.APPROVE_OPTION) {
                File saveFile = chooser.getSelectedFile();
                FileOutputStream fos = new FileOutputStream(saveFile);

                byte[] buffer = new byte[4096];
                long remaining = fileLength;
                int read;
                while (remaining > 0
                        && (read = dataIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, read);
                    remaining -= read;
                }
                fos.close();
                chatPanel.appendMessage("System", "Đã nhận file: " + saveFile.getName());
            } else {
                // ... (Logic đọc bỏ dữ liệu)
                byte[] buffer = new byte[4096];
                long remaining = fileLength;
                while (remaining > 0) {
                    int r = dataIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (r == -1) break;
                    remaining -= r;
                }
                chatPanel.appendMessage("System", "Đã từ chối nhận file: " + fileName);
            }
        } catch (IOException e) {
            chatPanel.appendMessage("System", "Nhận file thất bại!");
        }
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }
    
    public String getPeerAddress() {
        return peerAddress;
    }
}