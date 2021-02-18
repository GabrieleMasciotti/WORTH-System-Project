import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ChatMessageReceiver implements Runnable{
    private final Chats chats;
    private final String username;
    public Selector selector;
    private ChatFrame chatWindow = null;
    public boolean showNotifications = true;
    private final InetAddress myInet;
    
    public void setChatWindow(ChatFrame cw){
        this.chatWindow = cw;
    }
    
    public void resetChatWindow(){
        this.chatWindow = null;
    }
    
    public ChatMessageReceiver(Chats c, String name, InetAddress inetAddress){
        this.chats = c;
        this.username = name;
        this.myInet = inetAddress;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            
            ArrayList<String> groupsToAdd;
            ArrayList<String> groupsToRemove;
            DatagramChannel channel;
            String projectName;
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(myInet);
            byte [] arr;
            while(true){
                groupsToAdd = this.chats.getChatAdded();
                if(!groupsToAdd.isEmpty()){
                    for(String pr : groupsToAdd){
                        InetSocketAddress i = chats.getPrAddress(pr);
                        DatagramChannel ch = DatagramChannel.open(StandardProtocolFamily.INET);
                        ch.setOption(StandardSocketOptions.SO_REUSEADDR,true);
                        ch.bind(new InetSocketAddress(i.getPort()));
                        ch.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
                        ch.configureBlocking(false);
                        ch.join(i.getAddress(), networkInterface);
                        ByteBuffer att = ByteBuffer.allocateDirect(1024);
                        att.clear();
                        att.put(pr.getBytes());
                        att.flip();
                        ch.register(selector, SelectionKey.OP_READ, att);
                    }
                    this.chats.resetChatAdded(groupsToAdd);
                }
                groupsToRemove = this.chats.getChatRemoved();
                if(!groupsToRemove.isEmpty()){
                    Iterator<SelectionKey> keys = selector.keys().iterator();
                    while(keys.hasNext()){
                        SelectionKey k = keys.next();
                        DatagramChannel c = (DatagramChannel) k.channel();
                        ByteBuffer attac = (ByteBuffer) k.attachment();
                        arr = new byte[attac.limit()];
                        attac.get(arr);
                        attac.rewind();
                        projectName = new String(arr);
                        if(groupsToRemove.contains(projectName)){
                            c.close();
                            k.cancel();
                        }
                    }
                    this.chats.resetChadRemoved(groupsToRemove);
                }
                
                selector.select();
                
                groupsToAdd = this.chats.getChatAdded();        //dopo la select ripete i controlli
                if(!groupsToAdd.isEmpty()){
                    for(String pr : groupsToAdd){
                        InetSocketAddress i = chats.getPrAddress(pr);
                        DatagramChannel ch = DatagramChannel.open(StandardProtocolFamily.INET);
                        ch.setOption(StandardSocketOptions.SO_REUSEADDR,true);
                        ch.bind(new InetSocketAddress(i.getPort()));
                        ch.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
                        ch.configureBlocking(false);
                        ch.join(i.getAddress(), networkInterface);
                        ByteBuffer att = ByteBuffer.allocateDirect(1024);
                        att.clear();
                        att.put(pr.getBytes());
                        att.flip();
                        ch.register(selector, SelectionKey.OP_READ, att);
                    }
                    this.chats.resetChatAdded(groupsToAdd);
                }
                groupsToRemove = this.chats.getChatRemoved();
                if(!groupsToRemove.isEmpty()){
                    Iterator<SelectionKey> keys = selector.keys().iterator();
                    while(keys.hasNext()){
                        SelectionKey k = keys.next();
                        DatagramChannel c = (DatagramChannel) k.channel();
                        ByteBuffer attac = (ByteBuffer) k.attachment();
                        arr = new byte[attac.limit()];
                        attac.get(arr);
                        attac.rewind();
                        projectName = new String(arr);
                        if(groupsToRemove.contains(projectName)){
                            c.close();
                            k.cancel();
                        }
                    }
                    this.chats.resetChadRemoved(groupsToRemove);
                }
                
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while(selectedKeys.hasNext()){
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if(!key.isValid()) {continue;}
                    if(key.isReadable()){
                        ByteBuffer buf = ByteBuffer.allocateDirect(500);
                        channel = (DatagramChannel) key.channel();
                        buf.clear();
                        channel.receive(buf);
                        buf.flip();
                        arr = new byte[buf.limit()];
                        buf.get(arr);
                        String msg = new String(arr);
                        ByteBuffer att;
                        att = (ByteBuffer) key.attachment();
                        arr = new byte[att.limit()];
                        att.get(arr);
                        att.rewind();
                        projectName = new String(arr);
                        this.chats.putNewMsg(projectName, msg);
                        String sender = msg.substring(msg.indexOf("[")+1, msg.indexOf(":")-1);
                        boolean msgShownInChat = false;
                        if(!sender.equals(this.username)){       //se non è l'utente di questo client ad avere inviato il messaggio
                            if(this.chatWindow != null) {
                                if(this.chatWindow.projectName.equals(projectName)) {
                                    this.chatWindow.addMsginChat(msg); //l'utente che sta utilizzando questo client ha la finestra della chat aperta (mostra subito il messaggio nella chat)
                                    msgShownInChat = true;
                                }
                            }
                            if(!msgShownInChat && showNotifications){       //l'utente non è nella finestra della chat e quindi invia una notifica di messaggio ricevuto (se le notifiche non sono state disattivate dall'utente)
                                JFrame mg = new JFrame("New WORTH message!");
                                mg.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                                JLabel label = new JLabel(this.username+", you received a new WORTH message by "+sender+" in '"+projectName+"' project chat!");
                                label.setFont(new java.awt.Font("Ubuntu", 1, 16));
                                JLabel fakelabel1 = new JLabel(new ImageIcon("icone"+File.separator+"newMsgNot.png"));
                                JLabel fakelabel2 = new JLabel(" ");
                                mg.getContentPane().add(BorderLayout.NORTH,fakelabel1);
                                mg.getContentPane().add(BorderLayout.CENTER,label);
                                mg.getContentPane().add(BorderLayout.SOUTH,fakelabel2);
                                mg.pack();
                                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                                int width = mg.getSize().width;
                                mg.setLocation((dim.width-width)/2, 0);
                                mg.setVisible(true);
                                mg.toFront();
                            }
                        }
                        
                        
                    }
                }
                
                
            }
            
            
        } catch (Exception e) {
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
    
    
}
