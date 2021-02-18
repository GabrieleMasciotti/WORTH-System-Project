import java.util.ArrayList;

public class Card {
    private final String cardName;
    private final String description;
    private final String project;
    private ArrayList<String> listsVisited;
    private final String createdBy;
    
    public Card(String name, String des, String prjName, String user){
        this.cardName = name;
        this.description = des;
        this.project = prjName;
        this.listsVisited = new ArrayList<>();
        this.createdBy = user;
    }
    
    public String getName(){
        return this.cardName;
    }
    
    public String getDescription(){
        return this.description;
    }
    
    public String getCurrentList(){
        return this.listsVisited.get(listsVisited.size()-1).substring(listsVisited.get(listsVisited.size()-1).indexOf(" ;")+2, listsVisited.get(listsVisited.size()-1).lastIndexOf(";"));
    }
    
    public ArrayList<String> getLists(){        //storia della card
        return this.listsVisited;
    }
    
    public void addVisitedList(String list){
        this.listsVisited.add(list);
    }
    
    public String getCreator(){
        return this.createdBy;
    }
    
    public void setPersistentVisitedLists(ArrayList<String> history){
        this.listsVisited = history;
    }
}
