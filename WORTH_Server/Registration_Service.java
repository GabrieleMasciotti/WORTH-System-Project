import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class Registration_Service extends RemoteServer implements Registration_Interface{
    private static final long serialVersionUID = 1L;
    private final ObjectMapper obj;
    private final Hashtable<String,BigInteger> usersDB;           //data base di utenti registrati nel sistema
    private final Hashtable<String,String> usersStates;       //data base di utenti con relativi stati (da inviare agli utenti in risposta alle registrazioni per callback)
    private final File usersFile = new File("registeredUsers.json");   //file json contenente la serializzazione del DB di utenti registrati
    private final Hashtable<String,Client_UserUpdates_Interface> clientsToBeNotified;      //lista di clienti da avvisare con callback
    private final MessageDigest hasher;

    public Registration_Service() throws IOException, NoSuchAlgorithmException {
        this.hasher = MessageDigest.getInstance("SHA-1");
        boolean created = usersFile.createNewFile();
        this.obj = new ObjectMapper();
        if(!created && usersFile.length()!=0){
            this.usersDB = obj.readValue(usersFile, new TypeReference<Hashtable<String,BigInteger>>(){});   //se è già presente un file contente record di iscrizione, recupera le informazioni da persistere
            Set<String> usernames = this.usersDB.keySet();
            this.usersStates = new Hashtable<>();
            for(String user : usernames){
                this.usersStates.put(user, "offline");      //recupera la lista di utenti registrati e inizializza il loro stato ad offline
            }
        }
        else {
            this.usersDB = new Hashtable<>();
            this.usersStates = new Hashtable<>();
        }
        this.clientsToBeNotified = new Hashtable<>();
    }

    @Override
    public synchronized int register(String nickUtente, String password) throws IOException {
        if(nickUtente.isBlank()){
            return 2;
        }
        if(password.isBlank()){
            return 3;
        }
        hasher.update(password.getBytes());         //si prepara per calcolare l'hash della password
        if(usersDB.putIfAbsent(nickUtente, new BigInteger(hasher.digest())) != null){      //username già in uso
            return 1;
        }
        obj.writeValue(usersFile,usersDB);
        usersStates.put(nickUtente, "offline");
        Collection<Client_UserUpdates_Interface> values = this.clientsToBeNotified.values();
        Iterator<Client_UserUpdates_Interface> iter = values.iterator();
        Client_UserUpdates_Interface client;
        while(iter.hasNext()) {        //notifica ai clients iscritti alle callbacks
            client = iter.next();
            try {
                client.notifyNewRegistration(nickUtente, "offline");
            } catch (RemoteException ex) {
                iter.remove();
            }
        }
        return 0;
    }

    @Override
    public synchronized Hashtable<String,String> registerForNotification(String user, Client_UserUpdates_Interface client) throws RemoteException {
        if(!this.clientsToBeNotified.containsKey(user)){
            this.clientsToBeNotified.put(user, client);           //registrazione di un nuovo cliente da avvisare con callbacks
        }
        return this.usersStates;
    }

    @Override
    public synchronized void unRegisterForNotification(String user) throws RemoteException {
        this.clientsToBeNotified.remove(user);           //rimozione di un client da avvertire con callbacks
    }
    
    public synchronized int checkUserLogin(String userName, String password) throws RemoteException{
        if(!this.usersDB.containsKey(userName)) return 1;           //errore: utente non iscritto al servizio
        if(!this.usersDB.get(userName).equals(new BigInteger(hasher.digest(password.getBytes())))) return 2;  //errore: password errata
        if(this.usersStates.get(userName).equals("online")) return 3;   //errore: l'account è già aperto in un'altra finestra (o dispositivo)
        this.usersStates.replace(userName, "online");
        this.notifyStateChange(userName, "online");
        return 0;       //il login può essere effettuato
    }
    
    public synchronized boolean changeUserPassword(String userName, String password, String newPassword) throws IOException{
        if(this.usersDB.get(userName).equals(new BigInteger(hasher.digest(password.getBytes())))){this.usersDB.replace(userName, new BigInteger(hasher.digest(password.getBytes())), new BigInteger(hasher.digest(newPassword.getBytes()))); obj.writeValue(usersFile,usersDB); return true;}
        else return false;
    }
    
    public synchronized int checkUserDelete(String userName, String password) throws IOException{
        if(!this.usersDB.get(userName).equals(new BigInteger(hasher.digest(password.getBytes())))) return 1;  //errore: password errata
        this.usersDB.remove(userName);
        obj.writeValue(usersFile, usersDB);
        this.usersStates.remove(userName);
        this.unRegisterForNotification(userName);
        Collection<Client_UserUpdates_Interface> values = this.clientsToBeNotified.values();
        Iterator<Client_UserUpdates_Interface> iter = values.iterator();
        Client_UserUpdates_Interface client;
        while(iter.hasNext()) {        //notifica agli utenti iscritti al servizio di callback la cancellazione
            client = iter.next();
            try {
                client.notifyAccountRemoval(userName);
            } catch (RemoteException ex) {
                iter.remove();
            }
        }
        return 0;
    }
    
    public synchronized void logOut(String userName) throws RemoteException{
        this.usersStates.replace(userName, "offline");
        this.notifyStateChange(userName, "offline");
    }
    
    public synchronized void logIn(String userName) throws RemoteException{
        this.usersStates.replace(userName, "online");
        this.notifyStateChange(userName, "online");
    }
    
    public synchronized void notifyStateChange(String name, String state) {
        Collection<Client_UserUpdates_Interface> values = this.clientsToBeNotified.values();
        Iterator<Client_UserUpdates_Interface> iter = values.iterator();
        Client_UserUpdates_Interface client;
        while(iter.hasNext()){
            client = iter.next();
            try {
                client.notifyStateChange(name, state);
            } catch (RemoteException ex) {          //connessione persa col client da notificare
                iter.remove();      //client rimosso dalla lista di avvisi callback
            }
        }
    }
    
    public synchronized boolean userExist(String userName){
        return this.usersDB.containsKey(userName);
    }
    
    public synchronized void notifyNewMember(String user, ArrayList<String> oldMembers, String prjName) throws RemoteException{
        for(String om : oldMembers){
            Client_UserUpdates_Interface client = this.clientsToBeNotified.get(om);
            if(client != null) {
                try {
                    client.notifyNewMember(user,prjName);
                } catch (RemoteException ex) {
                    this.clientsToBeNotified.remove(om);
                }
            }
        }
    }
    
    public synchronized void notifyMemberRemoval(String user, ArrayList<String> oldMembers, String prjName) throws RemoteException{
        for(String om : oldMembers){
            Client_UserUpdates_Interface client = this.clientsToBeNotified.get(om);
            if(client != null) {
                try {
                    client.notifyMemberRemoval(user, prjName);
                } catch (RemoteException ex) {
                    this.clientsToBeNotified.remove(om);
                }
            }
        }
    }
    
}
