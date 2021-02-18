public class waitUserAction {       //monitor per la gestione dell'attesa delle scelte utente nelle varie finestre della GUI
    
    private boolean userDone = false;
    
    public synchronized void waitUser() throws InterruptedException{
        while(!userDone){
            wait();
        }
        this.userDone = false;
    }
    
    public synchronized void setUserDone(){
        this.userDone = true;
        notifyAll();
    }

}
