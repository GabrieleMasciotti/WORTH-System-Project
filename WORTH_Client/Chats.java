import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

public class Chats {        //classe condivisa tra il thread main del client e il thread che si occupa di ricevere i messaggi delle chat di progetto
    private final Hashtable<String,InetSocketAddress> addresses;
    private final Hashtable<String,ArrayList<String>> chats;
    private final ArrayList<String> chatAdded;
    private final ArrayList<String> chatRemoved;
    
    public synchronized InetSocketAddress getPrAddress(String prjName){
        return this.addresses.get(prjName);
    }
    
    public synchronized void addChat(String prjName, InetSocketAddress group){
        this.chats.put(prjName, new ArrayList<>());
        this.addresses.put(prjName, group);
        this.chatAdded.add(prjName);
    }
    
    public synchronized ArrayList<String> getChatAdded(){
        ArrayList<String> added = new ArrayList<>();
        for(String p : this.chatAdded){
            added.add(p);
        }
        return added;
    }
    
    public synchronized void resetChatAdded(ArrayList<String> added){
        for(String p : added){
            this.chatAdded.remove(p);
        }
    }
    
    public synchronized ArrayList<String> getChatRemoved(){
        ArrayList<String> removed = new ArrayList<>();
        for(String p : this.chatRemoved){
            removed.add(p);
        }
        return removed;
    }
    
    public synchronized void resetChadRemoved(ArrayList<String> removed){
        for(String p : removed){
            this.chatRemoved.remove(p);
        }
    }
    
    public synchronized void removeChat(String prjName){
        this.addresses.remove(prjName);
        this.chats.remove(prjName);
        this.chatAdded.remove(prjName);
        this.chatRemoved.add(prjName);
    }
    
    public synchronized void removeDeletedMsgsSystemMessage(String prjName){
        this.chats.get(prjName).remove(0);
    }
    
    public synchronized ArrayList<String> getChatMessages(String prjName){
        ArrayList<String> msgs = new ArrayList<>();
        for(String m : this.chats.get(prjName)){
            msgs.add(m);
        }
        return msgs;
    }
    
    public synchronized void putNewMsg(String prjName, String msg){
        this.chats.get(prjName).add(msg);
    }
    
    public Chats(){
        this.chats = new Hashtable<>();
        this.chatAdded = new ArrayList<>();
        this.addresses = new Hashtable<>();
        this.chatRemoved = new ArrayList<>();
    }
    
}
