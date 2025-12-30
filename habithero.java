import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.text.SimpleDateFormat;
import java.util.*;  // This imports java.util.List
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// GLOBAL CONSTANTS
class Constants {
    public static final String DATA_FOLDER = "HabitHeroData/";
    public static final String EXCEL_FILE = DATA_FOLDER + "HabitHero.xlsx";
}

// USER MODEL
class User {
    private int id;
    private String username;
    private String hashedPassword;

    public User(int id, String username, String hashedPassword) {
        this.id = id;
        this.username = username;
        this.hashedPassword = hashedPassword;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getHashedPassword() { return hashedPassword; }
}

// HABIT MODEL
class Habit {
    private int habitId;
    private int userId;
    private String name;
    private String description;
    private LocalDate createdDate;
    private int streak;
    private LocalDate lastCompleted;

    public Habit(int habitId, int userId, String name, String description) {
        this.habitId = habitId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.createdDate = LocalDate.now();
        this.streak = 0;
        this.lastCompleted = null;
    }

    public int getHabitId() { return habitId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getCreatedDate() { return createdDate; }
    public int getStreak() { return streak; }
    public LocalDate getLastCompleted() { return lastCompleted; }

    public void setStreak(int streak) { this.streak = streak; }
    public void setLastCompleted(LocalDate date) { this.lastCompleted = date; }
}

// PASSWORD HASHING
class PasswordUtil {
    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return password;
        }
    }
}

// FILE STORAGE (Using text files instead of Excel)
class FileStorage {
    private static final String USERS_FILE = "users.dat";
    private static final String HABITS_FILE = "habits.dat";
    
    // Save users to file
    public static void saveUsers(Map<String, User> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Load users from file
    @SuppressWarnings("unchecked")
    public static Map<String, User> loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return new HashMap<String, User>();
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
            return (Map<String, User>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<String, User>();
        }
    }
    
    // Save habits to file
    public static void saveHabits(Map<Integer, Habit> habits) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HABITS_FILE))) {
            oos.writeObject(habits);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Load habits from file
    @SuppressWarnings("unchecked")
    public static Map<Integer, Habit> loadHabits() {
        File file = new File(HABITS_FILE);
        if (!file.exists()) return new HashMap<Integer, Habit>();
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HABITS_FILE))) {
            return (Map<Integer, Habit>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<Integer, Habit>();
        }
    }
}

// DATABASE MANAGER
class DatabaseManager {
    private Map<String, User> users;
    private Map<Integer, Habit> habits;
    private int nextUserId = 1;
    private int nextHabitId = 1;
    
    private static DatabaseManager instance;
    
    private DatabaseManager() {
        loadData();
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    // Load all data
    private void loadData() {
        users = FileStorage.loadUsers();
        habits = FileStorage.loadHabits();
        
        // Find max IDs
        for (User user : users.values()) {
            if (user.getId() >= nextUserId) nextUserId = user.getId() + 1;
        }
        for (Habit habit : habits.values()) {
            if (habit.getHabitId() >= nextHabitId) nextHabitId = habit.getHabitId() + 1;
        }
    }
    
    // Save all data
    private void saveData() {
        FileStorage.saveUsers(users);
        FileStorage.saveHabits(habits);
    }
    
    // Register user
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }
        
