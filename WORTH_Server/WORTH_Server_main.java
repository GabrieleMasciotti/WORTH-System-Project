import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BorderLayout;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class WORTH_Server_main {

    public static void main(String[] args) {
        
        try{
        
        int RMIregistryPort;
        int TCPconnectionPort;
        String myIp = InetAddress.getLocalHost().getHostAddress();
        
        setConnectionParameters settings = new setConnectionParameters();
        settings.setLocationRelativeTo(null);
        settings.setVisible(true);
        while(settings.isShowing()){
            Thread.sleep(500);
        }
        if(!settings.defaultSettings){
            RMIregistryPort = Integer.parseUnsignedInt(settings.RMIPort);
            TCPconnectionPort = Integer.parseUnsignedInt(settings.TCPPort);
            myIp = settings.newIP;
            System.setProperty("java.rmi.server.hostname", myIp);
        }
        else{
            RMIregistryPort = 6989;
            TCPconnectionPort = 10000;
        }
        
        //servizio di iscrizione
        Registration_Service regService = new Registration_Service();
        Registration_Interface stub = (Registration_Interface) UnicastRemoteObject.exportObject(regService,0);
        LocateRegistry.createRegistry(RMIregistryPort);
        Registry reg = LocateRegistry.getRegistry(RMIregistryPort);
        reg.rebind("WORTH_Registration_Service",stub);
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        Selector selector = Selector.open();
        ServerSocket socket = serverChannel.socket();
        socket.bind(new InetSocketAddress(myIp, TCPconnectionPort));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        Set <SelectionKey> readyKeys;
        Iterator <SelectionKey> iter;
        int bytesReceived;
        byte [] arr;
        String inFromClient;
        int loginCheck;
        int deleteCheck;
        String name;
        String psw;
        String newPsw;
        int operation;
        String semiColon = ";";
        String prjName;
        File newProjectFolder;
        boolean bool;
        int result;
        String cardName;
        String cardDescription;
        String outToClient;
        String list;
        ObjectMapper obj = new ObjectMapper();
        Hashtable<String,Project> projects = new Hashtable<>();         //data base dei progetti nel server WORTH
        Hashtable<String,ArrayList<String>> noviceMembers = new Hashtable<>();         //insieme di utenti che sono stati aggiunti come nuovi membri di progetti (in attesa che effettuino il controllo di nuove aggiunte)
        DatagramSocket serverNotificator;      //socket utilizzata dal server per spedire notifiche ai gruppi di multicast delle chat dei progetti
        DatagramPacket notificationPacket;      //pacchetto contenente la notifiche che devono essere inviate
        String notification;        //testo della notifica da inviare
        
        String path = new File(".").getCanonicalPath();
        int firstByte;
        int secondByte;
        int thirdByte;
        int fourthByte;
        String ip;
        InetAddress multicastIp;
        File prjFolder = new File(path+File.separator+"WORTH_projects_serverDirectory");
        ArrayList<String> generatedIps = new ArrayList<>();         //lista di indirizzi ip multicast in uso per le chat di progetto
        File noviceMembersFile = new File(path+File.separator+"noviceMembers.json");
        boolean created = noviceMembersFile.createNewFile();
        if(!created && noviceMembersFile.length()!=0) noviceMembers = obj.readValue(noviceMembersFile, new TypeReference<Hashtable<String,ArrayList<String>>>(){});
        
        if(prjFolder.exists()){
            String[] pfol = prjFolder.list();
            Iterator<String> itera = Arrays.asList(pfol).iterator();      //il server recupera i progetti già esistenti nel sistema
            String aux;
            while(itera.hasNext()){
                aux = itera.next();
                File members = new File(prjFolder.getPath()+File.separator+aux+File.separator+"members.json");    //file con serializzazione della lista dei membri nella cartella del progetto
                ArrayList<String> m;
                m = obj.readValue(members, new TypeReference<ArrayList<String>>(){});       //lettura del file contenente la lista dei membri
                Project pr = new Project(aux);
                pr.setPersistentMemberList(m);
                File deleters = new File(prjFolder.getPath()+File.separator+aux+File.separator+"deleters.json");    //file con serializzazione della lista dei membri che hanno inviato delle richieste di cancellazione
                if(deleters.exists() && deleters.length()!=0){
                    ArrayList<String> d = obj.readValue(deleters, new TypeReference<ArrayList<String>>(){});
                    if(!d.isEmpty()) pr.setPersistentDeletersList(d);
                }
                File cardFolder = new File(prjFolder.getPath()+File.separator+aux+File.separator+"cards");   //cartella contenente le cards di progetto
                File[] cards = cardFolder.listFiles();
                ArrayList<String> history;
                String des;
                String creator;
                for(File card : cards){
                    history = obj.readValue(card, new TypeReference<ArrayList<String>>(){});
                    des = history.get(0).substring(history.get(0).lastIndexOf(semiColon)+1);
                    creator = history.get(0).substring(0, history.get(0).indexOf(semiColon));
                    Card c = new Card(card.getName().substring(0, card.getName().lastIndexOf(".json")), des, aux, creator);
                    c.setPersistentVisitedLists(history);
                    pr.addPersistentCard(c, history.get(history.size()-1).substring(history.get(history.size()-1).indexOf(" ;")+2,history.get(history.size()-1).lastIndexOf(semiColon)));
                }
                
                //generazione di un indirizzo ip multicast per ogni progetto (da utilizzare per la chat)
                firstByte = (int) ((Math.random() * (239-225+1)) + 225);    //genera il valore del primo byte casualmente tra 225 e 239
                secondByte = (int) (Math.random() * 256);
                thirdByte = (int) (Math.random() * 256);        //generazione dei restanti 3 byte casualmente tra 0 e 255
                fourthByte = (int) (Math.random() * 256);
                ip = firstByte+"."+secondByte+"."+thirdByte+"."+fourthByte;
                while(generatedIps.contains(ip)){       //fintanto che l'indirizzo generato è già in uso, ripete la generazione casuale
                    firstByte = (int) ((Math.random() * (239-225+1)) + 225);
                    secondByte = (int) (Math.random() * 256);
                    thirdByte = (int) (Math.random() * 256);
                    fourthByte = (int) (Math.random() * 256);
                    ip = firstByte+"."+secondByte+"."+thirdByte+"."+fourthByte;
                }
                generatedIps.add(ip);
                multicastIp = InetAddress.getByName(ip);
                int port = (int)((Math.random() * (65535-2000+1)) + 2000);          //scelta una porta random per la costruzione dell'InetSocketAddress
                pr.setChatAddress(new InetSocketAddress(multicastIp, port));
                
                projects.put(aux,pr);       //aggiunge il progetto definito nel data base del sistema (hashtable)
            }
        }
        else{
            prjFolder.mkdir();      //se la cartella dei progetti non esisteva
        }
        
        JFrame msg = new JFrame("WORTH Server");
        msg.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel("WORTH server is running and is available for connections on port "+socket.getLocalPort()+" at "+myIp+". RMI registry port: "+RMIregistryPort+". Close this window to TERMINATE EXECUTION.");
        label.setFont(new java.awt.Font("Ubuntu", 1, 16));
        JLabel fakelabel1 = new JLabel(" ");
        JLabel fakelabel2 = new JLabel(" ");
        msg.getContentPane().add(BorderLayout.NORTH,fakelabel1);
        msg.getContentPane().add(BorderLayout.CENTER,label);
        msg.getContentPane().add(BorderLayout.SOUTH,fakelabel2);
        msg.pack();
        msg.setVisible(true);
        msg.toFront();
        
        while(true){
            selector.select();
            readyKeys = selector.selectedKeys();
            iter = readyKeys.iterator();
            while(iter.hasNext()){
                SelectionKey chiave = iter.next();
                iter.remove();
                if(!chiave.isValid()){
                    ByteBuffer sendBuffer = (ByteBuffer) chiave.attachment();
                    if(sendBuffer != null){
                        arr = new byte[sendBuffer.remaining()];
                        sendBuffer.get(arr);
                        name = new String(arr);
                        name = name.substring(name.lastIndexOf(semiColon)+1);
                        regService.logOut(name);            //se la connessione viene persa il sistema imposta ad offline lo stato dell'utente
                    }
                    chiave.cancel();
                    continue;
                }
                if(chiave.isAcceptable()){
                    ServerSocketChannel server = (ServerSocketChannel) chiave.channel();
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                }
                else if(chiave.isReadable()){
                    SocketChannel client = (SocketChannel) chiave.channel();
                    ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(4096);
                    ByteBuffer sendBuffer = ByteBuffer.allocateDirect(4096);
                    receiveBuffer.clear();
                    client.read(receiveBuffer);        //lettura dati inviati dall'utente
                    receiveBuffer.flip();
                    if(!receiveBuffer.hasRemaining()){     //la connessione con il client è stata chiusa inaspettatamente
                        sendBuffer = (ByteBuffer) chiave.attachment();
                        if(sendBuffer != null){
                            arr = new byte[sendBuffer.remaining()];
                            sendBuffer.get(arr);
                            name = new String(arr);
                            name = name.substring(name.lastIndexOf(semiColon)+1);
                            regService.logOut(name);            //se la connessione viene persa il sistema imposta ad offline lo stato dell'utente
                        }
                        client.close();
                        chiave.cancel();
                    }
                    else{
                        operation = receiveBuffer.getInt();        //lettura del primo intero inviato dal client (operazione desiderata)
                        if(operation == 0){             //l'operazione richiesta è quella di LOG IN
                            bytesReceived = receiveBuffer.getInt();     //prossimi quattro byte del buffer che indicano il numero di bytes delle credenziali
                            arr = new byte[bytesReceived];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            name = inFromClient.substring(0,inFromClient.lastIndexOf(semiColon));
                            psw = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1, bytesReceived);
                            loginCheck = regService.checkUserLogin(name, psw);
                            sendBuffer.clear();
                            sendBuffer.putInt(0);
                            sendBuffer.putInt(loginCheck);
                            sendBuffer.put(name.getBytes());        //username memorizzato nell'attachment per effettuare il logout
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 1){     //richiesta dal client la lista dei riferimenti alle chat dei progetti di cui l'utente è membro
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            sendBuffer.clear();
                            sendBuffer.putInt(-8);      //writeOp: il server deve inviare al client la lista dei riferimenti alle chat dei progetti
                            sendBuffer.put(receiveBuffer);      //username dell'utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 2){         //il client effettua una operazione di LOG OUT dal servizio
                            sendBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[sendBuffer.remaining()];
                            sendBuffer.get(arr);
                            name = new String(arr);         //recupera dall'attachment il nome utente per effettuarne il log out
                            regService.logOut(name);
                            client.close();                 //chiusura della connessione
                            chiave.cancel();
                        }
                        if(operation == 3){         //il client effettua il LOGIN AUTOMATICO post registrazione avvenuta con successo
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);     //il server legge lo username dell'utente e come sempre lo mette nell'attachment (ci deve rimanere sempre!)
                            sendBuffer.clear();
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            regService.logIn(name);     //imposta lo stato dell'utente ad online
                        }
                        if(operation == 4){         //operazione richiesta: CREAZIONE DI UN NUOVO PROGETTO (codice 4)
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            prjName = new String(arr);      //il server ha ottenuto il nome del nuovo progetto
                            newProjectFolder = new File(prjFolder.getPath()+File.separator+prjName);
                            bool = newProjectFolder.mkdir();    //tenta di creare la nuova cartella del progetto
                            if(!bool){
                                result = 1;  //se la cartella non è stata creata (già ne esiste una con quel nome)
                                receiveBuffer = (ByteBuffer) chiave.attachment();
                                sendBuffer.clear();
                                sendBuffer.putInt(-1);      //segnala al passo writable che deve restituire al client il risultato del controllo per la creazione del nuovo progetto
                                sendBuffer.putInt(result);   //copia lo username nel nuovo attachment aggiungendo il risultato del controllo da inviare al client
                                sendBuffer.put(receiveBuffer);
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                            else{       //non esistono progetti con il nome desiderato, la creazione può essere portata a termine
                                result = 0;
                                receiveBuffer = (ByteBuffer) chiave.attachment();
                                sendBuffer.clear();
                                sendBuffer.putInt(-4);      //segnala al passo writable che deve restituire al client il risultato del controllo per la creazione del nuovo progetto e l'indirizzo per la nuova chat
                                sendBuffer.putInt(result);   //copia lo username nel nuovo attachment aggiungendo il risultato del controllo da inviare al client
                                arr = new byte[receiveBuffer.remaining()];
                                receiveBuffer.get(arr);
                                name = new String(arr);     //nome dell'utente che sta creando il progetto
                                Project np = new Project(prjName);
                                np.addMember(name);                 //aggiunto l'utente creatore come membro del progetto
                                File members = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"members.json");
                                members.createNewFile();
                                obj.writeValue(members, np.getMemberList());
                                //generazione indirizzo ip di multicast per la chat del nuovo progetto
                                firstByte = (int) ((Math.random() * (239-225+1)) + 225);
                                secondByte = (int) (Math.random() * 256);
                                thirdByte = (int) (Math.random() * 256);
                                fourthByte = (int) (Math.random() * 256);
                                ip = firstByte+"."+secondByte+"."+thirdByte+"."+fourthByte;
                                while(generatedIps.contains(ip)){
                                    firstByte = (int) ((Math.random() * (239-225+1)) + 225);
                                    secondByte = (int) (Math.random() * 256);
                                    thirdByte = (int) (Math.random() * 256);
                                    fourthByte = (int) (Math.random() * 256);
                                    ip = firstByte+"."+secondByte+"."+thirdByte+"."+fourthByte;
                                }
                                generatedIps.add(ip);
                                multicastIp = InetAddress.getByName(ip);
                                int port = (int)((Math.random() * (65535-2000+1)) + 2000);
                                np.setChatAddress(new InetSocketAddress(multicastIp, port));
                                projects.put(prjName, np);
                                File cards = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards");
                                cards.mkdir();
                                outToClient = ip+semiColon+port;
                                sendBuffer.putInt(outToClient.getBytes().length);       //indirizzo da inviare al client
                                sendBuffer.put(outToClient.getBytes());
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                        if(operation == 5){     //visualizzazione MENU di un progetto esistente
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            prjName = new String(arr);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            if(!projects.containsKey(prjName)) result = 1;      //non esiste il progetto che si vuole visualizzare
                            else{
                                if(!projects.get(prjName).checkMember(name)) result = 2;    //non si è membri del progetto che si vuole visualizzare
                                else{
                                    if(projects.get(prjName).isDeleter(name)) result = 3;       //l'utente ha richiesto l'eliminazione del progetto
                                    else result = 0;    //la visualizzazione viene concessa dal server
                                }
                            }
                            if(result == 0 || result == 1 || result == 2){
                                sendBuffer.clear();
                                sendBuffer.putInt(-1);      //segnala al passo writable che deve inviare il risultato del controllo al client
                                sendBuffer.putInt(result);
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                            else{
                                sendBuffer.clear();
                                sendBuffer.putInt(-12);      //segnala al passo writable che deve inviare il risultato del controllo al client
                                sendBuffer.putInt(result);
                                sendBuffer.putInt(projects.get(prjName).getRemainingRequests());    //il server invia al client anche il numero delle richieste rimanenti per l'eliminazione
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                        if(operation == 6){     //visualizzazione LISTA DEI PROGETTI di cui l'utente è membro
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            sendBuffer.clear();
                            sendBuffer.putInt(-2);      //writeOp: segnala al passo writable che deve restituire al client la lunghezza della lista di progetti
                            sendBuffer.put(receiveBuffer);
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 7){         //ELIMINAZIONE DELL'ACCOUNT
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            name = inFromClient.substring(0,inFromClient.lastIndexOf(semiColon));
                            psw = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);
                            deleteCheck = regService.checkUserDelete(name, psw);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            if(deleteCheck == 0){   //se la cancellazione del profilo è avvenuta con successo
                                Enumeration<Project> e = projects.elements();
                                Project p;
                                ArrayList<String> removedProjects = new ArrayList<>();
                                while(e.hasMoreElements()){
                                    p = e.nextElement();
                                    p.removeMember(name);   //rimozione dell'utente dai membri dei progetti
                                    if(p.getMemberList().isEmpty() || p.getRemainingRequests()==0){    //se l'utente che sta cancellando il profilo è l'unico membro attivo del progetto, quest'ultimo viene cancellato insieme con le sue cards
                                        File pF = new File(prjFolder.getPath()+File.separator+p.getName());
                                        File [] files = pF.listFiles();
                                        for(File f : files){
                                            f.delete();         //eliminazione di tutti i file di progetto
                                        }
                                        File cardsFolder = new File(prjFolder.getPath()+File.separator+p.getName()+File.separator+"cards");
                                        if(cardsFolder.exists()){
                                            File [] cards = cardsFolder.listFiles();
                                            for(File fc : cards){
                                                fc.delete();        //eliminazione di tutti i file delle cards di progetto
                                            }
                                            cardsFolder.delete();   //eliminazione cartella delle cards
                                        }
                                        pF.delete();        //eliminazione cartella del progetto
                                        removedProjects.add(p.getName());
                                    }
                                    else{
                                        File members = new File(prjFolder.getPath()+File.separator+p.getName()+File.separator+"members.json");
                                        obj.writeValue(members, p.getMemberList());
                                    }
                                }
                                removedProjects.forEach(pn -> {
                                    generatedIps.remove(projects.get(pn).getChatAddress().getAddress().getHostAddress());   //rimozione dell'indirizzo della chat di progetto
                                    projects.remove(pn);            //rimozione dei progetti eliminati dal file system del server
                                });
                            }
                            sendBuffer.clear();
                            sendBuffer.putInt(-3);
                            sendBuffer.putInt(deleteCheck);
                            sendBuffer.put(name.getBytes());        //username memorizzato nell'attachment per effettuare il logout
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 8){     //CAMBIO PASSWORD
                            sendBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[sendBuffer.remaining()];
                            sendBuffer.get(arr);
                            name = new String(arr);         //nome utente
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            psw = inFromClient.substring(0,inFromClient.lastIndexOf(semiColon));      //password attuale
                            newPsw = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);   //nuova password
                            receiveBuffer.clear();
                            receiveBuffer.putInt(-1);       //writeOp di restituzione del controllo
                            if(regService.changeUserPassword(name, psw, newPsw)) receiveBuffer.putInt(0);   //cambio password effettuato
                            else receiveBuffer.putInt(1);           //cambio password fallito
                            receiveBuffer.put(name.getBytes());
                            receiveBuffer.flip();
                            chiave.attach(receiveBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 9){     //CREAZIONE DI UNA NUOVA CARD DI PROGETTO
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            prjName = inFromClient.substring(0, inFromClient.indexOf(semiColon)); //nome del progetto a cui aggiungere la card
                            cardName = inFromClient.substring(inFromClient.indexOf(semiColon)+1,inFromClient.lastIndexOf(semiColon));     //nome della nuova card
                            cardDescription = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);   //descrizione della nuova card
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            sendBuffer.clear();
                            if(projects.get(prjName).isInDeletion()){       //il progetto è in stato di eliminazione (sono state mandate delle deletion requests) e quindi non può essere modificato
                                sendBuffer.putInt(-1);
                                sendBuffer.putInt(2);       //segnalazione progetto non modificabile
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                                continue;
                            }
                            File cardFile = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards"+File.separator+cardName+".json");
                            boolean createNewFile = cardFile.createNewFile();
                            if(!createNewFile){     //segnala al client che esiste una card con quel nome
                                sendBuffer.putInt(-1);  //segnalazione (writeOp) al passo writable
                                sendBuffer.putInt(1);
                                sendBuffer.put(name.getBytes());     //username
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                            else{
                                sendBuffer.putInt(-1);      //writeOP
                                sendBuffer.putInt(0);       //segnala l'avvenuta creazione della carta
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                                Card newCard = new Card(cardName, cardDescription, prjName, name);
                                String firstHistoryEl = name+"; created card in list ;todo"+semiColon+cardDescription; //resa persistente la storia e la descrizione della card
                                newCard.addVisitedList(firstHistoryEl);
                                projects.get(prjName).addCard(newCard);
                                obj.writeValue(cardFile, newCard.getLists());
                                notification = "[WORTH Server]: "+name+" created a new card named '"+cardName+"'";
                                notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                serverNotificator = new DatagramSocket();
                                serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                serverNotificator.close();
                            }
                        }
                        if(operation == 10){        //operazione richiesta VISUALIZZAZIONE INFORMAZIONI CARD
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            prjName = inFromClient.substring(0, inFromClient.indexOf(semiColon));
                            cardName = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            sendBuffer.clear();
                            sendBuffer.putInt(-5);      //writeOp
                            outToClient = prjName+semiColon+cardName;
                            sendBuffer.putInt(outToClient.getBytes().length);
                            sendBuffer.put(outToClient.getBytes());    //nome della card di cui reperire le informazioni e progetto di appartenenza
                            sendBuffer.put(name.getBytes());        //nome utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 11){        //richiesta la LISTA DELLE CARDS associate ad un progetto (il server deve inviare la lunghezza della lista al client)
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            prjName = new String(arr);      //nome del progetto di cui sono richieste le cards
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);     //nome dell'utente che richiede la lista delle cards (recuperato dall'attachment)
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            sendBuffer.clear();
                            sendBuffer.putInt(-6);      //segnala al passo writable che deve restituire al client la lunghezza della lista di cards
                            sendBuffer.putInt(prjName.getBytes().length);
                            sendBuffer.put(prjName.getBytes());
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 12){        //operazione richiesta: SPOSTAMENTO DI UNA CARD
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String (arr);
                            prjName = inFromClient.substring(0, inFromClient.indexOf(semiColon));
                            cardName = inFromClient.substring(inFromClient.indexOf(semiColon)+1, inFromClient.lastIndexOf(semiColon));
                            list = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            result = projects.get(prjName).moveCard(cardName, list);
                            Card c = projects.get(prjName).getCard(cardName);   //card nella nuova lista (dopo lo spostamento)
                            if(result == 0){    //se lo spostamento è stato eseguito
                                String historyEl = name+"; moved card in list ;"+list+semiColon;
                                c.addVisitedList(historyEl);
                                File cardFile = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards"+File.separator+cardName+".json");
                                obj.writeValue(cardFile, c.getLists());
                                notification = "[WORTH Server]: "+name+" moved card '"+cardName+"' in list "+list;
                                notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                serverNotificator = new DatagramSocket();
                                serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                serverNotificator.close();
                            }
                            sendBuffer.clear();
                            sendBuffer.putInt(-1);      //il server rispedisce il risultato dello spostamento al client
                            sendBuffer.putInt(result);
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 13){        //VISUALIZZAZIONE STORIA DI UNA CARD
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String (arr);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            prjName = inFromClient.substring(0, inFromClient.indexOf(semiColon));
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            sendBuffer.clear();
                            sendBuffer.putInt(-7);      //demanda tutto al passo writable
                            sendBuffer.putInt(inFromClient.getBytes().length);
                            sendBuffer.put(inFromClient.getBytes());
                            sendBuffer.put(name.getBytes());          //nome utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 14){        //CANCELLAZIONE DI UN PROGETTO
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            prjName = new String(arr);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            Project p = projects.get(prjName);
                            if(p == null || !p.checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            result = p.deletable();
                            if(result == 0) {                       //se il progetto può essere cancellato (tutte le cards sono in done list)
                                int deleteRequestsRemaining = p.addDeleteRequest(name);  //aggiunta la richiesta di cancellazione dell'utente
                                if(deleteRequestsRemaining == 0){        //tutti i membri hanno richiesto di eliminare il progetto, il server procede alla sua rimozione dal sistema
                                    generatedIps.remove(projects.get(prjName).getChatAddress().getAddress().getHostAddress());  //rimozione dell'indirizzo di multicast in uso così che possa essere riutilizzato
                                    projects.remove(prjName);
                                    File pF = new File(prjFolder.getPath()+File.separator+prjName);
                                    File [] files = pF.listFiles();
                                    for(File f : files){
                                        f.delete();         //eliminazione di tutti i file di progetto
                                    }
                                    File cardsFolder = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards");
                                    if(cardsFolder.exists()){
                                        File [] cards = cardsFolder.listFiles();
                                        for(File fc : cards){
                                            fc.delete();        //eliminazione di tutti i file delle cards di progetto
                                        }
                                        cardsFolder.delete();   //eliminazione cartella delle cards
                                    }
                                    pF.delete();        //eliminazione cartella del progetto
                                }
                                else{
                                    File deletersFile = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"deleters.json");
                                    if(!deletersFile.exists()) deletersFile.createNewFile();
                                    obj.writeValue(deletersFile, p.getDeletersList());
                                    notification = "[WORTH Server]: "+name+" added a delete request for this project";
                                    notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                    serverNotificator = new DatagramSocket();
                                    serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                    serverNotificator.close();
                                }
                                sendBuffer.clear();
                                sendBuffer.putInt(-1);
                                sendBuffer.putInt(deleteRequestsRemaining);
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                            else{
                                sendBuffer.clear();
                                sendBuffer.putInt(-1);
                                sendBuffer.putInt(-1);      //il server segnala che il progetto non può essere eliminato poiché non tutte le cards sono in done list
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                        if(operation == 15){        //ELIMINAZIONE DI UNA CARD DI PROGETTO
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            prjName = inFromClient.substring(0, inFromClient.indexOf(semiColon));
                            cardName = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(name)){      //controlli di sicurezza
                                regService.unRegisterForNotification(name);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(name);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            result = projects.get(prjName).deleteCard(cardName);
                            if(result == 0){        //la carta esisteva ed è stata eliminata
                                File cardFile = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards"+File.separator+cardName+".json");
                                cardFile.delete();      //eliminazione del file della carta dal file system
                                notification = "[WORTH Server]: "+name+" deleted card '"+cardName+"'";
                                notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                serverNotificator = new DatagramSocket();
                                serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                serverNotificator.close();
                            }
                            sendBuffer.clear();
                            sendBuffer.putInt(-1);  //(writeOp) deve restituire come sempre il risultato al client
                            sendBuffer.putInt(result);
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 16){
                            chiave.interestOps(SelectionKey.OP_WRITE);      //il server deve inviare al client la lista serializzata nell'attachment (writeOp 9 già presente nell'attachment) dopo che il client ha allocato un buffer di ricezione delle giuste dimensioni
                        }
                        if(operation == 17){        //AGGIUNTA DI UN MEMBRO AD UN PROGETTO
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            inFromClient = new String(arr);
                            name = inFromClient.substring(0, inFromClient.indexOf(semiColon));
                            prjName = inFromClient.substring(inFromClient.indexOf(semiColon)+1);
                            bool = regService.userExist(name);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            String adder = new String(arr);
                            if(projects.get(prjName) == null || !projects.get(prjName).checkMember(adder)){      //controlli di sicurezza
                                regService.unRegisterForNotification(adder);    //client rimosso dalla lista di avvisi callback
                                regService.logOut(adder);            //imposta ad offline lo stato dell'utente
                                client.close();             //il client fraudolento viene buttato fuori dal servizio
                                chiave.cancel();
                                continue;
                            }
                            if(projects.get(prjName).isInDeletion()){           //se il progetto è in stato di eliminazione (non può essere modificato)
                                sendBuffer.clear();
                                sendBuffer.putInt(-1);      //writeOp, comunica al client l'esito dell'aggiunta del nuovo membro
                                sendBuffer.putInt(3);
                                sendBuffer.put(adder.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                                continue;
                            }
                            if(bool){       //l'utente che si vuole aggiungere è registrato al servizio
                                if(projects.get(prjName).checkMember(name)) result = 2;     //l'utente da aggiungere è già membro del progetto
                                else{
                                    //viene aggiunto l'utente alla lista di nuovi membri in attesa che questo controlli le nuove aggiunte
                                    if(!noviceMembers.containsKey(name)){
                                        noviceMembers.put(name, new ArrayList<>());
                                        noviceMembers.get(name).add(prjName);
                                        obj.writeValue(noviceMembersFile, noviceMembers);
                                        result = 0;
                                        notification = "[WORTH Server]: "+adder+" added "+name+" as a new member. Waiting for him/her to synchronize project list.";
                                        notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                        serverNotificator = new DatagramSocket();
                                        serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                        serverNotificator.close();
                                    }
                                    else{
                                        if(noviceMembers.get(name).contains(prjName)) result = 2;       //l'utente da aggiungere è già stato aggiunto
                                        else{
                                            noviceMembers.get(name).add(prjName);
                                            obj.writeValue(noviceMembersFile, noviceMembers);
                                            result = 0;
                                            notification = "[WORTH Server]: "+adder+" added "+name+" as a new member. Waiting for him/her to synchronize project list.";
                                            notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                            serverNotificator = new DatagramSocket();
                                            serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                            serverNotificator.close();
                                        }
                                    }
                                }
                            }
                            else result = 1;        //l'utente da aggiungere non esiste nel sistema
                            sendBuffer.clear();
                            sendBuffer.putInt(-1);      //writeOp, comunica al client l'esito dell'aggiunta del nuovo membro
                            sendBuffer.putInt(result);
                            sendBuffer.put(adder.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 18){        //il server deve inviare al client la lunghezza della lista di indirizzi multicast dei progetti a cui l'utente è stato aggiunto come nuovo membro
                            sendBuffer.clear();
                            sendBuffer.putInt(-10);     //writeOp in cui si preparerà la lista serializzata e si invierà la lunghezza al client
                            sendBuffer.put((ByteBuffer) chiave.attachment());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 19){        //il client richiede la lunghezza della lista di membri ad un progetto
                            sendBuffer.clear();
                            sendBuffer.putInt(-11);     //writeOp in cui si preparerà la lista serializzata e si invierà la lunghezza al client
                            sendBuffer.putInt(receiveBuffer.remaining());   //lunghezza del nome del progetto
                            sendBuffer.put(receiveBuffer);      //nome del progetto
                            sendBuffer.put((ByteBuffer) chiave.attachment());       //nome utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 20){                //rimozione di un membro da un progetto
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            prjName = new String(arr);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            int done;
                            if(projects.get(prjName).getMemberList().size() != 1){      //se non è l'unico membro del progetto
                                projects.get(prjName).removeMember(name);
                                if(projects.get(prjName).getRemainingRequests() == 0){      //era l'ultimo membro attivo (l'unico a non aver ancora inviato una richiesta di cancellazione)
                                    generatedIps.remove(projects.get(prjName).getChatAddress().getAddress().getHostAddress());  //rimozione dell'indirizzo di multicast in uso così che possa essere riutilizzato
                                    projects.remove(prjName);           //il progetto viene definitivamente eliminato dal sistema
                                    File pF = new File(prjFolder.getPath()+File.separator+prjName);
                                    File [] files = pF.listFiles();
                                    for(File f : files){
                                        f.delete();         //eliminazione di tutti i file di progetto
                                    }
                                    File cardsFolder = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards");
                                    if(cardsFolder.exists()){
                                        File [] cards = cardsFolder.listFiles();
                                        for(File fc : cards){
                                            fc.delete();        //eliminazione di tutti i file delle cards di progetto
                                        }
                                        cardsFolder.delete();   //eliminazione cartella delle cards
                                    }
                                    pF.delete();        //eliminazione cartella del progetto
                                    done = 2;
                                }
                                else{
                                    File members = new File(prjFolder.getPath()+File.separator+projects.get(prjName).getName()+File.separator+"members.json");
                                    obj.writeValue(members, projects.get(prjName).getMemberList());
                                    done = 1;
                                    //l'utente è stato rimosso
                                    regService.notifyMemberRemoval(name, projects.get(prjName).getMemberList(), prjName);   //callback ai membri del progetto rimasti (si aggiorna in live la lista dei membri)
                                    notification = "[WORTH Server]: "+name+" leaved this project";
                                    notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                    serverNotificator = new DatagramSocket();
                                    serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                    serverNotificator.close();
                                }
                            }
                            else done = 0;
                            sendBuffer.clear();
                            sendBuffer.putInt(-1);          //solito writeOp di restituzione del risultato
                            sendBuffer.putInt(done);
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_WRITE);
                        }
                        if(operation == 21){        //l'utente ha richiesto l'annullamento della richiesta di cancellazione mandata precedentemente
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            prjName = new String(arr);
                            receiveBuffer = (ByteBuffer) chiave.attachment();
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            Project p = projects.get(prjName);
                            if(p == null){      //il progetto è stato eliminato definitivamente
                                result = 1;
                                sendBuffer.clear();
                                sendBuffer.putInt(-1);
                                sendBuffer.putInt(result);
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                            else{
                                p.cancelDeleteRequest(name);        //richiesta di cancellazione annullata
                                File deletersFile = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"deleters.json");
                                obj.writeValue(deletersFile, p.getDeletersList());
                                notification = "[WORTH Server]: "+name+" canceled his/her delete request for this project";
                                notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prjName).getChatAddress());
                                serverNotificator = new DatagramSocket();
                                serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                serverNotificator.close();
                                result = 0;
                                sendBuffer.clear();
                                sendBuffer.putInt(-4);
                                sendBuffer.putInt(result);
                                outToClient = p.getChatAddress().getAddress().getHostAddress()+semiColon+p.getChatAddress().getPort();
                                sendBuffer.putInt(outToClient.getBytes().length);
                                sendBuffer.put(outToClient.getBytes());
                                sendBuffer.put(name.getBytes());
                                sendBuffer.flip();
                                chiave.attach(sendBuffer);
                                chiave.interestOps(SelectionKey.OP_WRITE);
                            }
                        }
                    }
                }
                else if(chiave.isWritable()){
                    SocketChannel client = (SocketChannel) chiave.channel();
                    ByteBuffer receiveBuffer;
                    ByteBuffer sendBuffer = ByteBuffer.allocateDirect(4096);
                    receiveBuffer = (ByteBuffer) chiave.attachment();
                    int writeOp = receiveBuffer.getInt();
                    if(writeOp == 0){           //deve mandare al client il risultato del controllo per il login
                        sendBuffer.clear();
                        sendBuffer.putInt(receiveBuffer.getInt());
                        sendBuffer.flip();
                        client.write(sendBuffer);               //manda l'output del controllo di login al client
                        sendBuffer.rewind();
                        loginCheck = sendBuffer.getInt();
                        if(loginCheck != 0){
                            client.close();
                            chiave.cancel();
                        }
                        else {
                            sendBuffer.clear();
                            arr = new byte[receiveBuffer.limit()-8];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    if(writeOp == -1){      //deve restituire al client il risultato del cotrollo per la visualizzazione di un progetto o creazione o spostamento di una card o cancellazione di un progetto o modifica della password o aggiunta di un nuovo membro di progetto o rimozione membro
                        sendBuffer.clear();
                        sendBuffer.putInt(receiveBuffer.getInt());      //il risultato da inviare
                        sendBuffer.flip();
                        client.write(sendBuffer);
                        sendBuffer.clear();          //ripristino dello username che deve rimanere sempre nell'attachment
                        sendBuffer.put(receiveBuffer);
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);
                        chiave.interestOps(SelectionKey.OP_READ);
                    }
                    if(writeOp == -2){          //deve inviare al client la lunghezza della lista dei progetti di cui l'utente è membro
                        arr = new byte[receiveBuffer.remaining()];
                        receiveBuffer.get(arr);
                        name = new String(arr);
                        ArrayList<String> projs = new ArrayList<>();
                        Enumeration<Project> e = projects.elements();
                        Project p;
                        while(e.hasMoreElements()){
                            p = e.nextElement();
                            if(p.checkMember(name)) projs.add(p.getName());     //controlla di quali progetti è membro l'utente che ha richiesto la lista
                        }
                        projs.trimToSize();
                        ByteBuffer b = ByteBuffer.wrap(obj.writeValueAsBytes(projs));   //scrittura nel buffer della serializzazione json della lista di progetti, da spedire in rete al client
                        sendBuffer.clear();
                        sendBuffer.putInt(b.remaining());
                        sendBuffer.flip();
                        client.write(sendBuffer);       //invia al client la lunghezza della lista di progetti di cui l'utente è membro
                        sendBuffer = ByteBuffer.allocateDirect(1024+b.limit());
                        sendBuffer.clear();             //preparazione dell'attachment per la prossima operazione di scrittura al client (writeOp -9)
                        sendBuffer.putInt(-9);
                        sendBuffer.putInt(b.limit());
                        sendBuffer.put(b);
                        name = semiColon+name;
                        sendBuffer.put(name.getBytes());
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);          //informazioni salvate nell'attachment per la prossima operazione di scrittura in rete (writeOp 9)
                        chiave.interestOps(SelectionKey.OP_READ);       //verrà eseguito il ramo readable con operation == 16
                    }
                    if(writeOp == -3){      //deve restituire al client il risultato della cancellazione dell'account dell'utente
                        sendBuffer.clear();
                        int r = receiveBuffer.getInt();     //risultato da inviare
                        sendBuffer.putInt(r);
                        sendBuffer.flip();
                        client.write(sendBuffer);
                         if(r == 0){
                             client.close();     //la cancellazione è stata eseguita e la connessione viene chiusa
                             chiave.cancel();
                        }
                        else{
                            sendBuffer.clear();
                            sendBuffer.put(receiveBuffer);
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    if(writeOp == -4){      //risposta alla creazione avvenuta di un progetto o all'annullamento della richiesta di cancellazione (il server invia l'indirizzo di chat)
                        sendBuffer.clear();
                        sendBuffer.putInt(receiveBuffer.getInt());      //risultato della creazione
                        int size = receiveBuffer.getInt();
                        arr = new byte[size];
                        receiveBuffer.get(arr);
                        sendBuffer.put(arr);        //indirizzo di multicast per la chat del nuovo progetto
                        sendBuffer.flip();
                        client.write(sendBuffer);
                        sendBuffer.clear();
                        sendBuffer.put(receiveBuffer);    //ripristino dell'attachment con i byte rimanenti nel receiveBuffer (username)
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);
                        chiave.interestOps(SelectionKey.OP_READ);
                    }
                    if(writeOp == -5){      //invia informazioni relative ad una card
                        sendBuffer.clear();
                        int size = receiveBuffer.getInt();
                        arr = new byte[size];
                        receiveBuffer.get(arr);
                        outToClient = new String(arr);
                        prjName = outToClient.substring(0, outToClient.indexOf(semiColon));
                        cardName = outToClient.substring(outToClient.lastIndexOf(semiColon)+1);
                        Card gotCard = projects.get(prjName).getCard(cardName);
                        if(gotCard == null){
                            sendBuffer.putInt(1);
                            sendBuffer.flip();
                            client.write(sendBuffer);       //segnala al client che la card non esiste nel progetto
                            sendBuffer.clear();
                            sendBuffer.put(receiveBuffer);      //nome utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_READ);
                        }
                        else{
                            sendBuffer.putInt(0);
                            outToClient = gotCard.getCreator()+semiColon+gotCard.getCurrentList()+semiColon+gotCard.getDescription();
                            sendBuffer.putInt(outToClient.getBytes().length);
                            sendBuffer.put(outToClient.getBytes());
                            sendBuffer.flip();
                            client.write(sendBuffer);
                            sendBuffer.clear();
                            sendBuffer.put(receiveBuffer);      //nome utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    if(writeOp == -6){          //il server deve inviare al client la lunghezza della lista delle cards di un progetto
                        int size = receiveBuffer.getInt();
                        arr = new byte[size];
                        receiveBuffer.get(arr);
                        prjName = new String(arr);
                        ByteBuffer b = ByteBuffer.wrap(obj.writeValueAsBytes(projects.get(prjName).getCardList()));   //scrittura nel buffer della serializzazione json della lista delle cards, da spedire in rete al client successivamente
                        sendBuffer.clear();
                        sendBuffer.putInt(b.remaining());
                        sendBuffer.flip();
                        client.write(sendBuffer);       //il server invia al client la lunghezza della lista delle cards di progetto
                        sendBuffer = ByteBuffer.allocateDirect(1024+b.limit());
                        sendBuffer.clear();         //preparazione dell'attachment
                        sendBuffer.putInt(-9);     //nuovo writeOp, in cui il server invia al client la lista delle cards di progetto
                        sendBuffer.putInt(b.limit());
                        sendBuffer.put(b);
                        arr = new byte[receiveBuffer.remaining()];
                        receiveBuffer.get(arr);
                        name = new String(arr);
                        name = semiColon+name;
                        sendBuffer.put(name.getBytes());      //nome utente
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);      //salva tutto nell'attachment per la prossima operazione di scrittura sul canale (writeOp 9)
                        chiave.interestOps(SelectionKey.OP_READ);       //verrà eseguito il ramo readable con operation == 16
                    }
                    if(writeOp == -7){          //il server invia al client la lunghezza della storia di una card, se questa esiste
                        int size = receiveBuffer.getInt();
                        arr = new byte[size];
                        receiveBuffer.get(arr);
                        inFromClient = new String(arr);
                        prjName = inFromClient.substring(0, inFromClient.indexOf(semiColon));
                        cardName = inFromClient.substring(inFromClient.lastIndexOf(semiColon)+1);
                        if(projects.get(prjName).getCard(cardName) == null){    //non esiste una carta con nome "cardName" nel progetto "prjName"
                            sendBuffer.clear();
                            sendBuffer.putInt(1);       //segnala al client che la card che cerca non esiste
                            sendBuffer.flip();
                            client.write(sendBuffer);
                            sendBuffer.clear();
                            sendBuffer.put(receiveBuffer);
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_READ);
                        }
                        else{
                            File cardFile = new File(prjFolder.getPath()+File.separator+prjName+File.separator+"cards"+File.separator+cardName+".json");
                            ArrayList<String> his = obj.readValue(cardFile, new TypeReference<ArrayList<String>>(){});
                            String first = his.remove(0);
                            his.add(0, first.substring(0, first.lastIndexOf(semiColon)));
                            ByteBuffer b = ByteBuffer.wrap(obj.writeValueAsBytes(his));     //serializzazione della storia della card da inviare
                            sendBuffer.clear();
                            sendBuffer.putInt(0);       //la card esiste
                            sendBuffer.putInt(b.remaining());   //invia la lunghezza della storia che verrà inviata subito dopo
                            sendBuffer.flip();
                            client.write(sendBuffer);
                            sendBuffer = ByteBuffer.allocateDirect(1024+b.limit());
                            sendBuffer.clear();
                            sendBuffer.putInt(-9);
                            sendBuffer.putInt(b.limit());
                            sendBuffer.put(b);
                            arr = new byte[receiveBuffer.remaining()];
                            receiveBuffer.get(arr);
                            name = new String(arr);
                            name = semiColon+name;
                            sendBuffer.put(name.getBytes());      //nome utente
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);      //salva tutto nell'attachment per la prossima operazione di scrittura sul canale (writeOp 9)
                            chiave.interestOps(SelectionKey.OP_READ);       //la card esiste quindi verrà eseguito il ramo readable con operation == 16
                        }
                    }
                    if(writeOp == -8){          //il server invia al client la lunghezza della lista di riferimenti alle chat dei progetti di cui è membro (dopo il login)
                        arr = new byte[receiveBuffer.remaining()];
                        receiveBuffer.get(arr);
                        name = new String(arr);
                        Hashtable<String,InetSocketAddress> ips = new Hashtable<>();
                        Enumeration<Project> e = projects.elements();
                        Project p;
                        while(e.hasMoreElements()){
                            p = e.nextElement();
                            if(p.checkActiveMember(name)) ips.put(p.getName(), p.getChatAddress());     //controlla di quali progetti è membro l'utente (di quali può ricevere i messaggi, cioè quelli per cui non ha mandato deletion requests)
                        }
                        ByteBuffer buff = ByteBuffer.wrap(obj.writeValueAsBytes(ips));
                        sendBuffer.clear();
                        sendBuffer.putInt(buff.remaining());
                        sendBuffer.flip();
                        client.write(sendBuffer);       //invia al client la lunghezza del buffer con gli indirizzi di multicast in modo che possa allocare un buffer di ricezione adeguatamente grande
                        sendBuffer = ByteBuffer.allocateDirect(1024+buff.limit());
                        sendBuffer.clear();         //preparazione dell'attachment
                        sendBuffer.putInt(-9);      //nuovo writeOp, in cui il server invia al client la lista degli indirizzi
                        sendBuffer.putInt(buff.limit());
                        sendBuffer.put(buff);
                        name = semiColon+name;
                        sendBuffer.put(name.getBytes());
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);      //salva tutto nell'attachment per la prossima operazione di scrittura sul canale (writeOp 9)
                        chiave.interestOps(SelectionKey.OP_READ);       //verrà eseguito il ramo readable con operation == 16
                    }
                    if(writeOp == -9){      //il server invia al client la lista serializzata nell'attachment (lista di indirizzi delle chat, o lista di progetti, o lista di cards)
                        int size = receiveBuffer.getInt();
                        sendBuffer = ByteBuffer.allocateDirect(size);
                        arr = new byte[size];
                        receiveBuffer.get(arr);
                        sendBuffer.clear();
                        sendBuffer.put(arr);
                        sendBuffer.flip();
                        client.write(sendBuffer);       //invio della lista al client
                        arr = new byte[receiveBuffer.remaining()];
                        receiveBuffer.get(arr);
                        name = new String(arr);
                        name = name.substring(name.lastIndexOf(semiColon)+1);
                        sendBuffer = ByteBuffer.allocateDirect(name.getBytes().length);
                        sendBuffer.clear();
                        sendBuffer.put(name.getBytes());        //ripristino del nome nell'attachment
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);      //ripristino dell'attachment
                        chiave.interestOps(SelectionKey.OP_READ);
                    }
                    if(writeOp == -10){
                        //il server prepara la lista serializzata di InetSocketAddress di progetti a cui l'utente è stato aggiunto come nuovo membro, da inviare al client alla prossima scrittura, e ne invia adesso la lunghezza così che quest'ultimo possa allocare un giusto buffer di ricezione
                        arr = new byte[receiveBuffer.remaining()];
                        receiveBuffer.get(arr);
                        name = new String(arr);
                        Hashtable<String,InetSocketAddress> ips = new Hashtable<>();
                        ArrayList<String> prjs = noviceMembers.get(name);
                        Project pr;
                        if(prjs != null){               //ci sono progetti in attesa di sincronizzazione per questo utente
                            for(String prName : prjs){
                                pr = projects.get(prName);
                                if(pr != null){
                                	ips.put(prName, pr.getChatAddress());
                                	//aggiunge l'utente come membro effettivo conferendogli tutti i diritti
                                	pr.addMember(name);
                                	File members = new File(prjFolder.getPath()+File.separator+prName+File.separator+"members.json");
                                	obj.writeValue(members, pr.getMemberList());
                                	regService.notifyNewMember(name, pr.getMemberList(), prName);       //notifica callback ai membri che il nuovo membro è stato aggiunto (si aggiorna live la lista dei membri)
                                	notification = "[WORTH Server]: "+name+" joined project";
                                	notificationPacket = new DatagramPacket(notification.getBytes(), notification.getBytes().length, projects.get(prName).getChatAddress());
                                	serverNotificator = new DatagramSocket();
                                	serverNotificator.send(notificationPacket);         //invio della notifica da parte del server ai membri del progetto
                                	serverNotificator.close();
                                }
                            }
                            ByteBuffer buff = ByteBuffer.wrap(obj.writeValueAsBytes(ips));
                            noviceMembers.remove(name);
                            obj.writeValue(noviceMembersFile, noviceMembers);
                            sendBuffer.clear();
                            sendBuffer.putInt(buff.remaining());
                            sendBuffer.flip();
                            client.write(sendBuffer);       //invia al client la lunghezza
                            sendBuffer = ByteBuffer.allocateDirect(1024+buff.limit());
                            sendBuffer.clear();         //preparazione dell'attachment
                            sendBuffer.putInt(-9);      //nuovo writeOp, in cui il server invia al client la lista degli indirizzi
                            sendBuffer.putInt(buff.limit());
                            sendBuffer.put(buff);
                            name = semiColon+name;
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);      //salva tutto nell'attachment per la prossima operazione di scrittura sul canale (writeOp 9)
                            chiave.interestOps(SelectionKey.OP_READ);       //verrà eseguito il ramo readable con operation == 16
                        }
                        else{       //non ci sono progetti in attesa di essere sincronizzati
                            sendBuffer.clear();
                            sendBuffer.putInt(0);       //la lunghezza della lista è zero perché non esiste
                            sendBuffer.flip();
                            client.write(sendBuffer);
                            sendBuffer.clear();
                            sendBuffer.put(name.getBytes());
                            sendBuffer.flip();
                            chiave.attach(sendBuffer);
                            chiave.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    if(writeOp == -11){     //il server prepara la lista di membri di un progetto e ne invia la lunghezza al client
                        int size = receiveBuffer.getInt();
                        arr = new byte[size];
                        receiveBuffer.get(arr);
                        prjName = new String(arr);
                        ArrayList<String> m = projects.get(prjName).getMemberList();
                        ByteBuffer buff = ByteBuffer.wrap(obj.writeValueAsBytes(m));
                        sendBuffer.clear();
                        sendBuffer.putInt(buff.remaining());
                        sendBuffer.flip();
                        client.write(sendBuffer);           //invia la lunghezza al client
                        sendBuffer = ByteBuffer.allocateDirect(1024+buff.limit());
                        sendBuffer.clear();
                        sendBuffer.putInt(-9);      //nuovo writeOp, in cui il server invia al client la lista di membri
                        sendBuffer.putInt(buff.limit());
                        sendBuffer.put(buff);
                        arr = new byte[receiveBuffer.remaining()];
                        receiveBuffer.get(arr);
                        name = new String(arr);
                        name = semiColon+name;
                        sendBuffer.put(name.getBytes());      //nome utente
                        sendBuffer.put(receiveBuffer);
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);
                        chiave.interestOps(SelectionKey.OP_READ);
                    }
                    if(writeOp == -12){     //il server invia al client il numero delle richieste rimanenti per la cancellazione del progetto che l'utente vorrebbe visualizzare
                        sendBuffer.clear();
                        sendBuffer.putInt(receiveBuffer.getInt());      //il risultato da inviare
                        sendBuffer.putInt(receiveBuffer.getInt());      //il numero delle richieste rimanenti
                        sendBuffer.flip();
                        client.write(sendBuffer);
                        sendBuffer.clear();          //ripristino dello username che deve rimanere sempre nell'attachment
                        sendBuffer.put(receiveBuffer);
                        sendBuffer.flip();
                        chiave.attach(sendBuffer);
                        chiave.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
            
        }
    }
        catch(Exception e){
            JFrame exception = new JFrame("WORTH Server Error!");
            exception.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            String exceptionMessage = e.getMessage()+"\n\n";
            for(StackTraceElement el : e.getStackTrace()){
                exceptionMessage = exceptionMessage.concat(el.toString());
                exceptionMessage = exceptionMessage+"\n";
            }
            JTextArea area = new JTextArea(exceptionMessage);
            area.setEditable(false);
            JScrollPane pane = new JScrollPane(area);
            JLabel label1 = new JLabel("Server reported error:");
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
