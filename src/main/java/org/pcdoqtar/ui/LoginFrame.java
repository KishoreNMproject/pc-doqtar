package org.pcdoqtar.ui;

import org.pcdoqtar.connection.FirebaseAuthClient;
import org.pcdoqtar.connection.FirebaseAuthClient.AuthSession;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

@SuppressWarnings("serial")
public class LoginFrame extends JFrame {
    private final JTextField email = new JTextField(20);
    private final JPasswordField pass = new JPasswordField(20);
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JButton login = new JButton("Login"), register = new JButton("Register");
    private final Consumer<AuthSession> onSuccess;

    public LoginFrame(Consumer<AuthSession> onSuccess) {
        super("PC DOQTAR - Sign in");
        this.onSuccess = onSuccess;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        JLabel t = new JLabel("PC DOQTAR", SwingConstants.CENTER);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 22f));
        JPanel b = new JPanel(new GridLayout(1, 2, 8, 0)); b.add(login); b.add(register);
        p.add(t); p.add(new JLabel("Email")); p.add(email);
        p.add(new JLabel("Password")); p.add(pass); p.add(b); p.add(status);
        add(p);
        getRootPane().setDefaultButton(login);
        login.addActionListener(e -> go(false));
        register.addActionListener(e -> go(true));
        pack(); setLocationRelativeTo(null);
    }

    private void go(boolean signUp) {
        String e = email.getText().trim(), pw = new String(pass.getPassword());
        if (e.isEmpty() || pw.isEmpty()) { msg("Enter email and password", true); return; }
        login.setEnabled(false); register.setEnabled(false); msg("Please wait...", false);
        new Thread(() -> {
            try {
                AuthSession s = signUp ? FirebaseAuthClient.signUp(e, pw) : FirebaseAuthClient.signIn(e, pw);
                SwingUtilities.invokeLater(() -> { dispose(); onSuccess.accept(s); });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    msg(ex.getMessage(), true); login.setEnabled(true); register.setEnabled(true);
                });
            }
        }).start();
    }

    private void msg(String m, boolean err) {
        status.setForeground(err ? Color.RED : new Color(0, 120, 0)); status.setText(m);
    }
}