        User user = new User(nextUserId++, username, PasswordUtil.hash(password));
        users.put(username, user);
        saveData();
        return true;
    }
    
    // Login user
    public User loginUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getHashedPassword().equals(PasswordUtil.hash(password))) {
            return user;
        }
        return null;
    }
    
    // Add habit
    public Habit addHabit(int userId, String name, String description) {
        Habit habit = new Habit(nextHabitId++, userId, name, description);
        habits.put(habit.getHabitId(), habit);
        saveData();
        return habit;
    }
    
    // Get user habits
    public java.util.List<Habit> getUserHabits(int userId) {
        java.util.List<Habit> userHabits = new ArrayList<Habit>();
        for (Habit habit : habits.values()) {
            if (habit.getUserId() == userId) {
                userHabits.add(habit);
            }
        }
        return userHabits;
    }
    
    // Complete habit
    public boolean completeHabit(int habitId) {
        Habit habit = habits.get(habitId);
        if (habit == null) return false;
        
        LocalDate today = LocalDate.now();
        LocalDate last = habit.getLastCompleted();
        
        if (last != null && last.equals(today)) {
            return false; // Already completed today
        }
        
        // Update streak
        if (last != null && last.plusDays(1).equals(today)) {
            habit.setStreak(habit.getStreak() + 1);
        } else {
            habit.setStreak(1);
        }
        
        habit.setLastCompleted(today);
        saveData();
        return true;
    }
    
    // Delete habit
    public boolean deleteHabit(int habitId) {
        if (habits.containsKey(habitId)) {
            habits.remove(habitId);
            saveData();
            return true;
        }
        return false;
    }
    
    // Get user stats
    public Map<String, Integer> getUserStats(int userId) {
        java.util.List<Habit> userHabits = getUserHabits(userId);
        LocalDate today = LocalDate.now();
        
        int totalStreak = 0;
        int completedToday = 0;
        int totalHabits = userHabits.size();
        
        for (Habit habit : userHabits) {
            totalStreak += habit.getStreak();
            if (habit.getLastCompleted() != null && habit.getLastCompleted().equals(today)) {
                completedToday++;
            }
        }
        
        Map<String, Integer> stats = new HashMap<String, Integer>();
        stats.put("totalHabits", totalHabits);
        stats.put("totalStreak", totalStreak);
        stats.put("completedToday", completedToday);
        stats.put("successRate", totalHabits > 0 ? (completedToday * 100) / totalHabits : 0);
        
        return stats;
    }
}

// SPLASH SCREEN
class SplashScreen extends JWindow {
    public SplashScreen() {
        setSize(500, 300);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(41, 128, 185));
        
        JLabel title = new JLabel("HABITHERO", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(80, 0, 20, 0));
        
        JLabel tagline = new JLabel("Build Better Habits. Become Your Hero.", SwingConstants.CENTER);
        tagline.setFont(new Font("Arial", Font.ITALIC, 18));
        tagline.setForeground(Color.WHITE);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 0, 80, 0));
        
        panel.add(title, BorderLayout.CENTER);
        panel.add(tagline, BorderLayout.SOUTH);
        
        add(panel);
        setVisible(true);
        
        javax.swing.Timer timer = new javax.swing.Timer(2000, e -> {
            dispose();
            new LoginUI().setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }
}

// LOGIN UI
class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private DatabaseManager db;
    
    public LoginUI() {
        db = DatabaseManager.getInstance();
        
        setTitle("HabitHero - Login");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title
        JLabel title = new JLabel("Welcome Back Hero!");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(41, 128, 185));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 30, 0);
        panel.add(title, gbc);
        
        // Username
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(new JLabel("Username:"), gbc);
        
        usernameField = new JTextField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(new JLabel("Password:"), gbc);
        
        passwordField = new JPasswordField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        panel.add(passwordField, gbc);
        
        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(errorLabel, gbc);
        
        // Login Button
        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(41, 128, 185));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        gbc.gridy = 4; gbc.insets = new Insets(20, 0, 10, 0);
        panel.add(loginBtn, gbc);
        
        // Register Link
        JLabel registerLink = new JLabel("Don't have an account? Register here");
        registerLink.setForeground(Color.BLUE);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = 5; gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(registerLink, gbc);
        
        // Action Listeners
        loginBtn.addActionListener(e -> login());
        
        registerLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                dispose();
                new RegisterUI().setVisible(true);
            }
        });
        
        add(panel);
    }
    
    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill all fields!");
            return;
        }
        
        User user = db.loginUser(username, password);
        if (user != null) {
            dispose();
            new DashboardUI(user).setVisible(true);
        } else {
            errorLabel.setText("Invalid username or password!");
        }
    }
}

