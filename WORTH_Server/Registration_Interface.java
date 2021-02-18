import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Hashtable;

public interface Registration_Interface extends Remote{
    public int register(String nickUtente, String password) throws IOException;
    public Hashtable<String,String> registerForNotification(String user, Client_UserUpdates_Interface client) throws RemoteException;
    public void unRegisterForNotification(String user) throws RemoteException;
}
