import java.awt.Color;
import java.io.File;
import javax.swing.ImageIcon;

public class Profile_Settings extends javax.swing.JFrame {

    private waitUserAction waitUser;
    private static final long serialVersionUID = 9L;
    private String userName;
    private int prjNum;
    private String state;
    public int operation;
    public String psw;
    public String newPassword;
    private String activeDisactive = "activated";
    
    public Profile_Settings() {
        initComponents();
    }
    public Profile_Settings(String user, int num, String state, waitUserAction wait, boolean notificationsState){
        this.userName = user;
        this.prjNum = num;
        this.state = state;
        this.waitUser = wait;
        if(notificationsState) this.activeDisactive = "activated";
        else this.activeDisactive = "deactivated";
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        profileTitleLabel = new javax.swing.JLabel();
        profileTitleLabel.setText("This is your personal profile, "+this.userName+".");
        jLabel1 = new javax.swing.JLabel();
        oldPasswordTextField = new javax.swing.JTextField();
        newPasswordTextField = new javax.swing.JTextField();
        changePswButton = new javax.swing.JButton();
        this.changePswButton.setIcon(new ImageIcon("icone"+File.separator+"password.png"));
        numberOfProjectsLabel = new javax.swing.JLabel();
        this.numberOfProjectsLabel.setText("Number of PROJECTS you are a member of:   "+this.prjNum+".");
        userNameLabel = new javax.swing.JLabel();
        this.userNameLabel.setText("Your USERNAME:  "+this.userName+".  (This can't be changed.)");
        stateLabel = new javax.swing.JLabel();
        this.stateLabel.setText("Your STATE in the system is:  "+this.state);
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        deleteAccountButton = new javax.swing.JButton();
        this.deleteAccountButton.setIcon(new ImageIcon("icone"+File.separator+"deleteaccount.png"));
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        backToHomeButton = new javax.swing.JButton();
        this.backToHomeButton.setIcon(new ImageIcon("icone"+File.separator+"back.png"));
        jLabel7 = new javax.swing.JLabel();
        ImageIcon img = new ImageIcon("icone"+File.separator+"semaf_v.gif");
        this.jLabel7.setIcon(img);
        jLabel8 = new javax.swing.JLabel();
        setNotificationButton = new javax.swing.JButton();
        this.setNotificationButton.setIcon(new ImageIcon("icone"+File.separator+"notSet.png"));
        jLabel9 = new javax.swing.JLabel();
        notStateLabel = new javax.swing.JLabel();
        if(activeDisactive.equals("activated")) this.notStateLabel.setForeground(Color.BLUE);
        else this.notStateLabel.setForeground(Color.RED);
        this.notStateLabel.setText(this.activeDisactive);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("WORTH Profile Settings");

        profileTitleLabel.setFont(new java.awt.Font("Ubuntu", 3, 20)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jLabel1.setText("Change your password:");

        oldPasswordTextField.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        oldPasswordTextField.setToolTipText("type your current password");

        newPasswordTextField.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        newPasswordTextField.setToolTipText("type your new password");

        changePswButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        changePswButton.setText(" Change Password");
        changePswButton.setToolTipText("press to change your password");
        changePswButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        changePswButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changePswButtonActionPerformed(evt);
            }
        });

        numberOfProjectsLabel.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N

        userNameLabel.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N

        stateLabel.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N

        jLabel2.setText("your current password");

        jLabel3.setText("your new password");

        jLabel4.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jLabel4.setText("Delete your account permanently:");

        jPasswordField1.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jPasswordField1.setToolTipText("type your password to delete the account");

        deleteAccountButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        deleteAccountButton.setText(" Delete Account");
        deleteAccountButton.setToolTipText("press to permanently delete this account");
        deleteAccountButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        deleteAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAccountButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("type your password");

        jLabel6.setFont(new java.awt.Font("Ubuntu", 3, 18)); // NOI18N
        jLabel6.setText(">> WORTH System <<");

        backToHomeButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        backToHomeButton.setText("   Back To Home Page");
        backToHomeButton.setToolTipText("press to go back to your home page");
        backToHomeButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        backToHomeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backToHomeButtonActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N

        jLabel8.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jLabel8.setText("Choose if you want to receive or not chats notifications:");

        setNotificationButton.setFont(new java.awt.Font("Ubuntu", 1, 16)); // NOI18N
        setNotificationButton.setText(" Able/Disable Notifications");
        setNotificationButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        setNotificationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setNotificationButtonActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jLabel9.setText("Your message notifications are:");

        notStateLabel.setFont(new java.awt.Font("Ubuntu", 1, 17)); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(344, 344, 344)
                        .addComponent(jLabel5))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(backToHomeButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(profileTitleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel6)
                        .addGap(132, 132, 132))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addComponent(jLabel2)
                                .addGap(60, 60, 60)
                                .addComponent(jLabel3))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(oldPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(changePswButton)))
                        .addContainerGap(9, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(40, 40, 40)
                                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(deleteAccountButton))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(numberOfProjectsLabel)
                                    .addComponent(userNameLabel)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(stateLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(110, 110, 110))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(33, 33, 33)
                                .addComponent(setNotificationButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addGap(28, 28, 28)
                                .addComponent(notStateLabel)))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(profileTitleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(48, 48, 48)
                        .addComponent(jLabel6)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(userNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addComponent(numberOfProjectsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(notStateLabel))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(setNotificationButton))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(oldPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(changePswButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(backToHomeButton)
                .addGap(14, 14, 14))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backToHomeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backToHomeButtonActionPerformed
        this.operation = 0;
        this.dispose();
        this.waitUser.setUserDone();
    }//GEN-LAST:event_backToHomeButtonActionPerformed

    private void deleteAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAccountButtonActionPerformed
        this.operation = 1;
        this.psw = new String(this.jPasswordField1.getPassword());
        this.dispose();
        this.waitUser.setUserDone();
    }//GEN-LAST:event_deleteAccountButtonActionPerformed

    private void changePswButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changePswButtonActionPerformed
        this.operation = 2;
        this.psw = this.oldPasswordTextField.getText();
        this.newPassword = this.newPasswordTextField.getText();
        this.waitUser.setUserDone();
    }//GEN-LAST:event_changePswButtonActionPerformed

    private void setNotificationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setNotificationButtonActionPerformed
        this.operation = 3;
        this.waitUser.setUserDone();
        if(activeDisactive.equals("activated")) {
            this.notStateLabel.setForeground(Color.RED);
            this.activeDisactive = "deactivated";
        }
        else {
            this.notStateLabel.setForeground(Color.BLUE);
            this.activeDisactive = "activated";
        }
        this.notStateLabel.setText(this.activeDisactive);
    }//GEN-LAST:event_setNotificationButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Profile_Settings.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Profile_Settings.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Profile_Settings.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Profile_Settings.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Profile_Settings().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backToHomeButton;
    private javax.swing.JButton changePswButton;
    private javax.swing.JButton deleteAccountButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JTextField newPasswordTextField;
    private javax.swing.JLabel notStateLabel;
    private javax.swing.JLabel numberOfProjectsLabel;
    private javax.swing.JTextField oldPasswordTextField;
    private javax.swing.JLabel profileTitleLabel;
    private javax.swing.JButton setNotificationButton;
    private javax.swing.JLabel stateLabel;
    private javax.swing.JLabel userNameLabel;
    // End of variables declaration//GEN-END:variables
}
