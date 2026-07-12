package app;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.Calendar;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JComboBox;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.CategoryPlot;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class ConstructionDataManagementSystem {

	String userRole;
	String loggedUsername;
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JLabel lblTotalLabour;
    private JLabel lblTotalCash;


    // Table models and tables
    private DefaultTableModel projectTableModel, labourTableModel, materialTableModel, quotationTableModel, paymentTableModel, runningBillTableModel;
    private JTable projectTable, labourTable, materialTable, quotationTable, paymentTable, runningBillTable;

    // MySQL connection info
    private final String DB_URL = "jdbc:mysql://localhost:3306/";
    private final String DB_NAME = "construction_db";
    private final String USER = "root";
    private final String PASS = "mmpc2405";

    private Connection conn;

    // For Payment page project dropdown
    private JComboBox<String> cmbProjectDropdown;
    private Vector<Integer> projectIds;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new LoginFrame();
            }
        });
    }


    public ConstructionDataManagementSystem() {
        connectDatabase();
        createTablesIfNotExist();
    }

    private void connectDatabase() {
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            conn.close();

            conn = DriverManager.getConnection(DB_URL + DB_NAME, USER, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database connection failed!");
        }
    }

    
    private void createTablesIfNotExist() {
        try {
            Statement stmt = conn.createStatement();

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS projects (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100)," +
                    "location VARCHAR(100)," +
                    "start_date DATE," +
                    "end_date DATE)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS labour (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100)," +
                    "role VARCHAR(50)," +
                    "date DATE," +
                    "status ENUM('P','A') DEFAULT 'A'," +
                    "hours INT," +
                    "wage INT)");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS material (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100)," +
                    "qty VARCHAR(50)," +
                    "supplier VARCHAR(100)," +
                    "supplier_mobile VARCHAR(20)," +
                    "invoice_no VARCHAR(50)," +
                    "invoice_date DATE," +
                    "delivery_date DATE)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS quotation (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "client VARCHAR(100)," +
                    "description VARCHAR(255)," +
                    "amount VARCHAR(20)," +
                    "date DATE)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS payments (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "project_id INT," +
                    "type ENUM('received','paid') NOT NULL," +
                    "category VARCHAR(100)," +
                    "description VARCHAR(255)," +
                    "amount DECIMAL(10,2) NOT NULL," +
                    "date DATE NOT NULL," +
                    "client_or_vendor VARCHAR(100)," +
                    "FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL)");

            stmt.executeUpdate(
                    "CREATE OR REPLACE VIEW running_bill_view AS " +
                    "SELECT p.name AS project_name, " +
                    "IFNULL(SUM(CASE WHEN pm.type='received' THEN pm.amount ELSE 0 END),0) AS total_received, " +
                    "IFNULL(SUM(CASE WHEN pm.type='paid' THEN pm.amount ELSE 0 END),0) AS total_paid, " +
                    "IFNULL(SUM(CASE WHEN pm.type='received' THEN pm.amount ELSE 0 END),0) - IFNULL(SUM(CASE WHEN pm.type='paid' THEN pm.amount ELSE 0 END),0) AS running_bill " +
                    "FROM projects p LEFT JOIN payments pm ON p.id = pm.project_id " +
                    "GROUP BY p.id"
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createUI() {
        frame = new JFrame("Construction Data Management System");
        frame.setSize(1200, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JLabel header = new JLabel("Construction Data Management System", JLabel.CENTER);
        header.setFont(new Font("Century Gothic", Font.BOLD, 28));
        header.setOpaque(true);
        header.setBackground(new Color(40, 60, 80));
        header.setForeground(Color.CYAN);
        header.setPreferredSize(new Dimension(frame.getWidth(), 50));
        frame.add(header, BorderLayout.NORTH);

     // ===== Sidebar Panel =====
        JPanel sidebar = new JPanel(new GridLayout(6, 1, 10, 10));
        sidebar.setBackground(new Color(52, 73, 94));

        // Sidebar width
        sidebar.setPreferredSize(new Dimension(190, frame.getHeight()));

        // ===== Buttons =====
        JButton btnProject     = new JButton("Project / Site Record");
        JButton btnLabour      = new JButton("<html><center>Labour Attendance<br>& Wages</center></html>");
        JButton btnMaterial    = new JButton("Material Tracking");
        JButton btnQuotation   = new JButton("Quotation");
        JButton btnPayment     = new JButton("Payments");
        JButton btnRunningBill = new JButton("Running Bill");

        // ===== Font Style & Size =====
        Font sidebarFont = new Font("Century Gothic", Font.BOLD, 17);

        JButton[] buttons = {
            btnProject, btnLabour, btnMaterial, btnQuotation, btnPayment, btnRunningBill
        };

        for (JButton btn : buttons) {
            btn.setFont(sidebarFont);
            btn.setFocusPainted(false);
            btn.setBackground(new Color(70, 130, 180));
            btn.setForeground(Color.ORANGE);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // spacing
            sidebar.add(btn);
        }

        frame.add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createDashboardPage(), "Dashboard");
        mainPanel.add(createProjectPage(), "Project");
        mainPanel.add(createLabourPage(), "Labour");
        mainPanel.add(createMaterialPage(), "Material");
        mainPanel.add(createQuotationPage(), "Quotation");
        mainPanel.add(createPaymentPage(), "Payment");
        mainPanel.add(createRunningBillPage(), "RunningBill");

        frame.add(mainPanel, BorderLayout.CENTER);

        btnProject.addActionListener(_ -> cardLayout.show(mainPanel, "Project"));
        btnLabour.addActionListener(_ -> cardLayout.show(mainPanel, "Labour"));
        btnMaterial.addActionListener(_ -> cardLayout.show(mainPanel, "Material"));
        btnQuotation.addActionListener(_ -> cardLayout.show(mainPanel, "Quotation"));
        btnPayment.addActionListener(_ -> {
            loadProjectDropdown();
            loadPayment(); 
            cardLayout.show(mainPanel, "Payment");
        });
        btnRunningBill.addActionListener(_ -> {
            loadRunningBill();
            cardLayout.show(mainPanel, "RunningBill");
        });

        frame.setVisible(true);
    }
    public ConstructionDataManagementSystem(String role) {
        this.userRole = role;
        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/construction_db",
                "root","mmpc2405"
            );
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,"Database connection failed!");
            return;
        }

        createUI();
        
    }
    
 // ---------- DASHBOARD PAGE ----------
    private JPanel createDashboardPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Welcome label
        JLabel lbl = new JLabel("Welcome to Dashboard", JLabel.CENTER);
        lbl.setFont(new Font("Century Gothic", Font.BOLD, 24));
        panel.add(lbl, BorderLayout.NORTH);

        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        try {
            Statement stmt = conn.createStatement();

            // Corrected queries
            int totalLabour = stmt.executeQuery("SELECT COUNT(*) FROM labour").next() ? stmt.getResultSet().getInt(1) : 0;
            int totalAssistant = stmt.executeQuery("SELECT COUNT(*) FROM assistant").next() ? stmt.getResultSet().getInt(1) : 0;
            int totalProject = stmt.executeQuery("SELECT COUNT(*) FROM projects").next() ? stmt.getResultSet().getInt(1) : 0;
            double totalCash = stmt.executeQuery("SELECT IFNULL(SUM(wage),0) FROM labour").next() ? stmt.getResultSet().getDouble(1) : 0;

            stmt.close();

            statsPanel.add(createStatCard("Total Labour", String.valueOf(totalLabour)));
            statsPanel.add(createStatCard("Total Assistants", String.valueOf(totalAssistant)));
            statsPanel.add(createStatCard("Total Projects", String.valueOf(totalProject)));
            statsPanel.add(createStatCard("Total Cash Paid (₹)", String.format("%.2f", totalCash)));

        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading dashboard stats");
        }

        panel.add(statsPanel, BorderLayout.CENTER);
        return panel;
    }

    // Helper: create a card for dashboard stat
    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Arial", Font.PLAIN, 22));
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }
    

    // ----------------- PROJECT PAGE -----------------

    private JPanel createProjectPage() {
        JPanel panel = new JPanel(new BorderLayout());
        projectTableModel = new DefaultTableModel(new String[]{"ID","Name","Location","Start","End"}, 0);
        projectTable = new JTable(projectTableModel);
        applyMultiLineRenderer(projectTable);
        loadProjects();

        JPanel form = new JPanel(new GridLayout(5,2,10,10));
        final JTextField txtName = new JTextField();
        final JTextField txtLocation = new JTextField();
        final JTextField txtStart = new JTextField();
        final JTextField txtEnd = new JTextField();

        form.add(new JLabel("Project Name:")); form.add(txtName);
        form.add(new JLabel("Location:")); form.add(txtLocation);
        form.add(new JLabel("Start Date (yyyy-MM-dd):")); form.add(txtStart);
        form.add(new JLabel("End Date (yyyy-MM-dd):")); form.add(txtEnd);

        JButton btnSave = new JButton("Save");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnSave); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        panel.add(form, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(projectTable), BorderLayout.CENTER);

        projectTable.getSelectionModel().addListSelectionListener(_ -> {
            int row = projectTable.getSelectedRow();
            if(row >= 0){
                txtName.setText(projectTableModel.getValueAt(row,1).toString());
                txtLocation.setText(projectTableModel.getValueAt(row,2).toString());
                txtStart.setText(projectTableModel.getValueAt(row,3).toString());
                txtEnd.setText(projectTableModel.getValueAt(row,4).toString());
            }
        });

        btnSave.addActionListener(_ -> {
            try{
                SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");

                PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO projects(name,location,start_date,end_date) VALUES(?,?,?,?)");
                pst.setString(1, txtName.getText());
                pst.setString(2, txtLocation.getText());
                pst.setString(3, dbFormat.format(userFormat.parse(txtStart.getText())));
                pst.setString(4, dbFormat.format(userFormat.parse(txtEnd.getText())));
                pst.executeUpdate();

                loadProjects();
                txtName.setText(""); txtLocation.setText(""); txtStart.setText(""); txtEnd.setText("");
            } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(frame,"Invalid date format!"); }
        });

        btnUpdate.addActionListener(_ -> {
            int row = projectTable.getSelectedRow();
            if(row >= 0){
                try{
                    SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");
                    PreparedStatement pst = conn.prepareStatement(
                            "UPDATE projects SET name=?, location=?, start_date=?, end_date=? WHERE id=?");
                    pst.setString(1, txtName.getText());
                    pst.setString(2, txtLocation.getText());
                    pst.setString(3, dbFormat.format(userFormat.parse(txtStart.getText())));
                    pst.setString(4, dbFormat.format(userFormat.parse(txtEnd.getText())));
                    pst.setInt(5, (int) projectTableModel.getValueAt(row,0));
                    pst.executeUpdate();
                    loadProjects();
                    txtName.setText(""); txtLocation.setText(""); txtStart.setText(""); txtEnd.setText("");
                } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(frame,"Invalid date format!"); }
            }
        });

        btnDelete.addActionListener(_ -> {
            int row = projectTable.getSelectedRow();
            if(row >=0){
                try{
                    PreparedStatement pst = conn.prepareStatement("DELETE FROM projects WHERE id=?");
                    pst.setInt(1, (int) projectTableModel.getValueAt(row,0));
                    pst.executeUpdate();
                    loadProjects();
                } catch (SQLException ex){ ex.printStackTrace(); }
            }
        });

        return panel;
    }

    private void loadProjects() {
        try{
            projectTableModel.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM projects");
            while(rs.next()){
                projectTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getString("start_date"),
                        rs.getString("end_date")
                });
            }
            adjustRowHeight(projectTable);
        } catch(SQLException e){ e.printStackTrace(); }
    }

 
 // ---------- LABOUR PAGE (Full Monthly Attendance + Salary Summary) ----------
    private void updateLabourTotals() {
        try {
            PreparedStatement pst1 = conn.prepareStatement(
                "SELECT COUNT(*) FROM labour_master"
            );
            ResultSet rs1 = pst1.executeQuery();
            rs1.next();
            lblTotalLabour.setText("Total Labour: " + rs1.getInt(1));
            rs1.close();
            pst1.close();

            PreparedStatement pst2 = conn.prepareStatement(
                "SELECT IFNULL(SUM(cash),0) FROM labour"
            );
            ResultSet rs2 = pst2.executeQuery();
            rs2.next();
            lblTotalCash.setText("Total Cash Paid: ₹" + rs2.getDouble(1));
            rs2.close();
            pst2.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    
    private JPanel createLabourPage() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

     // 1️⃣ Create Assistant Panel
        JPanel assistantPanel = new JPanel();
        assistantPanel.setLayout(new BorderLayout());

        // Example: Table to show assistants
        String[] columns = {"ID", "Name", "Username", "Salary", "Advance"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        assistantPanel.add(scroll, BorderLayout.CENTER);

        // Buttons for admin actions
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        
        btnAdd.addActionListener(_ -> {

            if(userRole.equals("ASSISTANT")) {
                JOptionPane.showMessageDialog(null, "You are not allowed!");
                return;
            }

            JTextField txtUsername = new JTextField();
            JPasswordField txtPassword = new JPasswordField();
            JTextField txtName = new JTextField();
            JTextField txtMobile = new JTextField();
            JTextField txtSalary = new JTextField();

            Object[] fields = {
                "Username (Login):", txtUsername,
                "Password:", txtPassword,
                "Name:", txtName,
                "Mobile:", txtMobile,
                "Salary:", txtSalary
            };

            int option = JOptionPane.showConfirmDialog(
                null,
                fields,
                "Add Assistant",
                JOptionPane.OK_CANCEL_OPTION
            );

            if(option == JOptionPane.OK_OPTION) {
                try {
                    // ===== 1️⃣ Check username exists =====
                    PreparedStatement checkPs = conn.prepareStatement(
                        "SELECT username FROM users WHERE username=?"
                    );
                    checkPs.setString(1, txtUsername.getText().trim());
                    ResultSet checkRs = checkPs.executeQuery();

                    if(checkRs.next()) {
                        JOptionPane.showMessageDialog(null,
                            "Username already exists ❌");
                        return;
                    }

                    // ===== 2️⃣ Insert into users table =====
                    PreparedStatement psUser = conn.prepareStatement(
                    	    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)"
                    	);

                    	psUser.setString(1, txtUsername.getText().trim());
                    	psUser.setString(2, new String(txtPassword.getPassword()));
                    	psUser.setString(3, "ASSISTANT");

                    	psUser.executeUpdate();

                    // ===== 3️⃣ Insert into assistant table =====
                    PreparedStatement psAssistant = conn.prepareStatement(
                    	    "INSERT INTO assistant (name, username, monthly_salary, advance_paid) VALUES (?, ?, ?, ?)"
                    	);

                    	psAssistant.setString(1, txtName.getText().trim());       // name
                    	psAssistant.setString(2, txtUsername.getText().trim());   // username
                    	psAssistant.setDouble(3, Double.parseDouble(txtSalary.getText().trim())); // salary
                    	psAssistant.setDouble(4, 0.0); // advance default

                    	psAssistant.executeUpdate();

                    JOptionPane.showMessageDialog(null, "Assistant added successfully ✅");

                    // table reload (agar method bana hua hai)
                    // loadAssistant();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error adding assistant");
                }
            }

        });



        // Panel for buttons
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        assistantPanel.add(btnPanel, BorderLayout.SOUTH);

        // 2️⃣ Load data from DB
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs;

            if(userRole.equals("ASSISTANT")) {
                // Assistant → only own record
                rs = stmt.executeQuery("SELECT * FROM assistant WHERE username='" + loggedUsername + "'");
            } else {
                // Admin → all assistants
                rs = stmt.executeQuery("SELECT * FROM assistant");
            }

            while(rs.next()) {
            	Object[] row = {
            		    rs.getInt("id"),
            		    rs.getString("name"),
            		    rs.getString("username"),
            		    rs.getDouble("monthly_salary"),
            		    rs.getDouble("advance_paid")
            		};
            		model.addRow(row);

            }

        } catch(SQLException ex) {
            ex.printStackTrace();
        }

        // 3️⃣ Role-based button control
        if(userRole.equals("ASSISTANT")) {
            btnAdd.setEnabled(false);
            btnUpdate.setEnabled(false);
            btnDelete.setEnabled(false);
        }

        // 4️⃣ Add tab
        tabs.addTab("Assistant", assistantPanel);

     // ================= ATTENDANCE TAB =================
        JPanel attendancePanel = new JPanel(new BorderLayout());

        // ---------- FORM ----------
        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));

        JTextField txtName = new JTextField();
        JTextField txtRole = new JTextField();
        JTextField txtDate = new JTextField("yyyy-MM-dd");
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"P", "A", "H"});
        JTextField txtCash = new JTextField("0");
        JTextField txtOvertime = new JTextField("0");
        JTextField txtDailyRate = new JTextField();
        JTextField txtOTRate = new JTextField();

        form.add(new JLabel("Name")); form.add(txtName);
        form.add(new JLabel("Role")); form.add(txtRole);
        form.add(new JLabel("Date")); form.add(txtDate);
        form.add(new JLabel("Status")); form.add(cmbStatus);
        form.add(new JLabel("Cash")); form.add(txtCash);
        form.add(new JLabel("Overtime (hrs)")); form.add(txtOvertime);
        form.add(new JLabel("Daily Rate")); form.add(txtDailyRate);
        form.add(new JLabel("OT Rate")); form.add(txtOTRate);

     // ---------- BUTTONS ----------
        JButton btnSave = new JButton("Save");
        JButton btnUpdateAtt = new JButton("Update");
        JButton btnDeleteAtt = new JButton("Delete");

        JPanel btnPanelAtt = new JPanel();
        btnPanelAtt.add(btnSave);
        btnPanelAtt.add(btnUpdateAtt);
        btnPanelAtt.add(btnDeleteAtt);


        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(form, BorderLayout.CENTER);
        topPanel.add(btnPanel, BorderLayout.SOUTH);

        attendancePanel.add(topPanel, BorderLayout.NORTH);

        // ---------- MONTH + TABLE ----------
        JPanel middlePanel = new JPanel(new BorderLayout());
        JComboBox<String> cmbMonth = new JComboBox<>();
        fillMonthCombo(cmbMonth);

        JButton btnLoad = new JButton("Load");

        JPanel monthPanel = new JPanel();
        monthPanel.add(new JLabel("Month"));
        monthPanel.add(cmbMonth);
        monthPanel.add(btnLoad);

        middlePanel.add(monthPanel, BorderLayout.NORTH);

        labourTableModel = new DefaultTableModel();
        labourTable = new JTable(labourTableModel);
        applyMultiLineRenderer(labourTable);

        middlePanel.add(new JScrollPane(labourTable), BorderLayout.CENTER);
        attendancePanel.add(middlePanel, BorderLayout.CENTER);

        // ---------- AUTO LOAD RATE ----------
        txtName.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                try {
                    PreparedStatement pst = conn.prepareStatement(
                        "SELECT role, daily_rate, ot_rate FROM labour_master WHERE name=?"
                    );
                    pst.setString(1, txtName.getText().trim());
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        txtRole.setText(rs.getString("role"));
                        txtDailyRate.setText(rs.getString("daily_rate"));
                        txtOTRate.setText(rs.getString("ot_rate"));
                    }
                    rs.close();
                    pst.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // ---------- SAVE ----------
        btnSave.addActionListener(_ -> {
            try {
                int labourId = getLabourIdByName(txtName.getText().trim());
                if (labourId == 0) {
                    JOptionPane.showMessageDialog(frame, "Labour not found!");
                    return;
                }

                PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO labour (labour_id, name, date, status, cash, overtime) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "status=VALUES(status), cash=VALUES(cash), overtime=VALUES(overtime)"
                );

                pst.setInt(1, labourId);
                pst.setString(2, txtName.getText().trim());
                pst.setString(3, txtDate.getText().trim());
                pst.setString(4, cmbStatus.getSelectedItem().toString());
                pst.setDouble(5, Double.parseDouble(txtCash.getText()));
                pst.setDouble(6, Double.parseDouble(txtOvertime.getText()));

                pst.executeUpdate();
                pst.close();

                JOptionPane.showMessageDialog(frame, "Saved ✅");
                btnLoad.doClick();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ---------- UPDATE ----------
        btnUpdateAtt.addActionListener(_ -> {

            try {
                PreparedStatement pst = conn.prepareStatement(
                    "UPDATE labour SET status=?, cash=?, overtime=? WHERE labour_id=? AND date=?"
                );

                pst.setString(1, cmbStatus.getSelectedItem().toString());
                pst.setDouble(2, Double.parseDouble(txtCash.getText()));
                pst.setDouble(3, Double.parseDouble(txtOvertime.getText()));
                pst.setInt(4, getLabourIdByName(txtName.getText().trim()));
                pst.setString(5, txtDate.getText().trim());

                pst.executeUpdate();
                pst.close();

                JOptionPane.showMessageDialog(frame, "Updated ✅");
                btnLoad.doClick();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ---------- DELETE ----------
        btnDelete.addActionListener(_ -> {
            try {
                PreparedStatement pst = conn.prepareStatement(
                    "DELETE FROM labour WHERE labour_id=?"
                );
                pst.setInt(1, getLabourIdByName(txtName.getText().trim()));
                pst.executeUpdate();
                pst.close();

                JOptionPane.showMessageDialog(frame, "Deleted ✅");
                btnLoad.doClick();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ---------- LOAD ----------
        btnLoad.addActionListener(_ -> {
            if (cmbMonth.getSelectedItem() != null) {
                loadMonthlyAttendance(cmbMonth.getSelectedItem().toString());
            }
        });

        // ---------- TABLE CLICK ----------
        labourTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    int r = labourTable.getSelectedRow();
                    int c = labourTable.getSelectedColumn();
                    if (r < 0 || c < 2) return;

                    String name = labourTable.getValueAt(r, 0).toString();
                    txtName.setText(name);

                    String month = cmbMonth.getSelectedItem().toString();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sdf.parse(month));

                    String date = String.format("%04d-%02d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        c - 1
                    );

                    txtDate.setText(date);

                    PreparedStatement pst = conn.prepareStatement(
                        "SELECT status, cash, overtime FROM labour WHERE labour_id=? AND date=?"
                    );
                    pst.setInt(1, getLabourIdByName(name));
                    pst.setString(2, date);

                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        cmbStatus.setSelectedItem(rs.getString("status"));
                        txtCash.setText(rs.getString("cash"));
                        txtOvertime.setText(rs.getString("overtime"));
                    }

                    rs.close();
                    pst.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        tabs.addTab("Attendance", attendancePanel);


        // ------- SALARY SUMMARY TAB -------
        JPanel summaryPanel = new JPanel(new BorderLayout());
        JPanel summaryTop = new JPanel();
        JComboBox<String> cmbMonth2 = new JComboBox<>();
        fillMonthCombo(cmbMonth2);
        JButton btnLoadSummary = new JButton("Load Summary");
        summaryTop.add(new JLabel("Month:"));
        summaryTop.add(cmbMonth2);
        summaryTop.add(btnLoadSummary);
        summaryPanel.add(summaryTop, BorderLayout.NORTH);

        DefaultTableModel summaryModel = new DefaultTableModel(
            new String[]{"Name","Role","Daily Rate","OT Rate/hr","Present","Half","Absent","OT (hrs)","Advance (₹)","Final Pay (₹)"}, 0);
        JTable summaryTable = new JTable(summaryModel);
        summaryTable.setAutoCreateRowSorter(true);
        summaryPanel.add(new JScrollPane(summaryTable), BorderLayout.CENTER);

        btnLoadSummary.addActionListener(_ -> {
            String monthYear = (String) cmbMonth2.getSelectedItem();
            if (monthYear != null) loadSalarySummary(monthYear, summaryModel);
        });
        tabs.addTab("Salary Summary", summaryPanel);
        outerPanel.add(tabs, BorderLayout.CENTER);

        // 🔥 FORCE totals update when page loads
        SwingUtilities.invokeLater(() -> updateLabourTotals());

        return outerPanel;

    }

    // ------------- Helper Methods -------------
    private int getLabourIdByName(String name) throws Exception {
        PreparedStatement pst = conn.prepareStatement(
            "SELECT id FROM labour_master WHERE name=?"
        );
        pst.setString(1, name);
        ResultSet rs = pst.executeQuery();

        int id = 0;
        if (rs.next()) id = rs.getInt("id");

        rs.close();
        pst.close();
        return id;
    }


        
     // === Monthly Attendance Loader (labour_id version) ===
    private void loadMonthlyAttendance(String monthYear) {
        try {
            labourTableModel.setRowCount(0);
            labourTableModel.setColumnCount(0);

            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
            Date parsed = sdf.parse(monthYear);

            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            labourTableModel.addColumn("Name");
            labourTableModel.addColumn("Role");

            for (int d = 1; d <= daysInMonth; d++)
                labourTableModel.addColumn(String.format("%02d", d));

            labourTableModel.addColumn("Total Present");

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT id, name, role FROM labour_master ORDER BY name"
            );

            while (rs.next()) {
                int labourId = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");

                Object[] row = new Object[daysInMonth + 3];
                row[0] = name;
                row[1] = role;

                double totalPresent = 0;

                for (int d = 1; d <= daysInMonth; d++) {
                    String dateStr = String.format("%04d-%02d-%02d", year, month, d);

                    PreparedStatement pst = conn.prepareStatement(
                        "SELECT status, IFNULL(cash,0), IFNULL(overtime,0) " +
                        "FROM labour WHERE labour_id=? AND date=?"
                    );
                    pst.setInt(1, labourId);
                    pst.setString(2, dateStr);

                    ResultSet rday = pst.executeQuery();

                    String cell = "A";
                    double cash = 0, ot = 0;

                    if (rday.next()) {
                        String s = rday.getString("status");
                        cash = rday.getDouble("cash");
                        ot = rday.getDouble("overtime");
                        cell = s;

                        if ("P".equals(s)) totalPresent += 1;
                        else if ("H".equals(s)) totalPresent += 0.5;
                    }

                    StringBuilder sb = new StringBuilder(cell);
                    if (cash > 0) sb.append("\n₹").append(cash);
                    if (ot > 0) sb.append("\nOT ").append(ot).append("h");

                    row[d + 1] = sb.toString();

                    rday.close();
                    pst.close();
                }

                row[daysInMonth + 2] = totalPresent;
                labourTableModel.addRow(row);
            }

            rs.close();
            st.close();
            applyMultiLineRenderer(labourTable);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

        
        private void fillMonthCombo(JComboBox<String> combo) {
            try {
                combo.removeAllItems();

                PreparedStatement pst = conn.prepareStatement(
                    "SELECT DISTINCT DATE_FORMAT(date,'%M %Y') AS monthYear " +
                    "FROM labour ORDER BY STR_TO_DATE(monthYear,'%M %Y') DESC"
                );

                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    combo.addItem(rs.getString("monthYear"));
                }

                rs.close();
                pst.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }





     // === Salary Summary Loader (labour_id version) ===
        private void loadSalarySummary(String monthYear, DefaultTableModel summaryModel) {
            try {
                summaryModel.setRowCount(0);

                SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
                Date parsed = sdf.parse(monthYear);

                Calendar cal = Calendar.getInstance();
                cal.setTime(parsed);

                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                    "SELECT id, name, role, IFNULL(daily_rate,0), IFNULL(ot_rate,0) " +
                    "FROM labour_master ORDER BY name"
                );

                while (rs.next()) {
                    int labourId = rs.getInt("id");
                    String name = rs.getString("name");
                    String role = rs.getString("role");
                    double dr = rs.getDouble(4);
                    double otr = rs.getDouble(5);

                    double present = 0, half = 0, absent = 0;
                    double totalOT = 0, totalAdvance = 0, totalWage = 0;

                    for (int d = 1; d <= daysInMonth; d++) {
                        String dateStr = String.format("%04d-%02d-%02d", year, month, d);

                        PreparedStatement pst = conn.prepareStatement(
                            "SELECT status, IFNULL(cash,0), IFNULL(overtime,0) " +
                            "FROM labour WHERE labour_id=? AND date=?"
                        );
                        pst.setInt(1, labourId);
                        pst.setString(2, dateStr);

                        ResultSet rday = pst.executeQuery();

                        if (rday.next()) {
                            String s = rday.getString("status");
                            double cash = rday.getDouble("cash");
                            double ot = rday.getDouble("overtime");

                            if ("P".equals(s)) present++;
                            else if ("H".equals(s)) half++;
                            else absent++;

                            totalAdvance += cash;
                            totalOT += ot;

                            double wage = 0;
                            if ("P".equals(s)) wage += dr;
                            else if ("H".equals(s)) wage += dr * 0.5;
                            wage += ot * otr;

                            totalWage += wage;
                        } else {
                            absent++;
                        }

                        rday.close();
                        pst.close();
                    }

                    double finalPay = totalWage - totalAdvance;

                    summaryModel.addRow(new Object[]{
                        name, role, dr, otr,
                        present, half, absent,
                        totalOT, totalAdvance, finalPay
                    });
                }

                rs.close();
                st.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }





 // ----------------- MATERIAL PAGE -----------------
   
    private JPanel createMaterialPage() {
        JPanel panel = new JPanel(new BorderLayout());

        // Table model
        materialTableModel = new DefaultTableModel(new String[]{
            "ID","Name","Quantity","Amount (₹)","Supplier","Mobile","Invoice No","Invoice Date","Delivery Date"},0);
        materialTable = new JTable(materialTableModel);
        applyMultiLineRenderer(materialTable);
        loadMaterial();

        // Form fields
        JPanel form = new JPanel(new GridLayout(9,2,10,10));
        final JTextField txtName = new JTextField();
        final JTextField txtQty = new JTextField();
        final JTextField txtAmount = new JTextField();
        final JTextField txtSupplier = new JTextField();
        final JTextField txtMobile = new JTextField();
        final JTextField txtInvoiceNo = new JTextField();
        final JTextField txtInvoiceDate = new JTextField();
        final JTextField txtDeliveryDate = new JTextField();

        // Add fields
        form.add(new JLabel("Material Name:")); form.add(txtName);
        form.add(new JLabel("Quantity:")); form.add(txtQty);
        form.add(new JLabel("Amount (₹):")); form.add(txtAmount);
        form.add(new JLabel("Supplier:")); form.add(txtSupplier);
        form.add(new JLabel("Supplier Mobile:")); form.add(txtMobile);
        form.add(new JLabel("Invoice No:")); form.add(txtInvoiceNo);
        form.add(new JLabel("Invoice Date (yyyy-MM-dd):")); form.add(txtInvoiceDate);
        form.add(new JLabel("Delivery Date (yyyy-MM-dd):")); form.add(txtDeliveryDate);

        // Buttons
        JButton btnSave = new JButton("Save");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnSave); 
        btnPanel.add(btnUpdate); 
        btnPanel.add(btnDelete);

        panel.add(form, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(materialTable), BorderLayout.CENTER);

        // Table selection → fill fields
        materialTable.getSelectionModel().addListSelectionListener(_ -> {
            int row = materialTable.getSelectedRow();
            if (row >= 0) {
                txtName.setText(materialTableModel.getValueAt(row,1).toString());
                txtQty.setText(materialTableModel.getValueAt(row,2).toString());
                txtAmount.setText(materialTableModel.getValueAt(row,3).toString());
                txtSupplier.setText(materialTableModel.getValueAt(row,4).toString());
                txtMobile.setText(materialTableModel.getValueAt(row,5).toString());
                txtInvoiceNo.setText(materialTableModel.getValueAt(row,6).toString());
                txtInvoiceDate.setText(materialTableModel.getValueAt(row,7).toString());
                txtDeliveryDate.setText(materialTableModel.getValueAt(row,8).toString());
            }
        });

        // SAVE
        btnSave.addActionListener(_ -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO material(name,qty,amount,supplier,supplier_mobile,invoice_no,invoice_date,delivery_date) VALUES(?,?,?,?,?,?,?,?)");
                pst.setString(1, txtName.getText());
                pst.setString(2, txtQty.getText());
                pst.setString(3, txtAmount.getText());
                pst.setString(4, txtSupplier.getText());
                pst.setString(5, txtMobile.getText());
                pst.setString(6, txtInvoiceNo.getText());
                pst.setString(7, sdf.format(sdf.parse(txtInvoiceDate.getText())));
                pst.setString(8, sdf.format(sdf.parse(txtDeliveryDate.getText())));
                pst.executeUpdate();
                pst.close();

                JOptionPane.showMessageDialog(frame, "Material Saved Successfully!");
                loadMaterial();

                txtName.setText(""); txtQty.setText(""); txtAmount.setText("");
                txtSupplier.setText(""); txtMobile.setText(""); txtInvoiceNo.setText("");
                txtInvoiceDate.setText(""); txtDeliveryDate.setText("");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error! Check all fields or date format (yyyy-MM-dd).");
            }
        });

        // UPDATE
        btnUpdate.addActionListener(_ -> {
            int row = materialTable.getSelectedRow();
            if (row >= 0) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    PreparedStatement pst = conn.prepareStatement(
                        "UPDATE material SET name=?, qty=?, amount=?, supplier=?, supplier_mobile=?, invoice_no=?, invoice_date=?, delivery_date=? WHERE id=?");
                    pst.setString(1, txtName.getText());
                    pst.setString(2, txtQty.getText());
                    pst.setString(3, txtAmount.getText());
                    pst.setString(4, txtSupplier.getText());
                    pst.setString(5, txtMobile.getText());
                    pst.setString(6, txtInvoiceNo.getText());
                    pst.setString(7, sdf.format(sdf.parse(txtInvoiceDate.getText())));
                    pst.setString(8, sdf.format(sdf.parse(txtDeliveryDate.getText())));
                    pst.setInt(9, (int) materialTableModel.getValueAt(row,0));
                    pst.executeUpdate();
                    pst.close();

                    JOptionPane.showMessageDialog(frame, "Material Updated Successfully!");
                    loadMaterial();

                    txtName.setText(""); txtQty.setText(""); txtAmount.setText("");
                    txtSupplier.setText(""); txtMobile.setText(""); txtInvoiceNo.setText("");
                    txtInvoiceDate.setText(""); txtDeliveryDate.setText("");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error updating record.");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a record to update!");
            }
        });

        // DELETE
        btnDelete.addActionListener(_ -> {

            int row = materialTable.getSelectedRow();
            if (row >= 0) {
                try {
                    int confirm = JOptionPane.showConfirmDialog(frame, 
                        "Are you sure you want to delete this record?", 
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;

                    PreparedStatement pst = conn.prepareStatement("DELETE FROM material WHERE id=?");
                    pst.setInt(1, (int) materialTableModel.getValueAt(row,0));
                    pst.executeUpdate();
                    pst.close();

                    JOptionPane.showMessageDialog(frame, "Material Deleted Successfully!");
                    loadMaterial();

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error deleting record!");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a record to delete!");
            }
        });

        return panel;
    }

    private void loadMaterial() {
        try {
            materialTableModel.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM material");
            while (rs.next()) {
                materialTableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("qty"),
                    rs.getString("amount"),
                    rs.getString("supplier"),
                    rs.getString("supplier_mobile"),
                    rs.getString("invoice_no"),
                    rs.getString("invoice_date"),
                    rs.getString("delivery_date")
                });
            }
            adjustRowHeight(materialTable);
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ----------------- QUOTATION PAGE -----------------
   
    private JPanel createQuotationPage() {
        JPanel panel = new JPanel(new BorderLayout());

        quotationTableModel = new DefaultTableModel(new String[]{"ID","Client","Description","Amount","Date"},0);
        quotationTable = new JTable(quotationTableModel);
        applyMultiLineRenderer(quotationTable);
        loadQuotation();

        JPanel form = new JPanel(new GridLayout(5,2,10,10));
        final JTextField txtClient = new JTextField();
        final JTextField txtDesc = new JTextField();
        final JTextField txtAmount = new JTextField();
        final JTextField txtDate = new JTextField();

        form.add(new JLabel("Client Name:")); form.add(txtClient);
        form.add(new JLabel("Description:")); form.add(txtDesc);
        form.add(new JLabel("Amount:")); form.add(txtAmount);
        form.add(new JLabel("Date (yyyy-MM-dd):")); form.add(txtDate);

        JButton btnSave = new JButton("Save");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnSave); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        panel.add(form, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(quotationTable), BorderLayout.CENTER);

        quotationTable.getSelectionModel().addListSelectionListener(_ -> {
            int row = quotationTable.getSelectedRow();
            if(row >=0){
                txtClient.setText(quotationTableModel.getValueAt(row,1).toString());
                txtDesc.setText(quotationTableModel.getValueAt(row,2).toString());
                txtAmount.setText(quotationTableModel.getValueAt(row,3).toString());
                txtDate.setText(quotationTableModel.getValueAt(row,4).toString());
            }
        });

        btnSave.addActionListener(_ -> {
            try{
                SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");

                PreparedStatement pst = conn.prepareStatement("INSERT INTO quotation(client,description,amount,date) VALUES(?,?,?,?)");
                pst.setString(1, txtClient.getText());
                pst.setString(2, txtDesc.getText());
                pst.setString(3, txtAmount.getText());
                pst.setString(4, dbFormat.format(userFormat.parse(txtDate.getText())));
                pst.executeUpdate();

                loadQuotation();
                txtClient.setText(""); txtDesc.setText(""); txtAmount.setText(""); txtDate.setText("");
            } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(frame,"Invalid date format!"); }
        });

        btnUpdate.addActionListener(_ -> {
            int row = quotationTable.getSelectedRow();
            if(row >=0){
                try{
                    SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");

                    PreparedStatement pst = conn.prepareStatement("UPDATE quotation SET client=?, description=?, amount=?, date=? WHERE id=?");
                    pst.setString(1, txtClient.getText());
                    pst.setString(2, txtDesc.getText());
                    pst.setString(3, txtAmount.getText());
                    pst.setString(4, dbFormat.format(userFormat.parse(txtDate.getText())));
                    pst.setInt(5, (int) quotationTableModel.getValueAt(row,0));
                    pst.executeUpdate();

                    loadQuotation();
                    txtClient.setText(""); txtDesc.setText(""); txtAmount.setText(""); txtDate.setText("");
                } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(frame,"Invalid date format!"); }
            }
        });

        btnDelete.addActionListener(_ -> {
            int row = quotationTable.getSelectedRow();
            if(row >=0){
                try{
                    PreparedStatement pst = conn.prepareStatement("DELETE FROM quotation WHERE id=?");
                    pst.setInt(1, (int) quotationTableModel.getValueAt(row,0));
                    pst.executeUpdate();
                    loadQuotation();
                } catch(SQLException ex){ ex.printStackTrace(); }
            }
        });

        return panel;
    }

    private void loadQuotation(){
        try{
            quotationTableModel.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM quotation");
            while(rs.next()){
                quotationTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("client"),
                        rs.getString("description"),
                        rs.getString("amount"),
                        rs.getString("date")
                });
            }
            adjustRowHeight(quotationTable);
        } catch(SQLException e){ e.printStackTrace(); }
    }

    // ----------------- PAYMENT PAGE -----------------
   
    private JPanel createPaymentPage(){
        JPanel panel = new JPanel(new BorderLayout());

        paymentTableModel = new DefaultTableModel(new String[]{"ID","Project","Type","Category","Description","Amount","Date","Cash/Online"},0);
        paymentTable = new JTable(paymentTableModel);
        applyMultiLineRenderer(paymentTable);

        JPanel form = new JPanel(new GridLayout(7,2,10,10));
        cmbProjectDropdown = new JComboBox<>();

        // Type dropdown: received / paid
        final JComboBox<String> cmbType = new JComboBox<>(new String[] {"received","paid"});

        // Category dropdown: Labour, Material, Other Expenses, Client Cash
        final JComboBox<String> cmbCategory = new JComboBox<>(new String[] {"Labour","Material","Other Expenses","Client"});

        final JTextField txtDesc = new JTextField();
        final JTextField txtAmount = new JTextField();
        final JTextField txtDate = new JTextField();
        final JTextField txtCashOnline = new JTextField();

        form.add(new JLabel("Project:")); form.add(cmbProjectDropdown);
        form.add(new JLabel("Type:")); form.add(cmbType);
        form.add(new JLabel("Category:")); form.add(cmbCategory);
        form.add(new JLabel("Description:")); form.add(txtDesc);
        form.add(new JLabel("Amount:")); form.add(txtAmount);
        form.add(new JLabel("Date (yyyy-MM-dd):")); form.add(txtDate);
        form.add(new JLabel("Cash/Online:")); form.add(txtCashOnline);

        JButton btnSave = new JButton("Save");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnSave); btnPanel.add(btnUpdate); btnPanel.add(btnDelete);

        panel.add(form, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(paymentTable), BorderLayout.CENTER);

        paymentTable.getSelectionModel().addListSelectionListener(_ -> {
            int row = paymentTable.getSelectedRow();
            if(row>=0){
                String projectName = paymentTableModel.getValueAt(row,1).toString();
                cmbProjectDropdown.setSelectedItem(projectName);

                String type = paymentTableModel.getValueAt(row,2).toString();
                cmbType.setSelectedItem(type);

                String category = paymentTableModel.getValueAt(row,3).toString();

                boolean found = false;
                for (int i=0;i<cmbCategory.getItemCount();i++){
                    if(cmbCategory.getItemAt(i).equals(category)){ found = true; break; }
                }
                if(!found){
                    cmbCategory.addItem(category);
                }
                cmbCategory.setSelectedItem(category);

                txtDesc.setText(paymentTableModel.getValueAt(row,4).toString());
                txtAmount.setText(paymentTableModel.getValueAt(row,5).toString());
                txtDate.setText(paymentTableModel.getValueAt(row,6).toString());
                txtCashOnline.setText(paymentTableModel.getValueAt(row,7).toString());
            }
        });

        btnSave.addActionListener(_ -> {
            if (cmbProjectDropdown.getItemCount() == 0){
                JOptionPane.showMessageDialog(frame, "Please add a project first.");
                return;
            }
            int projectId = projectIds.get(cmbProjectDropdown.getSelectedIndex());
            try{
                SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = userFormat.parse(txtDate.getText());

                PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO payments(project_id,type,category,description,amount,date,cash_or_online) VALUES(?,?,?,?,?,?,?)");
                pst.setInt(1, projectId);
                pst.setString(2, cmbType.getSelectedItem().toString());
                pst.setString(3, cmbCategory.getSelectedItem().toString());
                pst.setString(4, txtDesc.getText());
                pst.setDouble(5, Double.parseDouble(txtAmount.getText()));
                pst.setString(6, dbFormat.format(date));
                pst.setString(7, txtCashOnline.getText());
                pst.executeUpdate();

                loadPayment();
                txtDesc.setText(""); txtAmount.setText(""); txtDate.setText(""); txtCashOnline.setText("");
            } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(frame,"Invalid input! Check amount/date."); }
        });

        btnUpdate.addActionListener(_ -> {
            int row = paymentTable.getSelectedRow();
            if(row>=0){
                int projectId = projectIds.get(cmbProjectDropdown.getSelectedIndex());
                try{
                    SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = userFormat.parse(txtDate.getText());

                    PreparedStatement pst = conn.prepareStatement(
                            "UPDATE payments SET project_id=?, type=?, category=?, description=?, amount=?, date=?, cash_or_online=? WHERE id=?");
                    pst.setInt(1, projectId);
                    pst.setString(2, cmbType.getSelectedItem().toString());
                    pst.setString(3, cmbCategory.getSelectedItem().toString());
                    pst.setString(4, txtDesc.getText());
                    pst.setDouble(5, Double.parseDouble(txtAmount.getText()));
                    pst.setString(6, dbFormat.format(date));
                    pst.setString(7, txtCashOnline.getText());
                    pst.setInt(8, (int) paymentTableModel.getValueAt(row,0));
                    pst.executeUpdate();

                    loadPayment();
                } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(frame,"Invalid input! Check amount/date."); }
            }
        });

        btnDelete.addActionListener(_ -> {
            int row = paymentTable.getSelectedRow();
            if(row>=0){
                try{
                    PreparedStatement pst = conn.prepareStatement("DELETE FROM payments WHERE id=?");
                    pst.setInt(1, (int) paymentTableModel.getValueAt(row,0));
                    pst.executeUpdate();
                    loadPayment();
                } catch(SQLException ex){ ex.printStackTrace(); }
            }
        });

        return panel;
    }

    private void loadProjectDropdown(){
        try{
            cmbProjectDropdown.removeAllItems();
            projectIds = new Vector<Integer>();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id,name FROM projects");
            while(rs.next()){
                cmbProjectDropdown.addItem(rs.getString("name"));
                projectIds.add(rs.getInt("id"));
            }
        } catch(SQLException e){ e.printStackTrace(); }
    }

    private void loadPayment(){
        try{

            if (projectIds == null) loadProjectDropdown();

            if (paymentTableModel == null){
                paymentTableModel = new DefaultTableModel(new String[]{"ID","Project","Type","Category","Description","Amount","Date","Cash/Online"},0);
                paymentTable.setModel(paymentTableModel);
            }
            paymentTableModel.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT pay.id, p.name, pay.type, pay.category, pay.description, pay.amount, pay.date, pay.cash_or_online " +
                    "FROM payments pay LEFT JOIN projects p ON pay.project_id = p.id");
            while(rs.next()){
                paymentTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name") != null ? rs.getString("name") : "(No project)",
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        rs.getString("date"),
                        rs.getString("cash_or_online")
                });
            }
            adjustRowHeight(paymentTable);
        } catch(SQLException e){ e.printStackTrace(); }
    }

    // ----------------- RUNNING BILL PAGE -----------------
    
    private JPanel createRunningBillPage(){
        JPanel panel = new JPanel(new BorderLayout());
        runningBillTableModel = new DefaultTableModel(new String[]{"Project","Total Received","Total Paid","Running Bill"},0);
        runningBillTable = new JTable(runningBillTableModel);
        applyMultiLineRenderer(runningBillTable);

        panel.add(new JScrollPane(runningBillTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadRunningBill(){
        try{
            runningBillTableModel.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM running_bill_view");
            while(rs.next()){
                runningBillTableModel.addRow(new Object[]{
                        rs.getString("project_name"),
                        rs.getDouble("total_received"),
                        rs.getDouble("total_paid"),
                        rs.getDouble("running_bill")
                });
            }
            adjustRowHeight(runningBillTable);
        } catch(SQLException e){ e.printStackTrace(); }
    }

    // ----------------- Utility -----------------
   
    private void applyMultiLineRenderer(JTable table){
        table.setDefaultRenderer(Object.class, new TextAreaRenderer());
    }

    private void adjustRowHeight(JTable table){
        for(int row=0; row<table.getRowCount(); row++){
            int maxHeight = table.getRowHeight();
            for(int column=0; column<table.getColumnCount(); column++){
                TableCellRenderer cellRenderer = table.getCellRenderer(row,column);
                Component comp = table.prepareRenderer(cellRenderer,row,column);
                maxHeight = Math.max(comp.getPreferredSize().height,maxHeight);
            }
            table.setRowHeight(row,maxHeight);
        }
    }

    static class TextAreaRenderer extends JTextArea implements TableCellRenderer{
        private static final long serialVersionUID = 1L;

        public TextAreaRenderer(){
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column){
            setText(value != null ? value.toString() : "");
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setFont(table.getFont());
            return this;
        }
    }
}