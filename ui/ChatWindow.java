package ui;

import javax.swing.*;
import java.awt.*;

// ChatWindow giờ chỉ là khung chứa JTabbedPane
public class ChatWindow extends JFrame {
    private JTabbedPane tabbedPane;

    public ChatWindow() {
        setTitle("P2P Chat (Tab Mode)");
        setSize(800, 500); // Tăng kích thước
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Thêm một Tab ban đầu cho thông báo hệ thống
        JTextArea systemArea = new JTextArea("Chào mừng đến với ứng dụng Chat P2P Tab Mode!\nHãy kết nối với Peer đầu tiên.");
        systemArea.setEditable(false);
        tabbedPane.addTab("System Log", new JScrollPane(systemArea));
        
        setVisible(true);
    }
    
    // Phương thức để thêm một Tab Chat mới
    public void addChatPanel(String title, ChatPanel panel) {
        tabbedPane.addTab(title, panel);
        tabbedPane.setSelectedComponent(panel); // Chọn Tab vừa thêm
    }
    
    // Phương thức để xóa một Tab khi kết nối bị đóng
    public void removeChatPanel(ChatPanel panel) {
        tabbedPane.remove(panel);
    }

    // Phương thức hiển thị thông báo ra System Log (Tab đầu tiên)
    public void appendSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            Component systemLog = tabbedPane.getComponentAt(0);
            if (systemLog instanceof JScrollPane) {
                JViewport viewport = ((JScrollPane) systemLog).getViewport();
                if (viewport.getView() instanceof JTextArea) {
                    ((JTextArea) viewport.getView()).append(message + "\n");
                }
            }
        });
    }
    
    // Xóa các hàm cũ không dùng nữa (như addSendAction, getInputText, clearInput, appendMessage)
    // vì logic này đã chuyển vào ChatPanel.
}