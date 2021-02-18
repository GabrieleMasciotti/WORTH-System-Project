import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class UserUpdates_ROC extends RemoteObject implements Client_UserUpdates_Interface{
    private static final long serialVersionUID = 2L;
    private Hashtable<String,String> users;
    private Hashtable<String,String> projectMembers;
    private ChatFrame chatMemberStatus = null;
    
    public UserUpdates_ROC(){
        this.users = new Hashtable<>();         //struttura dati lato client per mantere traccia degli utenti del servizio WORTH
    }
    
    @Override
    public void notifyNewRegistration(String userName, String state) throws RemoteException {
        users.put(userName, state);     //notifica di una nuova registrazione avvenuta
    }

    @Override
    public void notifyStateChange(String userName, String state) throws RemoteException {
        users.replace(userName, state);     //notifica di cambio di stato di un utente
        if(projectMembers != null){
            if(projectMembers.containsKey(userName)) projectMembers.replace(userName, state);
            if(chatMemberStatus != null){       //sicuramente è non null anche la lista projectMembers
                chatMemberStatus.updateMemberStatus(projectMembers);
            }
        }
    }

    @Override
    public void notifyAccountRemoval(String userName) throws RemoteException {
        users.remove(userName);         //notifica di avvenuta rimozione di un account
        if(projectMembers != null){
            if(projectMembers.containsKey(userName)) projectMembers.remove(userName);
            if(chatMemberStatus != null){       //sicuramente è non null anche la lista projectMembers
                chatMemberStatus.updateMemberStatus(projectMembers);
            }
        }
    }
    
    public void setChatMemberStatusFrame(ChatFrame chat){
        this.chatMemberStatus = chat;
    }
    
    public void resetChatMemberStatusFrame(){
        this.chatMemberStatus = null;
    }
    
    public void setMemberStatusAtStart(){
        chatMemberStatus.updateMemberStatus(projectMembers);
    }
    
    public void setUsers(Hashtable<String,String> users){
        this.users = users;
    }
    
    public String listUsers(){
        Set<Entry<String,String>> set = this.users.entrySet();
        ArrayList<Entry<String,String>> arr = new ArrayList<>(set);
        return arr.toString();
    }
    
    public ArrayList<String> listOnlineUsers(){
        Set<Entry<String,String>> set = this.users.entrySet();
        ArrayList<String> output = new ArrayList<>();
        Iterator<Entry<String,String>> iter = set.iterator();
        Entry<String,String> entry;
        while(iter.hasNext()){
            entry = iter.next();
            if(entry.getValue().equals("online")){
                output.add(entry.getKey());
            }
        }
        return output;
    }
    
    public String checkUserState(String name){
        return this.users.get(name);
    }
    
    public void setProjectMembersList(ArrayList<String> members){
        this.projectMembers = new Hashtable<>();
        for(String user : members){
            this.projectMembers.put(user, this.users.get(user));
        }
    }
    
    public void resetProjectMemberList(){
        this.projectMembers = null;
    }
    
    public Hashtable<String,String> getMemberList(){
        return this.projectMembers;
    }

    @Override
    public void notifyNewMember(String userName, String prjName) throws RemoteException {       //un noviceMember è diventato un membro effettivo (aggiorna live la lista di membri)
        if(projectMembers != null){
            if(chatMemberStatus.projectName.equals(prjName)){       //se il nuovo membro è stato aggiunto al progetto di cui l'utente sta visualizzando la schermata chat and members
                projectMembers.put(userName, users.get(userName));
                if(chatMemberStatus != null){       //sicuramente è non null anche la lista projectMembers
                    chatMemberStatus.updateMemberStatus(projectMembers);
                }
            }
        }
    }

    @Override
    public void notifyMemberRemoval(String userName, String prjName) throws RemoteException {
        if(projectMembers != null){
            if(chatMemberStatus.projectName.equals(prjName)){       //il membro ha lasciato il progetto di cui l'utente sta visualizzando la finestra della chat
                projectMembers.remove(userName);
                if(chatMemberStatus != null) chatMemberStatus.updateMemberStatus(projectMembers);
            }
        }
    }
    
}
