import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ChatFrame extends javax.swing.JFrame {

    private waitUserAction waitUser;
    private static final long serialVersionUID = 7L;
    public int operation;
    public String projectName;
    private InetSocketAddress address;
    private String username;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private Chats chats;
    private String onlineSince;
    private final StyledDocument chatDocument = new DefaultStyledDocument();
    private final SimpleAttributeSet thisusernameAttributeSet = new SimpleAttributeSet();
    private final SimpleAttributeSet messageAttributeSet = new SimpleAttributeSet();
    private final SimpleAttributeSet othersnameAttributeSet = new SimpleAttributeSet();
    private final SimpleAttributeSet systemcommunicationsAttributeSet = new SimpleAttributeSet();
    private final SimpleAttributeSet serverNotificationAttributeSet = new SimpleAttributeSet();
    public String userToAdd;
    private final StyledDocument statusDocument = new DefaultStyledDocument();
    private final ReentrantLock chatDocumentLock = new ReentrantLock();
    
    private void addMsgInDocument(String msg, SimpleAttributeSet attr) {
        try {
            chatDocumentLock.lock();
            try {
                this.chatDocument.insertString(this.chatDocument.getLength(), msg, attr);
            } finally {
                chatDocumentLock.unlock();
            }
        } catch (BadLocationException e) {
            JFrame exception = new JFrame("WORTH Program Error!");
            exception.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            String exceptionMessage = e.getMessage()+"\n\n";
            for(StackTraceElement el : e.getStackTrace()){
                exceptionMessage = exceptionMessage.concat(el.toString());
                exceptionMessage = exceptionMessage+"\n";
            }
            JTextArea area = new JTextArea(exceptionMessage);
            area.setEditable(false);
            JScrollPane pane = new JScrollPane(area);
            JLabel label1 = new JLabel("Program reported error:");
            label1.setIcon(new ImageIcon("icone"+File.separator+"exception.png"));
            label1.setHorizontalAlignment(0);
            JLabel label2 = new JLabel("Please close this window and restart the program.");
            label1.setFont(new java.awt.Font("Ubuntu", 1, 16));
            label2.setFont(new java.awt.Font("Ubuntu", 1, 16));
            exception.getContentPane().add(BorderLayout.NORTH,label1);
            exception.getContentPane().add(BorderLayout.CENTER,pane);
            exception.getContentPane().add(BorderLayout.SOUTH,label2);
            exception.pack();
            exception.setVisible(true);
            exception.setLocationRelativeTo(null);
            exception.toFront();
            exception.setAlwaysOnTop(true);
        }
    }
    
    public void addMsginChat(String msg) {      //metodo utilizzato dal thread di ricezione dei messaggi
        try {
            String m = msg.substring(0, msg.indexOf(":")+1);
            chatDocumentLock.lock();
            try {
                if(m.equals("[WORTH Server]:")) this.chatDocument.insertString(this.chatDocument.getLength(), m+" ", serverNotificationAttributeSet);
                else this.chatDocument.insertString(this.chatDocument.getLength(), m+" ", othersnameAttributeSet);
                this.chatDocument.insertString(this.chatDocument.getLength(), msg.substring(msg.indexOf(":")+1)+"\n\n", this.messageAttributeSet);
            } finally {
                chatDocumentLock.unlock();
            }
        } catch (BadLocationException e) {
            JFrame exception = new JFrame("WORTH Program Error!");
            exception.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            String exceptionMessage = e.getMessage()+"\n\n";
            for(StackTraceElement el : e.getStackTrace()){
                exceptionMessage = exceptionMessage.concat(el.toString());
                exceptionMessage = exceptionMessage+"\n";
            }
            JTextArea area = new JTextArea(exceptionMessage);
            area.setEditable(false);
            JScrollPane pane = new JScrollPane(area);
            JLabel label1 = new JLabel("Program reported error:");
            label1.setIcon(new ImageIcon("icone"+File.separator+"exception.png"));
            label1.setHorizontalAlignment(0);
            JLabel label2 = new JLabel("Please close this window and restart the program.");
            label1.setFont(new java.awt.Font("Ubuntu", 1, 16));
            label2.setFont(new java.awt.Font("Ubuntu", 1, 16));
            exception.getContentPane().add(BorderLayout.NORTH,label1);
            exception.getContentPane().add(BorderLayout.CENTER,pane);
            exception.getContentPane().add(BorderLayout.SOUTH,label2);
            exception.pack();
            exception.setVisible(true);
            exception.setLocationRelativeTo(null);
            exception.toFront();
            exception.setAlwaysOnTop(true);
        }
    }
    
    public void updateMemberStatus(Hashtable<String,String> status){
        try{
            this.statusDocument.remove(0, this.statusDocument.getLength());
            Style onlinestatusImgStyle = this.statusDocument.addStyle("onlineStyle", null);
            StyleConstants.setIcon(onlinestatusImgStyle, new ImageIcon("icone"+File.separator+"semaf_v.gif"));
            Style offlinestatusImgStyle = this.statusDocument.addStyle("offlineStyle", null);
            StyleConstants.setIcon(offlinestatusImgStyle, new ImageIcon("icone"+File.separator+"semaf_r.gif"));
            Set<Entry<String, String>> entrySet = status.entrySet();
            Iterator<Entry<String,String>> iter = entrySet.iterator();
            Entry<String,String> entry;
            while(iter.hasNext()){
                entry = iter.next();
                this.statusDocument.insertString(this.statusDocument.getLength(), entry.getKey(), othersnameAttributeSet);
                this.statusDocument.insertString(this.statusDocument.getLength(), " is now ", messageAttributeSet);
                this.statusDocument.insertString(this.statusDocument.getLength(), entry.getValue()+" ", othersnameAttributeSet);
                if(entry.getValue().equals("online")) this.statusDocument.insertString(this.statusDocument.getLength(), "not shown string\n", onlinestatusImgStyle);
                if(entry.getValue().equals("offline")) this.statusDocument.insertString(this.statusDocument.getLength(), "not shown string\n", offlinestatusImgStyle);
            }
        }
        catch (BadLocationException e){
            JFrame exception = new JFrame("WORTH Program Error!");
            exception.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            String exceptionMessage = e.getMessage()+"\n\n";
            for(StackTraceElement el : e.getStackTrace()){
                exceptionMessage = exceptionMessage.concat(el.toString());
                exceptionMessage = exceptionMessage+"\n";
            }
            JTextArea area = new JTextArea(exceptionMessage);
            area.setEditable(false);
            JScrollPane pane = new JScrollPane(area);
            JLabel label1 = new JLabel("Program reported error:");
            label1.setIcon(new ImageIcon("icone"+File.separator+"exception.png"));
            label1.setHorizontalAlignment(0);
            JLabel label2 = new JLabel("Please close this window and restart the program.");
            label1.setFont(new java.awt.Font("Ubuntu", 1, 16));
            label2.setFont(new java.awt.Font("Ubuntu", 1, 16));
            exception.getContentPane().add(BorderLayout.NORTH,label1);
            exception.getContentPane().add(BorderLayout.CENTER,pane);
            exception.getContentPane().add(BorderLayout.SOUTH,label2);
            exception.pack();
            exception.setVisible(true);
            exception.setLocationRelativeTo(null);
            exception.toFront();
            exception.setAlwaysOnTop(true);
        }
    }
    
    public ChatFrame(String project, InetSocketAddress address, String username, Chats chats, waitUserAction wait, String oS){
        this.projectName = project;
        this.address = address;
        this.username = username;
        this.chats = chats;
        this.waitUser = wait;
        this.onlineSince = oS;
        initComponents();
    }
    
    public ChatFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        this.jLabel1.setText("'"+this.projectName+"' project chat and members window.");
        jLabel2 = new javax.swing.JLabel();
        this.jLabel2.setIcon(new ImageIcon("icone"+File.separator+"chatLabel.png"));
        jScrollPane2 = new javax.swing.JScrollPane();
        chatTextPane = new javax.swing.JTextPane();
        this.chatTextPane.setDocument(this.chatDocument);
        this.thisusernameAttributeSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        this.thisusernameAttributeSet.addAttribute(StyleConstants.FontSize, 17);
        this.thisusernameAttributeSet.addAttribute(StyleConstants.Foreground, Color.RED);
        this.messageAttributeSet.addAttribute(StyleConstants.FontSize, 16);
        this.systemcommunicationsAttributeSet.addAttribute(StyleConstants.FontSize, 15);
        this.systemcommunicationsAttributeSet.addAttribute(StyleConstants.Italic, Boolean.TRUE);
        this.systemcommunicationsAttributeSet.addAttribute(StyleConstants.Foreground, Color.BLUE);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");     //formattatore di data e ora
        this.othersnameAttributeSet.addAttribute(StyleConstants.FontSize, 17);
        this.othersnameAttributeSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        this.serverNotificationAttributeSet.addAttribute(StyleConstants.Foreground, Color.GRAY);
        this.serverNotificationAttributeSet.addAttribute(StyleConstants.FontSize, 17);
        this.serverNotificationAttributeSet.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        ArrayList<String> oldMessages = this.chats.getChatMessages(this.projectName);
        if(oldMessages.size() == 1 && oldMessages.get(0).equals("[WORTH SYSTEM]: old messages lost while trying to delete this project")) {
            this.addMsgInDocument(oldMessages.get(0)+"\n\n", this.systemcommunicationsAttributeSet);
            this.chats.removeDeletedMsgsSystemMessage(this.projectName);
        }
        else{
            if(!oldMessages.isEmpty()) this.addMsgInDocument("[WORTH SYSTEM]: old messages (will persist until you terminate the program):"+"\n\n", this.systemcommunicationsAttributeSet);
            for(String m : oldMessages){
                this.addMsgInDocument(m.substring(0, m.indexOf(":")+1)+" ", this.othersnameAttributeSet);
                this.addMsgInDocument(m.substring(m.indexOf(":")+1)+"\n\n", this.messageAttributeSet);
            }
        }
        this.addMsgInDocument("[WORTH SYSTEM]: chat opened at: "+dtf.format(LocalDateTime.now())+"\n\n", this.systemcommunicationsAttributeSet);
        jScrollPane1 = new javax.swing.JScrollPane();
        typeMsgTextArea = new javax.swing.JTextArea();
        sendButton = new javax.swing.JButton();
        this.sendButton.setIcon(new ImageIcon("icone"+File.separator+"send.png"));
        backButton = new javax.swing.JButton();
        this.backButton.setIcon(new ImageIcon("icone"+File.separator+"backToMenu.png"));
        jLabel3 = new javax.swing.JLabel();
        this.jLabel3.setIcon(new ImageIcon("icone"+File.separator+"members.png"));
        jLabel4 = new javax.swing.JLabel();
        newMemberNameTextField = new javax.swing.JTextField();
        addMemberButton = new javax.swing.JButton();
        this.addMemberButton.setIcon(new ImageIcon("icone"+File.separator+"newMember.png"));
        jLabel5 = new javax.swing.JLabel();
        this.jLabel5.setIcon(new ImageIcon("icone"+File.separator+"connected.gif"));
        jScrollPane3 = new javax.swing.JScrollPane();
        membersStatusTextPane = new javax.swing.JTextPane();
        this.membersStatusTextPane.setDocument(this.statusDocument);
        jLabel6 = new javax.swing.JLabel();
        leaveProjectButton = new javax.swing.JButton();
        this.leaveProjectButton.setIcon(new ImageIcon("icone"+File.separator+"leavePr.png"));
        jLabel7 = new javax.swing.JLabel();
        this.jLabel7.setText(" "+this.username+" you are online since "+this.onlineSince);
        this.jLabel7.setIcon(new ImageIcon("icone"+File.separator+"you.png"));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("WORTH project Chat");

        jLabel1.setFont(new java.awt.Font("Ubuntu", 3, 20)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Ubuntu", 1, 17)); // NOI18N
        jLabel2.setText(" Project Chat");

        chatTextPane.setEditable(false);
        chatTextPane.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        chatTextPane.setToolTipText("project chat");
        jScrollPane2.setViewportView(chatTextPane);

        typeMsgTextArea.setBackground(new java.awt.Color(255, 255, 204));
        typeMsgTextArea.setColumns(20);
        typeMsgTextArea.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        typeMsgTextArea.setRows(5);
        typeMsgTextArea.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, new java.awt.Color(255, 204, 51), null, new java.awt.Color(255, 204, 51)), "Type a new message...", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Ubuntu", 1, 15), new java.awt.Color(255, 102, 102))); // NOI18N
        typeMsgTextArea.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        typeMsgTextArea.setSelectionColor(new java.awt.Color(255, 153, 102));
        jScrollPane1.setViewportView(typeMsgTextArea);

        sendButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        sendButton.setText(" Send Message");
        sendButton.setToolTipText("send this message");
        sendButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        backButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        backButton.setText("   Back To Project Menu");
        backButton.setToolTipText("press to go back to project menu");
        backButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Ubuntu", 1, 17)); // NOI18N
        jLabel3.setText(" Member Area");

        jLabel4.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabel4.setText("2. Add a member to this project:");

        newMemberNameTextField.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        newMemberNameTextField.setToolTipText("type the new member name");

        addMemberButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        addMemberButton.setText(" Add Member");
        addMemberButton.setToolTipText("press to add the new member");
        addMemberButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addMemberButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMemberButtonActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabel5.setText("1. LIVE members status in the system:");

        membersStatusTextPane.setEditable(false);
        membersStatusTextPane.setBorder(javax.swing.BorderFactory.createEtchedBorder(null, new java.awt.Color(153, 204, 255)));
        membersStatusTextPane.setToolTipText("live members situation in the system");
        jScrollPane3.setViewportView(membersStatusTextPane);

        jLabel6.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabel6.setText("3. Leave this project:");

        leaveProjectButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        leaveProjectButton.setText(" Leave this Project");
        leaveProjectButton.setToolTipText("press to leave this project");
        leaveProjectButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        leaveProjectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leaveProjectButtonActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Ubuntu", 2, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(0, 0, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 615, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(28, 28, 28)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel5)
                                            .addComponent(jLabel4)
                                            .addComponent(jLabel3)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(49, 49, 49)
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(30, 30, 30)
                                        .addComponent(jLabel6))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addComponent(newMemberNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(addMemberButton))))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(52, 52, 52)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 322, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(backButton))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(sendButton)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(leaveProjectButton)
                                                .addGap(99, 99, 99))))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(120, 120, 120)
                                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(38, 38, 38)
                                .addComponent(jLabel2))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 965, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(13, 13, 13))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 384, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, Short.MAX_VALUE)
                                .addComponent(backButton))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(41, 41, 41)
                                .addComponent(sendButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(newMemberNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addMemberButton))
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addComponent(leaveProjectButton)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        this.operation = 0;
        this.dispose();
        this.waitUser.setUserDone();
    }//GEN-LAST:event_backButtonActionPerformed

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
        String message = this.typeMsgTextArea.getText();
        this.typeMsgTextArea.selectAll();
        this.typeMsgTextArea.cut();
        if(message.isBlank() || message.length()>150){
            JFrame mg = new JFrame("Message not acceptable!");
            mg.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            JLabel label = new JLabel("Please type a non-blank message, containing not more than 150 characters.");
            label.setFont(new java.awt.Font("Ubuntu", 1, 16));
            JLabel fakelabel1 = new JLabel(new ImageIcon("icone"+File.separator+"msgError.png"));
            JLabel fakelabel2 = new JLabel(" ");
            mg.getContentPane().add(BorderLayout.NORTH,fakelabel1);
            mg.getContentPane().add(BorderLayout.CENTER,label);
            mg.getContentPane().add(BorderLayout.SOUTH,fakelabel2);
            mg.pack();
            mg.setLocationRelativeTo(null);
            mg.setVisible(true);
            mg.toFront();
            mg.setAlwaysOnTop(true);
        }
        else{
            message = "["+this.username+"]:"+message;
            byte [] arr = message.getBytes();
            this.packet = new DatagramPacket(arr, arr.length, this.address.getAddress(), this.address.getPort());
            try {
                socket = new DatagramSocket();
                socket.send(packet);
                chatDocumentLock.lock();
                try {
                    this.chatDocument.insertString(this.chatDocument.getLength(), message.substring(0, message.indexOf(":")+1)+" ",this.thisusernameAttributeSet);
                    this.chatDocument.insertString(this.chatDocument.getLength(), message.substring(message.indexOf(":")+1)+"\n\n", this.messageAttributeSet);
                } finally {
                    chatDocumentLock.unlock();
                }
                socket.close();
            } catch (IOException | BadLocationException e) {
                JFrame exception = new JFrame("WORTH Program Error!");
                exception.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                String exceptionMessage = e.getMessage()+"\n\n";
                for(StackTraceElement el : e.getStackTrace()){
                    exceptionMessage = exceptionMessage.concat(el.toString());
                    exceptionMessage = exceptionMessage+"\n";
                }
                JTextArea area = new JTextArea(exceptionMessage);
                area.setEditable(false);
                JScrollPane pane = new JScrollPane(area);
                JLabel label1 = new JLabel("Program reported error:");
                label1.setIcon(new ImageIcon("icone"+File.separator+"exception.png"));
                label1.setHorizontalAlignment(0);
                JLabel label2 = new JLabel("Please close this window and restart the program.");
                label1.setFont(new java.awt.Font("Ubuntu", 1, 16));
                label2.setFont(new java.awt.Font("Ubuntu", 1, 16));
                exception.getContentPane().add(BorderLayout.NORTH,label1);
                exception.getContentPane().add(BorderLayout.CENTER,pane);
                exception.getContentPane().add(BorderLayout.SOUTH,label2);
                exception.pack();
                exception.setVisible(true);
                exception.setLocationRelativeTo(null);
                exception.toFront();
                exception.setAlwaysOnTop(true);
            }
        }
    }//GEN-LAST:event_sendButtonActionPerformed

    private void addMemberButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addMemberButtonActionPerformed
        this.operation = 1;
        this.userToAdd = this.newMemberNameTextField.getText();
        this.waitUser.setUserDone();
    }//GEN-LAST:event_addMemberButtonActionPerformed

    private void leaveProjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leaveProjectButtonActionPerformed
        this.operation = 2;
        this.waitUser.setUserDone();
        this.dispose();
    }//GEN-LAST:event_leaveProjectButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ChatFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ChatFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ChatFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ChatFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ChatFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addMemberButton;
    private javax.swing.JButton backButton;
    private javax.swing.JTextPane chatTextPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton leaveProjectButton;
    private javax.swing.JTextPane membersStatusTextPane;
    private javax.swing.JTextField newMemberNameTextField;
    private javax.swing.JButton sendButton;
    private javax.swing.JTextArea typeMsgTextArea;
    // End of variables declaration//GEN-END:variables
}
