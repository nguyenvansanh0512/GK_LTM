package chat;

import ui.ChatWindow;

import java.io.*;
import java.net.Socket;
import javax.swing.JFileChooser;

public class ChatHandler extends Thread { // Thay đổi thành extends Thread
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private ChatWindow chatWindow;
    private final String peerAddress;
    
    // Interface để thông báo cho ChatApp khi kết nối bị đóng
    public interface ConnectionClosedListener {
        void onClosed(ChatHandler handler);
    }
    
    private ConnectionClosedListener closedListener;

    public ChatHandler(Socket socket, ChatWindow chatWindow, ConnectionClosedListener listener) throws IOException {
        this.socket = socket;
        this.chatWindow = chatWindow;
        this.peerAddress = socket.getInetAddress().getHostAddress(); // Lưu địa chỉ
        this.closedListener = listener;

        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
    }
    
    // Ghi đè run() của Thread
    @Override
    public void run() {
        receiveLoop();
    }

    // Gửi tin nhắn text (KHÔNG hiển thị "Me" ở đây)
    public boolean sendMessage(String message) {
        try {
            dataOut.writeUTF("TEXT:" + message);
            dataOut.flush();
            return true;
        } catch (IOException e) {
            return false; // Báo hiệu gửi thất bại
        }
    }

    // Gửi file (Sửa đổi để nhận file object từ ChatApp)
    public void sendFile(File file) {
        try {
            // Gửi header: FILE + tên file + độ dài
            dataOut.writeUTF("FILE:" + file.getName());
            dataOut.writeLong(file.length());

            byte[] buffer = new byte[4096];
            FileInputStream fis = new FileInputStream(file);
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, read);
            }
            dataOut.flush();
            fis.close();
            // KHÔNG hiển thị thông báo gửi thành công ở đây để tránh trùng lặp
        } catch (IOException e) {
            chatWindow.appendMessage("System", "Gửi file thất bại tới " + peerAddress + "!");
        }
    }

    // Luồng nhận dữ liệu
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
            // Kết nối bị đóng
            chatWindow.appendMessage("System", "Kết nối tới " + peerAddress + " đã bị đóng!");
        } finally {
            try {
                socket.close();
            } catch (IOException e) { /* ignored */ }
            if (closedListener != null) {
                closedListener.onClosed(this); // Thông báo cho ChatApp
            }
        }
    }

    // Xử lý text
    private void handleText(String message) {
        chatWindow.appendMessage("Peer (" + peerAddress + ")", message);
    }

    // Xử lý file (Logic giữ nguyên)
    private void handleFile(String header) {
        try {
            String fileName = header.substring(5);
            long fileLength = dataIn.readLong();

            // Hỏi người dùng nơi lưu file
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
                chatWindow.appendMessage("System", "Đã nhận file: " + saveFile.getName() + " từ " + peerAddress);
            } else {
                // Nếu user cancel, đọc bỏ dữ liệu (logic giữ nguyên)
                byte[] buffer = new byte[4096];
                long remaining = fileLength;
                while (remaining > 0) {
                    int r = dataIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (r == -1)
                        break;
                    remaining -= r;
                }
                chatWindow.appendMessage("System", "Đã từ chối nhận file: " + fileName + " từ " + peerAddress);
            }
        } catch (IOException e) {
            chatWindow.appendMessage("System", "Nhận file thất bại từ " + peerAddress + "!");
        }
    }
}