// REGISTER UI
class RegisterUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JLabel errorLabel;
    private DatabaseManager db;
    
    public RegisterUI() {
        db = DatabaseManager.getInstance();
        
        setTitle("HabitHero - Register");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title
        JLabel title = new JLabel("Join HabitHero!");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(41, 128, 185));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 30, 0);
        panel.add(title, gbc);
        
        // Username
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(new JLabel("Username:"), gbc);
        
        usernameField = new JTextField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(new JLabel("Password:"), gbc);
        
        passwordField = new JPasswordField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        panel.add(passwordField, gbc);
        
        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 3; gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(new JLabel("Confirm:"), gbc);
        
        confirmField = new JPasswordField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        panel.add(confirmField, gbc);
        
        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(errorLabel, gbc);
        
        // Register Button
        JButton registerBtn = new JButton("Register");
        registerBtn.setBackground(new Color(46, 204, 113));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        gbc.gridy = 5; gbc.insets = new Insets(20, 0, 10, 0);
        panel.add(registerBtn, gbc);
        
        // Login Link
        JLabel loginLink = new JLabel("Already have an account? Login here");
        loginLink.setForeground(Color.BLUE);
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = 6; gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(loginLink, gbc);
        
        // Action Listeners
        registerBtn.addActionListener(e -> register());
        
        loginLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                dispose();
                new LoginUI().setVisible(true);
            }
        });
        
        add(panel);
    }
    
    private void register() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            errorLabel.setText("Please fill all fields!");
            return;
        }
        
        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords don't match!");
            return;
        }
        
        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters!");
            return;
        }
        
        if (db.registerUser(username, password)) {
            JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
            dispose();
            new LoginUI().setVisible(true);
        } else {
            errorLabel.setText("Username already exists!");
        }
    }
}

// DASHBOARD UI
class DashboardUI extends JFrame {
    private User currentUser;
    private DatabaseManager db;
    private JPanel habitsPanel;
    private JLabel statsLabel;
    
    // Motivational quotes
    private String[] quotes = {
        "The secret of getting ahead is getting started.",
        "Small daily improvements lead to stunning results.",
        "Your future self will thank you for today's effort.",
        "Consistency is the key to achieving greatness."
    };
    
    public DashboardUI(User user) {
        this.currentUser = user;
        db = DatabaseManager.getInstance();
        
        setTitle("HabitHero Dashboard - " + user.getUsername());
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(41, 128, 185));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel welcomeLabel = new JLabel("Welcome, " + user.getUsername() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.WHITE);
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setForeground(new Color(41, 128, 185));
        
        header.add(welcomeLabel, BorderLayout.WEST);
        header.add(logoutBtn, BorderLayout.EAST);
        
