package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ChatWindow extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
     private JButton sendFileButton;


    public ChatWindow() {
      

        setTitle("P2P Chat ");
         setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendFileButton = new JButton("Send File"); 

        JPanel bottom = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(sendFileButton);
        
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(buttonPanel, BorderLayout.EAST);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    public void addSendAction(ActionListener listener) {
        sendButton.addActionListener(listener);
        inputField.addActionListener(listener); // Enter = gửi
    }
    public void addSendFileAction(ActionListener listener) {
         sendFileButton.addActionListener(listener);
    }

    public String getInputText() {
        return inputField.getText().trim();
    }

    public void clearInput() {
        inputField.setText("");
    }

    public void appendMessage(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n");
    }

   
}
