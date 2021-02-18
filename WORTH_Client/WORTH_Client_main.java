import java.awt.BorderLayout;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.JFrame;
import javax.swing.JLabel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class WORTH_Client_main {

    public void newMsgFrame(String title, String msg, String iconName){
        JFrame mg = new JFrame(title);
        mg.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        JLabel label = new JLabel(msg);
        label.setFont(new java.awt.Font("Ubuntu", 1, 16));
        JLabel fakelabel1 = new JLabel(new ImageIcon("icone"+File.separator+iconName));
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
    
    public void newScrollFrame(String title, String content){
        JFrame scroll = new JFrame(title);
        scroll.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        JScrollPane pane = new JScrollPane(area);
        scroll.add(pane);
        scroll.pack();
        scroll.setLocationRelativeTo(null);
        scroll.setVisible(true);
        scroll.toFront();
        scroll.setAlwaysOnTop(true);
    }
    
    public static void main(String[] args) throws RemoteException, NotBoundException, IOException, InterruptedException {
        
        try{
        WORTH_Client_main c = new WORTH_Client_main();
        Registration_Interface registration;
        Remote remoteStub;
        Registry reg;
        String serverIP;
        int serverPort;
        String myIP;
        InetAddress myInet = null;
        waitUserAction waitUser = new waitUserAction();
        String onlineSince;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");     //formattatore di data e ora
        boolean showNotification = true;
        
        String remoteornot;
        BeforeWeStart atStartFrame = new BeforeWeStart(waitUser);   //finestra in cui l'utente sceglie a quale server connettersi
        atStartFrame.setLocationRelativeTo(null);
        atStartFrame.setVisible(true);
        waitUser.waitUser();        //attende la scelta dell'utente
        if(atStartFrame.remote){        //l'utente ha scelto di collegarsi ad un server remoto
            remoteornot = "remote";
            serverIP = atStartFrame.ServerIp;
            serverPort = Integer.parseUnsignedInt(atStartFrame.serverPort);
            reg = LocateRegistry.getRegistry(serverIP,Integer.parseUnsignedInt(atStartFrame.RMIPort));
        }
        else{               //l'utente si collega al server locale con le impostazioni predefinite
            remoteornot = "local";
            reg = LocateRegistry.getRegistry(6989);
            serverIP = InetAddress.getLocalHost().getHostAddress();
            serverPort = 10000;
        }
        
        JFrame connecting = new JFrame("Connecting to WORTH server");
        connecting.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connecting.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        JLabel lab = new JLabel("Connecting to "+remoteornot+" WORTH server. Please wait...");
        JLabel fake1 = new JLabel(" ");
        JLabel fake2 = new JLabel(new ImageIcon("icone"+File.separator+"connecting.gif"));
        lab.setFont(new java.awt.Font("Ubuntu", 1, 18));
        connecting.getContentPane().add(BorderLayout.NORTH,fake1);
        connecting.getContentPane().add(BorderLayout.CENTER,lab);
        connecting.getContentPane().add(BorderLayout.SOUTH,fake2);
        connecting.pack();
        connecting.setLocationRelativeTo(null);
        connecting.setVisible(true);
        connecting.toFront();
        
        remoteStub = reg.lookup("WORTH_Registration_Service");
        registration = (Registration_Interface) remoteStub;         //stub del servizio remoto di registrazione
        
        UserUpdates_ROC callBackReceiver = new UserUpdates_ROC();
        Client_UserUpdates_Interface callBackStub;
        
        Access_Frame accessFrame;
        int inAccessFrame = 999;
        String name = new String();
        SocketChannel clientChannel = null;                            //dichiarazione channel di connessione TCP con il server WORTH
        ByteBuffer buf = ByteBuffer.allocateDirect(4096);
        String outToServer;
        int inFromServer;
        int bytesSent;
        String semiColon = ";";
        
        connecting.dispose();       //la connessione è avvenuta (il client ha individuato lo stub RMI del server)
        
        ObjectMapper obj = new ObjectMapper();
        Chats chats = new Chats();      //struttura che mantiene le chat dei progetti di cui l'utente è membro (condivisa con il thread di ricezione dei messaggi)
        
        /* visualizza la finestra di accesso */
        
        while(inAccessFrame != 0){                      //segnalazione e gestione errori dell'utente
            accessFrame = new Access_Frame(waitUser);     //visualizzazione finestra di accesso
            accessFrame.setLocationRelativeTo(null);
            accessFrame.setVisible(true);
            waitUser.waitUser();
            if(accessFrame.operation == 0) {      //se l'utente ha richiesto di effettuare una registrazione
                if(accessFrame.username.contains(semiColon) || accessFrame.psw.contains(semiColon)) {
                    inAccessFrame = 1;         //il nome utente e/o la password scelti contengono il ;
                    c.newMsgFrame("Entered credentials not acceptable by the system!", "Please do not use ';' (semicolon) in either username or password.","badcred.png");
                }
                else {
                    if(accessFrame.username.length() > 20 || accessFrame.psw.length() > 50){
                        inAccessFrame = 1;         //credenziali troppo lunghe
                        c.newMsgFrame("Too long credentials!", "ERROR! Too long username (max 20 chars) and password (max 50 chars). Registration failed.","badcred.png");
                    }
                    else{
                        inFromServer = registration.register(accessFrame.username, accessFrame.psw);       //se le credenziali sono corrette nella sintassi
                        name = accessFrame.username;
                        if(inFromServer==1){                           //se il nome utente è già in uso da qualcun'altro
                            c.newMsgFrame("UserName used!", "The specified USERNAME is ALREADY IN USE. Please, close this message and try again with another.","badcred.png");
                            inAccessFrame = 1;
                        }
                        else{
                            if(inFromServer==2){                           //se il nome utente è vuoto
                                c.newMsgFrame("UserName empty!", "The specified USERNAME is EMPTY. Choose a non-empty one so that we can recognise you properly.","badcred.png");
                                inAccessFrame = 1;
                            }
                            else{
                                if(inFromServer==3){                       //se la password non è stata inserita (campo vuoto)
                                    c.newMsgFrame("Empty password!", "ERROR, password field is mandatory. Please choose a password and fill in the box to complete the registration process.","badcred.png");
                                    inAccessFrame = 1;
                                }
                                else {
                                    inAccessFrame = 0;     //REGISTRAZIONE ANDATA A BUON FINE, il programma esegue il login automatico e poi mostra la home page
                                    clientChannel = SocketChannel.open(new InetSocketAddress(serverIP,serverPort));       //si connette al server per effettuare il login automatico post registrazione
                                    myIP = clientChannel.socket().getLocalAddress().getHostAddress();
                                    myInet = clientChannel.socket().getLocalAddress();
                                    //setta la proprietà di sistema con l'indirizzo ip della socket di connessione tcp col server, questo consente la corretta esportazione dell'oggetto remoto con cui il client si iscrive per ricevere notifiche di callback dal server (evita che alcuni dispositivi possano esportare con il loro loopback address)
                                    System.setProperty("java.rmi.server.hostname", myIP);
                                    callBackStub = (Client_UserUpdates_Interface) UnicastRemoteObject.exportObject(callBackReceiver, 0);
                                    buf.clear();
                                    buf.putInt(3);          //il client invia, sulla connessione TCP, al server il codice 3 (per il login automatico)
                                    buf.put(name.getBytes());    //inserisce nel buffer di invio il nome utente
                                    buf.flip();
                                    clientChannel.write(buf);               //invio dei dati al server per il login automatico
                                    callBackReceiver.setUsers(registration.registerForNotification(name, callBackStub));      //il client si registra per ricevere le callback dopo il login automatico
                                }
                            }
                        }
                    }
                }
            }
            else {      //se l'utente ha richiesto di effettuare il login
                name = accessFrame.username;
                clientChannel = SocketChannel.open(new InetSocketAddress(serverIP,serverPort));       //si connette al server per effettuare il login
                outToServer = name+semiColon+accessFrame.psw;
                bytesSent = outToServer.getBytes().length;
                buf.clear();
                buf.putInt(0);          //il client invia, sulla connessione TCP, al server il codice 0 (per il login)
                buf.putInt(bytesSent);              //indica al server il numero di bytes cha ha inserito nel buffer per le credenziali
                buf.put(outToServer.getBytes());
                buf.flip();
                clientChannel.write(buf);               //invio delle credenziali al server per il login
                buf.clear();
                clientChannel.read(buf);                //ricezione della risposta del server al tentato login
                buf.flip();
                inFromServer = buf.getInt();
                if(inFromServer == 1){
                    inAccessFrame = 1;
                    c.newMsgFrame("Unknown user!", "Error. The inserted username was not recognized! Please register to WORTH service before accessing.","unknown.png");
                    clientChannel.close();      //chiusura connessione a seguito dell'errore commesso dall'utente
                }
                else{
                    if(inFromServer == 2){
                        inAccessFrame = 2;
                        c.newMsgFrame("Incorrect password!", "LOGIN TO WORTH FAILED. The inserted password is not associated to this username.","wrongpsw.png");
                        clientChannel.close();      //chiusura connessione a seguito dell'errore commesso dall'utente
                    }
                    else{
                        if(inFromServer == 3){
                            inAccessFrame = 3;
                            c.newMsgFrame("Account already connected!", "Your WORTH account is already open in another window (or in another device). Please disconnect it there to enter here.","switchdevice.png");
                            clientChannel.close();      //chiusura connessione a seguito dell'errore commesso dall'utente
                        }
                        else{
                            inAccessFrame = 0;      //il LOGIN È ANDATO A BUON FINE, il programma riceve dal server i riferimenti per le chat dei progetti e poi mostra la home page
                            myIP = clientChannel.socket().getLocalAddress().getHostAddress();
                            myInet = clientChannel.socket().getLocalAddress();
                            //setta la proprietà di sistema con l'indirizzo ip della socket di connessione tcp col server, questo consente la corretta esportazione dell'oggetto remoto con cui il client si iscrive per ricevere notifiche di callback dal server (evita che alcuni dispositivi possano esportare con il loro loopback address)
                            //System.out.println("My ip: "+myIP);
                            //System.out.println("My inet: "+myInet);
                            System.setProperty("java.rmi.server.hostname", myIP);
                            callBackStub = (Client_UserUpdates_Interface) UnicastRemoteObject.exportObject(callBackReceiver, 0);
                            //System.out.println(callBackStub); //stampa il riferimento all'oggetto remoto
                            callBackReceiver.setUsers(registration.registerForNotification(name,callBackStub));     //si registra per ricevere le callback del server
                            buf.clear();
                            buf.putInt(1);  //operazione 1: RICHIESTA RIFERIMENTI ALLE CHAT DEI PROGETTI al server
                            buf.flip();
                            clientChannel.write(buf);
                            buf.clear();
                            clientChannel.read(buf);        //il client riceve la lunghezza della lista di indirizzi delle chat dei progetti di cui l'utente è membro
                            buf.flip();
                            ByteBuffer buff = ByteBuffer.allocateDirect(buf.getInt());      //alloca un buffer di ricezione delle giuste dimensioni (della lunghezza della lista che sta per ricevere)
                            buf.clear();
                            buf.putInt(16);     //operazione 16: il client comunica al server di essere pronto a ricevere la lista degli indirizzi delle chat
                            buf.flip();
                            clientChannel.write(buf);
                            buff.clear();
                            clientChannel.read(buff);       //il client riceve la lista degli InetSocketAddress
                            buff.flip();
                            byte [] arr = new byte[buff.limit()];
                            buff.get(arr);
                            Hashtable<String,InetSocketAddress> ips = obj.readValue(arr, new TypeReference<Hashtable<String,InetSocketAddress>>(){});
                            Iterator<Entry<String,InetSocketAddress>> iterator = ips.entrySet().iterator();
                            Entry<String,InetSocketAddress> en;
                            while(iterator.hasNext()){
                                en = iterator.next();
                                chats.addChat(en.getKey(), en.getValue());
                            }
                        }
                    }
                }
            }
        }
        
        /* chiusura della finestra di accesso */
        onlineSince = dtf.format(LocalDateTime.now());
        
        Home_Page_Frame home;
        int inHome = 999;
        boolean openMenu = false;
        String prjName = new String();
        boolean reOpenHome = false;
        
        ChatMessageReceiver mr = new ChatMessageReceiver(chats,name,myInet);
        Thread messageReceiver = new Thread(mr);
        messageReceiver.start();        //thread di ricezione dei messaggi relativi ai progetti di cui l'utente è membro attivato
        
        /* mostra la home page del sistema */
        home = new Home_Page_Frame(name,waitUser);
        home.setLocationRelativeTo(null);
        home.setVisible(true);
        while(inHome != 0){
            if(reOpenHome){
                home = new Home_Page_Frame(name,waitUser);
                home.setLocationRelativeTo(null);
                home.setVisible(true);
            }
            waitUser.waitUser();
            if(home.operation == 2){    //VISUALIZZAZIONE MENU DI UN PROGETTO
                prjName = home.showProjectName;
                buf.clear();
                buf.putInt(5);      //codice 5: visualizzazione menu di un progetto esistente
                buf.put(prjName.getBytes());
                buf.flip();
                clientChannel.write(buf);
                buf.clear();
                clientChannel.read(buf);
                buf.flip();
                int res = buf.getInt();     //risposta del server
                if(res == 1){
                    inHome = 1;     //restiamo nella home page
                    reOpenHome = false;
                    c.newMsgFrame("Non-existent project!", "The project you want to open doesn't exist in the WORTH system. Try again.","projecterror.png");
                }
                if(res == 2){
                    inHome = 2;     //restiamo nella home page
                    reOpenHome = false;
                    c.newMsgFrame("Cannot access the project!", "You are not member of any project named the way you specified.","projecterror.png");
                }
                if(res == 0){
                    openMenu = true;         //setta il flag per poi aprire la finestra col menù del progetto
                    reOpenHome = false;
                    prjName = home.showProjectName;
                    home.dispose();
                }
                if(res == 3){       //l'utente vuole visualizzare il menu di un progetto di cui ha richiesto l'eliminazione
                    int reqRem = buf.getInt();      //richieste rimanenti per la cancellazione del progetto da parte del server
                    cancelRequest cancel = new cancelRequest(prjName, reqRem, waitUser);
                    cancel.setLocationRelativeTo(null);
                    cancel.setVisible(true);        //viene richiesto all'utente se desidera annullare l'invio della richiesta di cancellazione del progetto
                    waitUser.waitUser();
                    if(cancel.operation == 0){      //l'utente non vuole annullare la richiesta di cancellazione mandata in precedenza
                        inHome = 1;
                        reOpenHome = false;
                    }
                    else{           //il client invia al server la richiesta di annullamento della richiesta di cancellazione
                        buf.clear();
                        buf.putInt(21);
                        buf.put(prjName.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);
                        buf.flip();
                        if(buf.getInt() == 1){      //mentre l'utente pensava, il progetto è stato eliminato definitivamente
                            inHome = 1;     //ripresenta la home page del sistema
                            reOpenHome = false;
                            c.newMsgFrame("Project deleted!", "Too late :(. This project has been permanently removed from the server.", "critical.png");
                        }
                        else{       //richiesta di cancellazione annullata
                            byte [] arr = new byte[buf.remaining()];
                            buf.get(arr);
                            String chatIp = new String(arr);
                            chats.addChat(prjName, new InetSocketAddress(InetAddress.getByName(chatIp.substring(0, chatIp.indexOf(semiColon))), Integer.parseInt(chatIp.substring(chatIp.lastIndexOf(semiColon)+1))));
                            mr.selector.wakeup();
                            reOpenHome = false;
                            inHome = 1;
                            home.dispose();
                            openMenu = true;         //setta il flag per poi aprire la finestra col menù del progetto
                            c.newMsgFrame("Deletion request canceled!", "Your deletion request for this project has been successfully canceled.", "success.png");
                        }
                    }
                }
            }
            if(home.operation == 1){    //CREAZIONE NUOVO PROGETTO
                if(home.newProjName.isBlank() || home.newProjName.contains(semiColon) || home.newProjName.length() > 500){
                    c.newMsgFrame("Project creation failed!", "Please enter a non-empty name for your new project (max 500 chars). Do not use ';' (semicolon) character in the name.","prjcraetfailed.png");
                    reOpenHome = false;
                    inHome = 1;
                    continue;   //riparte senza comunicare nulla al server
                }
                buf.clear();
                buf.putInt(4);      //il client richiede l'operazione con codice 4: creazione di un nuovo progetto
                buf.put(home.newProjName.getBytes());
                buf.flip();
                clientChannel.write(buf);
                buf.clear();
                clientChannel.read(buf);
                buf.flip();
                int res = buf.getInt();     //risposta del server
                if(res == 0){           //creazione andata a buon fine
                    c.newMsgFrame("Project creation successful!", "Project '"+home.newProjName+"' created successfully!","prjcreated.png");
                    prjName = home.newProjName;
                    byte [] arr = new byte [buf.remaining()];
                    buf.get(arr);
                    String ipAndPort = new String(arr);
                    chats.addChat(prjName, new InetSocketAddress(InetAddress.getByName(ipAndPort.substring(0, ipAndPort.indexOf(semiColon))), Integer.parseInt(ipAndPort.substring(ipAndPort.lastIndexOf(semiColon)+1))));
                    mr.selector.wakeup();
                    inHome = 1;
                    openMenu = true;
                    home.dispose();     //chiude la finestra della home page e visualizza il menu del progetto
                }
                if(res == 1){
                    c.newMsgFrame("Project creation failed!", "A project called '"+home.newProjName+"' already exists. Please try to change something in the name or to add some characters. Let your imagination work :D.","prjcraetfailed.png");
                    inHome = 1;     //restiamo nella home page
                    reOpenHome = false;
                }
            }
            if(home.operation == 0){    //LOG OUT
                buf.clear();
                buf.putInt(2);                          //il client richiede il log out al server (operazione con codice 2)
                buf.flip();
                registration.unRegisterForNotification(name);       //il client si cancella dal servizio di notifica callback
                clientChannel.write(buf);
                JFrame msg = new JFrame("Logged out");
                msg.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JLabel label = new JLabel("Hope to see you soon back on WORTH "+name+" ;)");
                label.setFont(new java.awt.Font("Ubuntu", 1, 16));
                JLabel fakelabel1 = new JLabel(new ImageIcon("icone"+File.separator+"loggedout.png"));
                JLabel fakelabel2 = new JLabel(" ");
                msg.getContentPane().add(BorderLayout.NORTH,fakelabel1);
                msg.getContentPane().add(BorderLayout.CENTER,label);
                msg.getContentPane().add(BorderLayout.SOUTH,fakelabel2);
                msg.pack();
                msg.setLocationRelativeTo(null);
                msg.setVisible(true);
                msg.setAlwaysOnTop(true);
                inHome = 0;         //chiude la finestra di home page
            }
            if(home.operation == 3){    //l'utente richiede la lista dei progetti di cui è membro
                buf.clear();
                buf.putInt(6);      //codice operazione lista dei progetti: 6; il client richiede la lunghezza della lista dei progetti
                buf.flip();
                clientChannel.write(buf);
                buf.clear();
                clientChannel.read(buf);    //il client riceve dal server la lunghezza della lista di progetti
                buf.flip();
                int size = buf.getInt();
                ByteBuffer buff = ByteBuffer.allocateDirect(size);      //allocazione di un buffer delle giuste dimensioni per consentire la ricezione di tutta la lista di progetti
                buf.clear();
                buf.putInt(16);     //operazione 16: il client comunica che è pronto a ricevere la lista dei progetti
                buf.flip();
                clientChannel.write(buf);
                buff.clear();
                clientChannel.read(buff);       //il client riceve la lista di progetti
                buff.flip();
                byte [] arr = new byte[buff.limit()];
                buff.get(arr);
                ArrayList<String> projs = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});     //lettura ArrayList serializzato in json dal server
                if(projs.isEmpty()){
                    c.newMsgFrame("Not member of any project!", "You are not member of any project. What about creating a brand new one?","nocontent.png");
                    inHome = 1;
                    reOpenHome = false;
                }
                else{
                    c.newScrollFrame("Your projects list", projs.toString()+"\n\n");
                    inHome = 1;
                    reOpenHome = false;
                }
            }
            if(home.operation == 4){        //visualizzazione finestra profile settings
                buf.clear();
                buf.putInt(6);      //codice operazione lista dei progetti: 6; il client richiede la lunghezza della lista dei progetti
                buf.flip();
                clientChannel.write(buf);
                buf.clear();
                clientChannel.read(buf);    //il client riceve dal server la lunghezza della lista di progetti
                buf.flip();
                int size = buf.getInt();
                ByteBuffer buff = ByteBuffer.allocateDirect(size);      //allocazione di un buffer delle giuste dimensioni per consentire la ricezione di tutta la lista di progetti
                buf.clear();
                buf.putInt(16);     //operazione 16: il client comunica che è pronto a ricevere la lista dei progetti
                buf.flip();
                clientChannel.write(buf);
                buff.clear();
                clientChannel.read(buff);       //il client riceve la lista di progetti
                buff.flip();
                byte [] arr = new byte[buff.limit()];
                buff.get(arr);
                ArrayList<String> projs = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});     //lettura ArrayList serializzato in json dal server
                int inProfile = 999;
                
                /* l'utente è nella pagina del suo profilo */
                boolean reOpenProfile = false;
                Profile_Settings profile = new Profile_Settings(name,projs.size(),callBackReceiver.checkUserState(name),waitUser,showNotification);
                profile.setLocationRelativeTo(null);
                profile.setVisible(true);
                while(inProfile != 0){
                    if(reOpenProfile){
                        profile = new Profile_Settings(name,projs.size(),callBackReceiver.checkUserState(name),waitUser,showNotification);
                        profile.setLocationRelativeTo(null);
                        profile.setVisible(true);
                    }
                    waitUser.waitUser();
                    if(profile.operation == 0){     //pulsante BACK to home page premuto
                        inProfile = 0;      //esce dalla pagina del profilo
                        reOpenHome = true;
                        inHome = 1;
                    }
                    if(profile.operation == 1){     //DELETE account button premuto
                        AreYouSure areusure = new AreYouSure(waitUser);
                        areusure.setLocationRelativeTo(null);
                        areusure.setVisible(true);
                        waitUser.waitUser();
                        if(areusure.sure){      //se l'utente è sicuro di voler cancellare il suo account
                            buf.clear();
                            buf.putInt(7);          //il client richiede l'operazione di rimozione dell'account (codice 7)
                            outToServer = name+semiColon+profile.psw;
                            buf.put(outToServer.getBytes());
                            buf.flip();
                            clientChannel.write(buf);               //invio delle credenziali al server per il controllo
                            buf.clear();
                            clientChannel.read(buf);                //ricezione della risposta del server al controllo delle credenziali
                            buf.flip();
                            inFromServer = buf.getInt();        //risposta del server
                            if(inFromServer == 1){
                                c.newMsgFrame("Incorrect password!", "CANNOT DELETE ACCOUNT, the inserted password is not associated to this username! Try again.","wrongpsw.png");
                                inProfile = 1;      //ripresenta la pagina del profilo
                                reOpenProfile = true;
                            }
                            else{
                                JFrame deleted = new JFrame("Account removal successful!");
                                deleted.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                JLabel label = new JLabel("Bye bye "+name+", your account now doesn't exist anymore!");
                                label.setFont(new java.awt.Font("Ubuntu", 1, 16));
                                JLabel fakelabel1 = new JLabel(new ImageIcon("icone"+File.separator+"sad.png"));
                                JLabel fakelabel2 = new JLabel(" ");
                                deleted.getContentPane().add(BorderLayout.NORTH,fakelabel1);
                                deleted.getContentPane().add(BorderLayout.CENTER,label);
                                deleted.getContentPane().add(BorderLayout.SOUTH,fakelabel2);
                                deleted.pack();
                                deleted.setLocationRelativeTo(null);
                                deleted.setVisible(true);
                                deleted.toFront();
                                deleted.setAlwaysOnTop(true);
                                inProfile = 0;      //chiude la pagina del profilo
                                inHome = 0;      //esce dalla home page
                            }
                        }
                        else{       //l'utente ha cambiato idea
                            inProfile = 1;
                            reOpenProfile = true;
                        }
                    }
                    if(profile.operation == 2){     //operazione richiesta: CAMBIO DI PASSWORD
                        if(profile.newPassword.contains(semiColon) || profile.newPassword.isBlank() || profile.newPassword.length() > 50){
                            c.newMsgFrame("New password not acceptable!", "Please choose a not-blank password (max 50 chars) and do not use ';' (semicolon) character.","badcred.png");
                            inProfile = 1;      //ripresenta la pagina del profilo
                            reOpenProfile = false;
                        }
                        else{
                            buf.clear();
                            buf.putInt(8);
                            outToServer = profile.psw+semiColon+profile.newPassword;
                            buf.put(outToServer.getBytes());
                            buf.flip();
                            clientChannel.write(buf);
                            buf.clear();
                            clientChannel.read(buf);
                            buf.flip();
                            if(buf.getInt() == 1){      //cambio di password fallito
                                c.newMsgFrame("Incorrect password!", "CANNOT CHANGE PASSWORD, the current password you inserted is not associated to your username! Try again.","wrongpsw.png");
                                inProfile = 1;      //ripresenta la pagina del profilo
                                reOpenProfile = false;
                            }
                            else{
                                c.newMsgFrame("Password changed!", "Your password was successfully modified!","acceptedpsw.png");
                                inProfile = 1;      //ripresenta la pagina del profilo
                                reOpenProfile = false;
                            }
                        }
                    }
                    if(profile.operation == 3){         //disattivazione/riattivazione delle notifiche di messaggi delle chats
                        showNotification = !showNotification;
                        mr.showNotifications = showNotification;        //il thread di ricezione dei messaggi non mostra le notifiche se queste vengono disattivate
                        if(showNotification) c.newMsgFrame("Notifications active!", "Messages notifications from your projects chats have been reactivated.", "notOn.png");
                        else c.newMsgFrame("Notifications deactivated!", "Messages notifications from your projects chats have been deactivated.", "notOff.png");
                        inProfile = 1;
                        reOpenProfile = false;
                    }
                }
            }
            
            /* siamo fuori dalla pagina del profilo */
            /* torniamo nella home page */
            
            if(home.operation == 5){        //l'utente ha richiesto la LISTA DI TUTTI GLI UTENTI DI WORTH
                c.newScrollFrame("WORTH users list", callBackReceiver.listUsers()+"\n");
                reOpenHome = false;
                inHome = 1;
            }
            
            if(home.operation == 6){        //l'utente ha richiesto la LISTA DEGLI UTENTI ONLINE
                c.newScrollFrame("WORTH online users list", callBackReceiver.listOnlineUsers().toString()+"\n");
                reOpenHome = false;
                inHome = 1;
            }
            
            if(home.operation == 7){        //l'utente vuole controllare se ci sono progetti in cui deve essere aggiunto (sincronizzazione col server)
                buf.clear();
                buf.putInt(18);     //operazione del server con codice 18
                buf.flip();
                clientChannel.write(buf);
                buf.clear();
                clientChannel.read(buf);
                buf.flip();
                int size = buf.getInt();
                if(size == 0){      //non ci sono progetti da sincronizzare (a cui l'utente deve essere aggiunto come membro effettivo)
                    c.newMsgFrame("No project to be synchronized!", "Nobody added you as a project member. You have no new project to be synchronized.", "notsync.png");
                    inHome = 1;
                    reOpenHome = false;
                    continue;
                }
                ByteBuffer buff = ByteBuffer.allocateDirect(size);      //alloca un buffer di ricezione delle giuste dimensioni (della lunghezza della lista che sta per ricevere)
                buf.clear();
                buf.putInt(16);     //operazione 16: il client comunica al server di essere pronto a ricevere la lista degli indirizzi delle chat dei nuovi progetti
                buf.flip();
                clientChannel.write(buf);
                buff.clear();
                clientChannel.read(buff);       //il client riceve la lista degli InetSocketAddress
                buff.flip();
                byte [] arr = new byte[buff.limit()];
                buff.get(arr);
                Hashtable<String,InetSocketAddress> ips = obj.readValue(arr, new TypeReference<Hashtable<String,InetSocketAddress>>(){});
                Iterator<Entry<String,InetSocketAddress>> iterator = ips.entrySet().iterator();
                Entry<String,InetSocketAddress> en;
                while(iterator.hasNext()){
                    en = iterator.next();
                    chats.addChat(en.getKey(), en.getValue());
                }
                mr.selector.wakeup();       //sveglia il thread di ricezione dei messaggi così che l'utente possa iniziare a ricevere i messaggi dalle nuove chat
                inHome = 1;
                reOpenHome = false;
                c.newMsgFrame("Projects synchronized!", "Your project list has been synchronized! You are now a member of "+ips.size()+" new projects.", "sync.png");
            }
            
            /* siamo infondo al while della home page, controlliamo se dobbiamo visualizzare il menu di un progetto */
            
            if(openMenu){     //operazioni di creazione di un nuovo progetto e visualizzazione del menu di un progetto
                ProjectMenu menu;
                int inMenu = 999;
                String newCard;
                String newDescription;
                String cardName;
                String list;
                boolean reOpenMenu = false;
                
                /* siamo nella pagina del menu del progetto */
                menu = new ProjectMenu(prjName,waitUser,name);        //visualizzazione finestra del menu di progetto
                menu.setLocationRelativeTo(null);
                menu.setVisible(true);
                while(inMenu!=0){
                    if(reOpenMenu){
                        menu = new ProjectMenu(prjName,waitUser,name);        //visualizzazione finestra del menu di progetto
                        menu.setLocationRelativeTo(null);
                        menu.setVisible(true);
                    }
                    waitUser.waitUser();
                    if(menu.operation == 0){        //pulsante back to the home page premuto
                        inHome = 1;         //ripresenta la pagina della home page
                        reOpenHome = true;
                        reOpenMenu = false;
                        openMenu = false;   //resetta l'indice di operazione che richiede la visualizzazione del menu di progetto
                        inMenu = 0;         //uscita dalla finestra di menu
                    }
                    if(menu.operation == 1){        //CREAZIONE NUOVA CARD DI PROGETTO
                        newCard = menu.newCardName;
                        newDescription = menu.newCardDescription;
                        if(newCard.isBlank() || newCard.contains(semiColon) || newDescription.contains(semiColon) || newCard.length() > 500){
                            c.newMsgFrame("Card name and description not acceptable!", "Please choose a not-blank name (max 500 chars) for this new card and do not use ';' (semicolon) character in either name or description.","cardcreatfail.png");
                            inMenu = 1;     //ripresenta la pagina di menu
                            reOpenMenu = false;
                            continue;
                        }
                        if(newDescription.length() > 1000){     //descrizione inserita troppo lunga
                            c.newMsgFrame("Card description too long!", "The inserted description is too long. Please reduce it to a maximum of 1000 characters.","cardcreatfail.png");
                            inMenu = 1;     //ripresenta la pagina di menu
                            reOpenMenu = false;
                        }
                        else{       //nome e descrizione inseriti rispettano i requisiti
                            buf.clear();
                            buf.putInt(9);          //operazione di creazione nuova card (codice 9)
                            outToServer = prjName+semiColon+newCard+semiColon+newDescription;
                            buf.put(outToServer.getBytes());
                            buf.flip();
                            clientChannel.write(buf);
                            buf.clear();
                            clientChannel.read(buf);
                            buf.flip();
                            inFromServer = buf.getInt();
                            if(inFromServer == 1){
                                c.newMsgFrame("Card already exists!", "Cannot create card. A card named '"+newCard+"' already exists in this project.","cardcreatfail.png");
                                inMenu = 1;     //ripresenta la pagina di menu
                                reOpenMenu = false;
                            }
                            else{
                                if(inFromServer == 2){
                                    c.newMsgFrame("Project unmodifiable!", "FORBIDDEN! This project cannot be modified because it is in deletion status (i.e. some members have sent their deletion request for it.)", "forbidden.png");
                                    inMenu = 1;
                                    reOpenMenu = false;
                                }
                                else{
                                    c.newMsgFrame("Card created!", "Card '"+newCard+"' created successfully!","cardcreated.png");
                                    inMenu = 1;     //ripresenta la pagina di menu
                                    reOpenMenu = false;
                                }
                            }
                        }
                    }
                    if(menu.operation == 2){        //visualizzazione PAGINA INFORMAZIONI CARD
                        buf.clear();
                        buf.putInt(10);     //operazione con codice 10
                        outToServer = prjName+semiColon+menu.cardToShow;
                        buf.put(outToServer.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);
                        buf.flip();
                        if(buf.getInt() == 1){
                            c.newMsgFrame("Nonexistent card!", "A card named '"+menu.cardToShow+"' doens't exist in this project. Why don't you create it?","nocard.png");
                            inMenu = 1;
                            reOpenMenu = false;
                        }
                        else{       //visualizza la pagina con le informazioni della card
                            bytesSent = buf.getInt();
                            byte [] arr = new byte[bytesSent];
                            buf.get(arr);
                            outToServer = new String(arr);
                            String aut = outToServer.substring(0, outToServer.indexOf(semiColon));
                            list = outToServer.substring(outToServer.indexOf(semiColon)+1, outToServer.lastIndexOf(semiColon));
                            String des = outToServer.substring(outToServer.lastIndexOf(semiColon)+1);
                            CardPageFrame pageInfo = new CardPageFrame(menu.cardToShow, aut, prjName, des, list);
                            pageInfo.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                            pageInfo.setLocationRelativeTo(null);
                            pageInfo.setVisible(true);
                            pageInfo.toFront();
                            pageInfo.setAlwaysOnTop(true);
                            inMenu = 1;
                            reOpenMenu = false;
                        }
                    }
                    if(menu.operation == 3){        //pulsante LIST CARDS premuto
                        buf.clear();
                        buf.putInt(11);      //codice operazione lista delle cards: 11; il client richiede la lunghezza della lista delle cards di progetto
                        buf.put(prjName.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);    //il client riceve dal server la lunghezza della lista delle cards
                        buf.flip();
                        int size = buf.getInt();
                        ByteBuffer buff = ByteBuffer.allocateDirect(size);      //allocazione di un buffer delle giuste dimensioni per consentire la ricezione di tutta la lista delle cards
                        buf.clear();
                        buf.putInt(16);     //operazione 16: il client comunica che è pronto a ricevere la lista delle cards
                        buf.flip();
                        clientChannel.write(buf);
                        buff.clear();
                        clientChannel.read(buff);       //il client riceve la lista delle cards di progetto
                        buff.flip();
                        byte [] arr = new byte[buff.limit()];
                        buff.get(arr);
                        ArrayList<String> cards = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});     //lettura ArrayList serializzato in json dal server
                        if(cards.isEmpty()){
                            c.newMsgFrame("No cards!", "This project has no cards yet.","nocontent.png");
                            inMenu = 1;
                            reOpenMenu = false;
                        }
                         else{
                            c.newScrollFrame(prjName+" project Card list", cards.toString()+"\n\n");
                            inMenu = 1;
                            reOpenMenu = false;
                        }
                    }
                    if(menu.operation == 4){        //l'utente richiede di spostare una card in una lista
                        cardName = menu.cardToMove;
                        list = menu.destinationList;
                        //conversione del nome della lista nel giusto formato atteso dal server:
                        if(list.contains("progress")) list = "inProgress";
                        if(list.contains("revised")) list = "toBeRevised";
                        if(list.contains("Done")) list = "done";
                        buf.clear();
                        buf.putInt(12);     //operazione con codice 12 (spostare una card)
                        outToServer = prjName+semiColon+cardName+semiColon+list;
                        buf.put(outToServer.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);
                        buf.flip();
                        inFromServer = buf.getInt();        //risposta del server
                        if(inFromServer == 1){          //la card da spostare non esiste nel progetto
                            c.newMsgFrame("Nonexistent card!", "A card named '"+cardName+"' doens't exist in this project. Why don't you create it?","nocard.png");
                            inMenu = 1;
                            reOpenMenu = false;
                            continue;
                        }
                        if(inFromServer == 2){      //lo spostamento non è consentito
                            JFrame constraints = new JFrame("Movement not allowed!");
                            constraints.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                            JLabel label = new JLabel("Card '"+cardName+"' cannot be moved to '"+list+"' list. Please follow the constraints shown below:");
                            label.setFont(new java.awt.Font("Ubuntu", 1, 16));
                            JLabel fakelabel1 = new JLabel("todo -> in Progress; in Progress -> to Be Revised; in Progress -> done; to Be Revised -> in Progress; to Be Revised -> done");
                            ImageIcon img = new ImageIcon("icone"+File.separator+"cardMovementsRules.png");
                            JLabel fakelabel2 = new JLabel(img);
                            fakelabel1.setFont(new java.awt.Font("Ubuntu", 0, 16));
                            constraints.getContentPane().add(BorderLayout.NORTH,label);
                            constraints.getContentPane().add(BorderLayout.CENTER,fakelabel2);
                            constraints.getContentPane().add(BorderLayout.SOUTH,fakelabel1);
                            constraints.pack();
                            constraints.setLocationRelativeTo(null);
                            constraints.setVisible(true);
                            constraints.toFront();
                            constraints.setAlwaysOnTop(true);
                            inMenu = 1;
                            reOpenMenu = false;
                            continue;
                        }
                        c.newMsgFrame("Movement successful!", "Card '"+cardName+"' is now in '"+list+"' list.","movesucc.png");    //lo spostamento è stato eseguito
                        inMenu = 1;
                        reOpenMenu = false;
                    }
                    if(menu.operation == 5){        //richiesta la STORIA DI UNA CARD
                        buf.clear();
                        buf.putInt(13);
                        outToServer = prjName+semiColon+menu.historyWantedCard;
                        buf.put(outToServer.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);        //riceve un primo output dal server
                        buf.flip();
                        if(buf.getInt() == 1){  //la card non esiste nel progetto
                            c.newMsgFrame("Nonexistent card!", "A card named '"+menu.historyWantedCard+"' doens't exist in this project. Why don't you create it?","nocard.png");
                            inMenu = 1;
                            reOpenMenu = false;
                            continue;
                        }
                        //viene mostrata la storia della carta
                        int size = buf.getInt();        //il client riceve la lunghezza della storia della card esistente
                        ByteBuffer buff = ByteBuffer.allocateDirect(size);      //allocazione di un buffer delle giuste dimensioni per consentire la ricezione di tutta la storia della card
                        buf.clear();
                        buf.putInt(16);     //operazione 16: il client comunica che è pronto a ricevere la storia della card
                        buf.flip();
                        clientChannel.write(buf);
                        buff.clear();
                        clientChannel.read(buff);       //il client riceve la storia della card da visualizzare
                        buff.flip();
                        byte [] arr = new byte[buff.limit()];
                        buff.get(arr);
                        ArrayList<String> history = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});
                        c.newScrollFrame(menu.historyWantedCard+" card history", history.toString()+"\n\n");
                        inMenu = 1;
                        reOpenMenu = false;
                    }
                    if(menu.operation == 6){        //richiesta la CANCELLAZIONE DEL PROGETTO
                        addingDeleteRequest delReqFrame = new addingDeleteRequest(waitUser);
                        delReqFrame.setLocationRelativeTo(null);
                        delReqFrame.setVisible(true);           //viene visualizzata la finestra con gli avvisi
                        waitUser.waitUser();
                        if(delReqFrame.operation == 0){     //l'utente ha cambiato idea
                            inMenu = 1;
                            reOpenMenu = true;
                        }
                        else{
                            InetSocketAddress chatToBeRemoved = chats.getPrAddress(prjName);
                            chats.removeChat(prjName);
                            mr.selector.wakeup();
                            buf.clear();
                            buf.putInt(14);
                            buf.put(prjName.getBytes());
                            buf.flip();
                            clientChannel.write(buf);
                            buf.clear();
                            clientChannel.read(buf);
                            buf.flip();
                            inFromServer = buf.getInt();
                            if(inFromServer == -1){
                                c.newMsgFrame("Project deletion failed!", "CANNOT DELETE PROJECT! Remember that this action is only allowed when all the cards are in the done list.","nodelete.png");
                                chats.addChat(prjName, chatToBeRemoved);
                                chats.putNewMsg(prjName, "[WORTH SYSTEM]: old messages lost while trying to delete this project");
                                mr.selector.wakeup();
                                inMenu = 1;
                                reOpenMenu = true;
                            }
                            else{
                                if(inFromServer == 0){
                                    c.newMsgFrame("Project deleted!", "You were the last remaining to send a delete request. Project deletion completed successfully!","prjdelsucc.png");
                                    inMenu = 0;     //chiude la finestra del menu del progetto eliminato e ripresenta la home page
                                    openMenu = false;   //resetta l'indice di operazione che richiede la visualizzazione del menu di progetto
                                    inHome = 1;
                                    reOpenHome = true;
                                    reOpenMenu = false;
                                }
                                else{
                                    c.newMsgFrame("Delete request added!", "Your delete request has been successfully added. "+inFromServer+" member/s remaining.", "delreqsucc.png");
                                    inMenu = 0;     //chiude la finestra del menu del progetto eliminato e ripresenta la home page
                                    openMenu = false;   //resetta l'indice di operazione che richiede la visualizzazione del menu di progetto
                                    inHome = 1;
                                    reOpenMenu = false;
                                    reOpenHome = true;
                                }
                            }
                        }
                    }
                    if(menu.operation == 7){        //richiesta l'ELIMINAZIONE DI UNA CARD
                        buf.clear();
                        buf.putInt(15);
                        outToServer = prjName+semiColon+menu.cardToDelete;
                        buf.put(outToServer.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);
                        buf.flip();
                        inFromServer = buf.getInt();
                        if(inFromServer == 1){      //il server comunica che la carta da eliminare non esiste
                            c.newMsgFrame("Nonexistent card!", "CANNOT DELETE CARD! A card named '"+menu.cardToDelete+"' doens't exist in this project.","nocard.png");
                            inMenu = 1;     //restiamo nel menu di progetto dopo l'eliminazione della card
                            reOpenMenu = false;
                        }
                        else{
                            if(inFromServer == 2){
                                c.newMsgFrame("Project unmodifiable!", "FORBIDDEN! This project cannot be modified because it is in deletion status (i.e. some members have sent their deletion request for it.)", "forbidden.png");
                                inMenu = 1;
                                reOpenMenu = false;
                            }
                            else{
                                c.newMsgFrame("Card deleted!", "Card '"+menu.cardToDelete+"' deletion completed successfully!","carddelsucc.png");
                                inMenu = 1;     //restiamo nel menu di progetto dopo l'eliminazione della card
                                reOpenMenu = false;
                            }
                        }
                    }
                    if(menu.operation == 8){        //richiesta la visualizzazione della finestra della chat e dei membri
                        int inChatAndMembers = 1;
                        boolean reOpenChat = false;
                        buf.clear();
                        buf.putInt(19);     //richieda la lista di membri del progetto
                        buf.put(prjName.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);
                        buf.flip();
                        ByteBuffer buff = ByteBuffer.allocateDirect(buf.getInt());
                        buf.clear();
                        buf.putInt(16);
                        buf.flip();
                        clientChannel.write(buf);
                        buff.clear();
                        clientChannel.read(buff);
                        buff.flip();
                        byte [] arr = new byte[buff.limit()];
                        buff.get(arr);
                        ArrayList<String> members = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});
                        callBackReceiver.setProjectMembersList(members);        //setta la lista di membri nel ricevitore di callbacks in modo da averne sempre aggiornato lo stato
                        ChatFrame chatFrame = new ChatFrame(prjName,chats.getPrAddress(prjName),name,chats,waitUser,onlineSince);
                        chatFrame.setLocationRelativeTo(null);
                        chatFrame.setVisible(true);
                        mr.setChatWindow(chatFrame);                                //setta sia nel ricevitore di messaggi che nel ricevitore di callbacks la finestra di chat appena aperta
                        callBackReceiver.setChatMemberStatusFrame(chatFrame);
                        callBackReceiver.setMemberStatusAtStart();      //setta la lista di membri e status dei membri nel progetto
                        while(inChatAndMembers != 0){       //fintanto che l'utente rimane nella finestra della chat di progetto
                            if(reOpenChat){
                                buf.clear();
                                buf.putInt(19);     //richieda la lista di membri del progetto
                                buf.put(prjName.getBytes());
                                buf.flip();
                                clientChannel.write(buf);
                                buf.clear();
                                clientChannel.read(buf);
                                buf.flip();
                                buff = ByteBuffer.allocateDirect(buf.getInt());
                                buf.clear();
                                buf.putInt(16);
                                buf.flip();
                                clientChannel.write(buf);
                                buff.clear();
                                clientChannel.read(buff);
                                buff.flip();
                                arr = new byte[buff.limit()];
                                buff.get(arr);
                                members = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});
                                callBackReceiver.setProjectMembersList(members);        //setta la lista di membri nel ricevitore di callbacks in modo da averne sempre aggiornato lo stato
                                chatFrame = new ChatFrame(prjName,chats.getPrAddress(prjName),name,chats,waitUser,onlineSince);
                                chatFrame.setLocationRelativeTo(null);
                                chatFrame.setVisible(true);
                                mr.setChatWindow(chatFrame);                                //setta sia nel ricevitore di messaggi che nel ricevitore di callbacks la finestra di chat appena aperta
                                callBackReceiver.setChatMemberStatusFrame(chatFrame);
                                callBackReceiver.setMemberStatusAtStart();      //setta la lista di membri e status dei membri nel progetto
                            }
                            waitUser.waitUser();
                            if(chatFrame.operation == 0){       //l'utente ha premuto il pulsante per tornare indietro al menu del progetto
                                mr.resetChatWindow();
                                callBackReceiver.resetChatMemberStatusFrame();
                                inChatAndMembers = 0;       //esce dalla finestra della chat
                                inMenu = 1;     //ripresenta la pagina del menu di progetto
                                reOpenMenu = true;
                                reOpenChat = false;
                            }
                            if(chatFrame.operation == 1){       //l'utente ha richiesto di aggiungere un membro al progetto
                                String userToAdd = chatFrame.userToAdd;
                                if(userToAdd.equals(name)){     //l'utente ha digitato il suo stesso nome
                                    c.newMsgFrame("Are you kidding me?", "Why should add yourself as a member another time?", "kidding.png");
                                    inChatAndMembers = 1;
                                    reOpenChat = false;
                                    continue;
                                }
                                buf.clear();
                                buf.putInt(17);
                                outToServer = userToAdd+semiColon+prjName;
                                buf.put(outToServer.getBytes());
                                buf.flip();
                                clientChannel.write(buf);
                                buf.clear();
                                clientChannel.read(buf);
                                buf.flip();
                                inFromServer = buf.getInt();
                                if(inFromServer == 1){      //l'utente che si vuole aggiungere come membro non è registrato al servizio
                                    c.newMsgFrame("User not registered!", "CANNOT ADD MEMBER. The username you inserted is not associated to any WORTH account.", "notMember.png");
                                }
                                else{
                                    if(inFromServer == 2){
                                        c.newMsgFrame("Already a member!", "Add failed! The user you want to add is already a member of this project.", "alreadyMember.png");
                                    }
                                    else{
                                        if(inFromServer == 3){
                                            c.newMsgFrame("Project unmodifiable!", "FORBIDDEN! This project cannot be modified because it is in deletion status (i.e. some members have sent their deletion request for it.)", "forbidden.png");
                                        }
                                        else{       //inFromServer == 0 , membro aggiunto correttamente
                                            c.newMsgFrame("Member added!", userToAdd+" is now a member of '"+prjName+"' project! Tell him/her to check the new added memberships! ;D", "yesMember.png");
                                        }
                                    }
                                }
                                inChatAndMembers = 1;       //rimostra la finestra della chat
                                reOpenChat = false;
                            }
                            if(chatFrame.operation == 2){       //l'utente vuole essere rimosso dal progetto
                                leavingProject leaving = new leavingProject(waitUser);
                                leaving.setLocationRelativeTo(null);
                                leaving.setVisible(true);
                                waitUser.waitUser();
                                if(leaving.operation == 1){     //il client richiede al server la rimozione dell'utente dai membri del progetto
                                    buf.clear();
                                    buf.putInt(20);
                                    buf.put(prjName.getBytes());
                                    buf.flip();
                                    clientChannel.write(buf);
                                    buf.clear();
                                    clientChannel.read(buf);
                                    buf.flip();
                                    inFromServer = buf.getInt();
                                    if(inFromServer == 0){
                                        c.newMsgFrame("Cannot leave project!", "You are the only member of this project. If you want to leave it, please delete it from the menu.", "cantLeave.png");
                                        inChatAndMembers = 1;
                                        reOpenChat = true;      //ripresenta la finestra della chat e dei membri
                                    }
                                    else{       //rimozione consentita
                                        if(inFromServer == 1){
                                            chatFrame.dispose();
                                            c.newMsgFrame("Project leaved!", "You leaved '"+prjName+"' project successfully!", "success.png");
                                            mr.resetChatWindow();
                                            callBackReceiver.resetChatMemberStatusFrame();
                                            chats.removeChat(prjName);
                                            mr.selector.wakeup();
                                            inChatAndMembers = 0;   //chiude la finestra della chat
                                            inMenu = 0;     //chiude la finestra del menu del progetto lasciato e ripresenta la home page
                                            openMenu = false;   //resetta l'indice di operazione che richiede la visualizzazione del menu di progetto
                                            reOpenMenu = false;
                                            inHome = 1;         //mostra la home page del sistema
                                            reOpenHome = true;
                                            reOpenChat = false;
                                        }
                                        else{       //l'utente era l'ultimo membro attivo del progetto, il quale quindi è stato eliminato
                                            chatFrame.dispose();
                                            c.newMsgFrame("Project deleted!", "You were the last active member of this project (the only who hadn't sent his/her deletion request yet). "+prjName+" no longer exists.", "nolongerexists.png");
                                            mr.resetChatWindow();
                                            callBackReceiver.resetChatMemberStatusFrame();
                                            chats.removeChat(prjName);
                                            mr.selector.wakeup();
                                            inChatAndMembers = 0;   //chiude la finestra della chat
                                            inMenu = 0;     //chiude la finestra del menu del progetto lasciato e ripresenta la home page
                                            openMenu = false;   //resetta l'indice di operazione che richiede la visualizzazione del menu di progetto
                                            inHome = 1;         //mostra la home page del sistema
                                            reOpenHome = true;
                                            reOpenMenu = false;
                                            reOpenChat = false;
                                        }
                                    }
                                }
                                else{           //l'utente ha cambiato idea
                                    inChatAndMembers = 1;   //viene visualizzata ancora la finestra della chat e dei membri del progetto
                                    reOpenChat = true;
                                }
                            }
                        }
                        callBackReceiver.resetProjectMemberList();
                        /* fuori dalla finestra della chat di progetto */
                    }
                    if(menu.operation == 9){        //richiesta la lavagna Kanban del progetto
                        JFrame loading = new JFrame("Loading Kanban!");
                        JLabel textLabel = new JLabel("Loading this project Kanban. Please wait while program receives data from WORTH server...");
                        JLabel gifLabel = new JLabel(new ImageIcon("icone"+File.separator+"loadingKanban.gif"));
                        textLabel.setFont(new java.awt.Font("Ubuntu", 1, 17));
                        loading.getContentPane().add(BorderLayout.NORTH,textLabel);
                        loading.getContentPane().add(BorderLayout.CENTER,gifLabel);
                        loading.pack();
                        loading.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                        loading.setLocationRelativeTo(null);
                        loading.setVisible(true);
                        loading.toFront();
                        buf.clear();
                        buf.putInt(11);      //codice operazione lista delle cards: 11; il client richiede la lunghezza della lista delle cards di progetto
                        buf.put(prjName.getBytes());
                        buf.flip();
                        clientChannel.write(buf);
                        buf.clear();
                        clientChannel.read(buf);    //il client riceve dal server la lunghezza della lista delle cards
                        buf.flip();
                        int size = buf.getInt();
                        ByteBuffer buff = ByteBuffer.allocateDirect(size);      //allocazione di un buffer delle giuste dimensioni per consentire la ricezione di tutta la lista delle cards
                        buf.clear();
                        buf.putInt(16);     //operazione 16: il client comunica che è pronto a ricevere la lista delle cards
                        buf.flip();
                        clientChannel.write(buf);
                        buff.clear();
                        clientChannel.read(buff);       //il client riceve la lista delle cards di progetto
                        buff.flip();
                        byte [] arr = new byte[buff.limit()];
                        buff.get(arr);
                        ArrayList<String> cards = obj.readValue(arr, new TypeReference<ArrayList<String>>(){});     //lettura ArrayList serializzato in json dal server
                        projectKanban kanban = new projectKanban(prjName);
                        for(String card : cards){
                            buf.clear();
                            buf.putInt(10);     //operazione con codice 10: richiesta informazioni sulla card
                            outToServer = prjName+semiColon+card;
                            buf.put(outToServer.getBytes());
                            buf.flip();
                            clientChannel.write(buf);
                            buf.clear();
                            clientChannel.read(buf);
                            buf.flip();
                            buf.getInt();
                            bytesSent = buf.getInt();
                            arr = new byte[bytesSent];
                            buf.get(arr);
                            outToServer = new String(arr);
                            list = outToServer.substring(outToServer.indexOf(semiColon)+1, outToServer.lastIndexOf(semiColon));
                            kanban.addCard(card, list);
                        }
                        loading.dispose();
                        
                        kanban.setLocationRelativeTo(null);
                        kanban.setVisible(true);
                        inMenu = 1;
                        reOpenMenu = false;
                    }
                }
                
                /* fuori dalla finestra del menu di progetto */
                
            }
        }
        
        /* fuori dalla home page */
        
    }
    
        catch(Exception e){
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
