import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Client_UserUpdates_Interface extends Remote{
    public void notifyNewRegistration(String userName, String state) throws RemoteException;
    public void notifyStateChange(String userName, String state) throws RemoteException;
    public void notifyAccountRemoval(String userName) throws RemoteException;
    public void notifyNewMember(String userName, String prjName) throws RemoteException;
    public void notifyMemberRemoval(String userName, String prjName) throws RemoteException;
}