        // Content Panel
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(240, 248, 255));
        
        // Motivational Quote
        Random rand = new Random();
        JLabel quoteLabel = new JLabel("<html><i>\"" + quotes[rand.nextInt(quotes.length)] + "\"</i></html>");
        quoteLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        quoteLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        quoteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Stats
        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        updateStats();
        
        // Add Habit Form
        JPanel addHabitPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        addHabitPanel.setBackground(Color.WHITE);
        addHabitPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextField habitNameField = new JTextField(15);
        JTextField habitDescField = new JTextField(15);
        JButton addHabitBtn = new JButton("Add Habit");
        addHabitBtn.setBackground(new Color(46, 204, 113));
        addHabitBtn.setForeground(Color.WHITE);
        
        addHabitPanel.add(new JLabel("Habit:"));
        addHabitPanel.add(habitNameField);
        addHabitPanel.add(new JLabel("Description:"));
        addHabitPanel.add(habitDescField);
        addHabitPanel.add(addHabitBtn);
        
        // Habits Panel
        habitsPanel = new JPanel();
        habitsPanel.setLayout(new BoxLayout(habitsPanel, BoxLayout.Y_AXIS));
        habitsPanel.setBackground(new Color(240, 248, 255));
        loadHabits();
        
        JScrollPane scrollPane = new JScrollPane(habitsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Add components
        content.add(quoteLabel);
        content.add(statsLabel);
        content.add(addHabitPanel);
        content.add(new JSeparator());
        content.add(scrollPane);
        
        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(content, BorderLayout.CENTER);
        
        // Action Listeners
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginUI().setVisible(true);
        });
        
        addHabitBtn.addActionListener(e -> {
            String name = habitNameField.getText().trim();
            String desc = habitDescField.getText().trim();
            
            if (!name.isEmpty()) {
                db.addHabit(currentUser.getId(), name, desc);
                habitNameField.setText("");
                habitDescField.setText("");
                loadHabits();
                updateStats();
                JOptionPane.showMessageDialog(this, "Habit added successfully!");
            }
        });
        
        add(mainPanel);
    }
    
    private void loadHabits() {
        habitsPanel.removeAll();
        
        java.util.List<Habit> habits = db.getUserHabits(currentUser.getId());
        
        if (habits.isEmpty()) {
            JLabel emptyLabel = new JLabel("No habits yet. Add your first habit!");
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            habitsPanel.add(emptyLabel);
        } else {
            for (Habit habit : habits) {
                habitsPanel.add(createHabitCard(habit));
                habitsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        
        habitsPanel.revalidate();
        habitsPanel.repaint();
    }
    
    private JPanel createHabitCard(Habit habit) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        // Habit info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        
        JLabel nameLabel = new JLabel(habit.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel descLabel = new JLabel(habit.getDescription());
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(Color.GRAY);
        
        JLabel dateLabel = new JLabel("Created: " + habit.getCreatedDate());
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        dateLabel.setForeground(Color.LIGHT_GRAY);
        
        infoPanel.add(nameLabel);
        infoPanel.add(descLabel);
        infoPanel.add(dateLabel);
        
        // Actions
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBackground(Color.WHITE);
        
        JLabel streakLabel = new JLabel("Streak: " + habit.getStreak() + " days");
        streakLabel.setFont(new Font("Arial", Font.BOLD, 12));
        streakLabel.setForeground(new Color(230, 126, 34));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton completeBtn = new JButton("Mark Complete");
        completeBtn.setBackground(new Color(46, 204, 113));
        completeBtn.setForeground(Color.WHITE);
        
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(231, 76, 60));
        deleteBtn.setForeground(Color.WHITE);
        
        buttonPanel.add(completeBtn);
        buttonPanel.add(deleteBtn);
        
        actionPanel.add(streakLabel);
        actionPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        actionPanel.add(buttonPanel);
        
        // Action listeners
        completeBtn.addActionListener(e -> {
            if (db.completeHabit(habit.getHabitId())) {
                loadHabits();
                updateStats();
                JOptionPane.showMessageDialog(this, "Great job! Habit marked as complete.");
            } else {
                JOptionPane.showMessageDialog(this, "Already completed today!");
            }
        });
        
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this habit?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (db.deleteHabit(habit.getHabitId())) {
                    loadHabits();
                    updateStats();
                    JOptionPane.showMessageDialog(this, "Habit deleted successfully!");
                }
            }
        });
        
        card.add(infoPanel, BorderLayout.WEST);
        card.add(actionPanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void updateStats() {
        Map<String, Integer> stats = db.getUserStats(currentUser.getId());
        
        String statsText = String.format(
            "<html><b>Stats:</b> Habits: %d | Total Streak: %d | Completed Today: %d | Success Rate: %d%%</html>",
            stats.get("totalHabits"),
            stats.get("totalStreak"),
            stats.get("completedToday"),
            stats.get("successRate")
        );
        
        statsLabel.setText(statsText);
    }
}

// MAIN CLASS
public class habithero {
    public static void main(String[] args) {
        // Start with splash screen
        SwingUtilities.invokeLater(() -> {
            new SplashScreen();
        });
    }
}