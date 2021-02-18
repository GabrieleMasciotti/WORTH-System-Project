import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Project {
    private final String projectName;
    private ArrayList<String> members;
    private final ArrayList<Card> todo;
    private final ArrayList<Card> inProgress;
    private final ArrayList<Card> toBeRevised;
    private final ArrayList<Card> done;
    private InetSocketAddress chatAddress;
    private boolean inDeletion = false;
    private ArrayList<String> deleters;
    
    public Project(String pName){
        this.projectName = pName;
        this.members = new ArrayList<>();
        this.todo = new ArrayList<>();
        this.inProgress = new ArrayList<>();
        this.toBeRevised = new ArrayList<>();
        this.done = new ArrayList<>();
        this.deleters = new ArrayList<>();
    }
    
    public void setPersistentMemberList(ArrayList<String> mem){
        this.members = mem;
    }
    
    public void addPersistentCard(Card card, String list){
        if(list.equals("todo")) this.todo.add(card);
        if(list.equals("inProgress")) this.inProgress.add(card);
        if(list.equals("toBeRevised")) this.toBeRevised.add(card);
        if(list.equals("done")) this.done.add(card);
    }
    
    public void setPersistentDeletersList(ArrayList<String> dels){
        this.inDeletion = true;
        this.deleters = dels;
    }
    
    public void setChatAddress(InetSocketAddress address){
        this.chatAddress = address;
    }
    
    public InetSocketAddress getChatAddress(){
        return this.chatAddress;
    }
    
    public ArrayList<String> getMemberList(){
        return this.members;
    }
    
    public String getName(){
        return this.projectName;
    }
    
    public boolean checkMember(String userName){
        return this.members.contains(userName);
    }
    
    public boolean checkActiveMember(String userName){
        return members.contains(userName) && !deleters.contains(userName);
    }
    
    public void removeMember(String name){
        this.members.remove(name);
        this.deleters.remove(name);
        if(this.deleters.isEmpty()) this.inDeletion = false;
    }
    
    public void addMember(String userName){
        this.members.add(userName);
    }
    
    public int addCard(Card card){
        todo.add(card);
        return 0;
    }
    
    public Card getCard(String cardName){
        for(Card c : this.todo){
            if(c.getName().equals(cardName)) return c;
        }
        for(Card c : this.inProgress){
            if(c.getName().equals(cardName)) return c;
        }
        for(Card c : this.toBeRevised){
            if(c.getName().equals(cardName)) return c;
        }
        for(Card c : this.done){
            if(c.getName().equals(cardName)) return c;
        }
        return null;
    }
    
    public ArrayList<String> getCardList(){
        ArrayList<String> cardList = new ArrayList<>();
        for(Card c : this.todo) cardList.add(c.getName());
        for(Card c : this.inProgress) cardList.add(c.getName());
        for(Card c : this.toBeRevised) cardList.add(c.getName());
        for(Card c : this.done) cardList.add(c.getName());
        cardList.trimToSize();
        return cardList;
    }
    
    public int moveCard(String card, String destList){
        Card c = this.getCard(card);
        if(c == null) return 1;
        String cur = c.getCurrentList();
        if(cur.equals(destList)) return 2;
        if(cur.equals("todo") && destList.equals("toBeRevised")) return 2;
        if(cur.equals("todo") && destList.equals("done")) return 2;
        if(cur.equals("done")) return 2;
        //lo spostamento della card è consentito
        if(destList.equals("inProgress")) this.inProgress.add(c);
        if(destList.equals("toBeRevised")) this.toBeRevised.add(c);
        if(destList.equals("done")) this.done.add(c);
        if(cur.equals("todo")) this.todo.remove(c);
        if(cur.equals("inProgress")) this.inProgress.remove(c);
        if(cur.equals("toBeRevised")) this.toBeRevised.remove(c);
        return 0;
    }
    
    public int deleteCard(String card){
        Card c = this.getCard(card);
        if(c == null) return 1;
        if(this.isInDeletion()) return 2;
        this.todo.remove(c);
        this.inProgress.remove(c);
        this.toBeRevised.remove(c);
        this.done.remove(c);
        return 0;
    }
    
    public int deletable(){
        if(!todo.isEmpty()) return 1;
        if(!inProgress.isEmpty()) return 1;
        if(!toBeRevised.isEmpty()) return 1;
        return 0;
    }
    
    public boolean isInDeletion(){
        return this.inDeletion;
    }
    
    public boolean isDeleter(String user){
        return this.deleters.contains(user);
    }
    
    public ArrayList<String> getDeletersList(){
        return this.deleters;
    }
    
    public int getRemainingRequests(){
        return this.members.size()-this.deleters.size();
    }
    
    public int addDeleteRequest(String user){
        this.inDeletion = true;
        this.deleters.add(user);
        return this.members.size()-this.deleters.size();
    }
    
    public void cancelDeleteRequest(String user){
        this.deleters.remove(user);
        if(this.deleters.isEmpty()) this.inDeletion = false;
    }
    
}
