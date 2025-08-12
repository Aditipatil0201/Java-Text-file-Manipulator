import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;

public class AppGUI extends JFrame {
    private FileHandler fileHandler;
    private JTextArea displayArea;
    private JLabel statusLabel;
    private String lastSearchKeyword = "";
    private List<int[]> matchPositions = new ArrayList<>();
    private int currentMatchIndex = -1;

    public AppGUI() {
        fileHandler = new FileHandler("data.txt"); // default file
        initUI();
        loadFileToDisplay();
    }

    private void initUI() {
        setTitle("Text File Manipulator");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        displayArea = new JTextArea();
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        displayArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(displayArea);

        // Top toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton openBtn = new JButton("Open");
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton searchBtn = new JButton("Search");
        JButton prevBtn = new JButton("Previous");
        JButton nextBtn = new JButton("Next");
        JButton refreshBtn = new JButton("Refresh");

        toolbar.add(openBtn);
        toolbar.addSeparator();
        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(deleteBtn);
        toolbar.addSeparator();
        toolbar.add(searchBtn);
        toolbar.add(prevBtn);
        toolbar.add(nextBtn);
        toolbar.addSeparator();
        toolbar.add(refreshBtn);

        // Status bar
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        // Keyboard shortcuts
        setupKeyBindings();

        // Layout
        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Button actions
        openBtn.addActionListener(e -> openFileChooser());
        addBtn.addActionListener(e -> addLine());
        editBtn.addActionListener(e -> editLine());
        deleteBtn.addActionListener(e -> deleteLine());
        searchBtn.addActionListener(e -> promptSearch());
        prevBtn.addActionListener(e -> previousMatch());
        nextBtn.addActionListener(e -> nextMatch());
        refreshBtn.addActionListener(e -> refreshFile());

        // Double-click to edit selected line
        displayArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // On double click, attempt to edit the line under caret
                    int caret = displayArea.getCaretPosition();
                    int line = getLineNumberAtCaret(caret);
                    if (line >= 0) {
                        String current = getLineText(line);
                        String edited = JOptionPane.showInputDialog(AppGUI.this, "Edit line " + (line+1) + ":", current);
                        if (edited != null) {
                            fileHandler.editLine(line, edited);
                            loadFileToDisplayPreserveSearch();
                            showStatus("Edited line " + (line+1));
                        }
                    }
                }
            }
        });
    }

    private void setupKeyBindings() {
        // Ctrl+F for search
        KeyStroke ksFind = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        displayArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksFind, "findAction");
        displayArea.getActionMap().put("findAction", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { promptSearch(); }
        });

        // Ctrl+R for refresh
        KeyStroke ksRefresh = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
        displayArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksRefresh, "refreshAction");
        displayArea.getActionMap().put("refreshAction", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { refreshFile(); }
        });

        // F3 Next match
        KeyStroke ksNext = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        displayArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksNext, "nextMatch");
        displayArea.getActionMap().put("nextMatch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { nextMatch(); }
        });

        // Shift+F3 Previous match
        KeyStroke ksPrev = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK);
        displayArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksPrev, "prevMatch");
        displayArea.getActionMap().put("prevMatch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { previousMatch(); }
        });
    }

    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open text file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            fileHandler.setFileName(f.getAbsolutePath());
            loadFileToDisplay();
            showStatus("Opened: " + f.getName());
        }
    }

    private void addLine() {
        String text = JOptionPane.showInputDialog(this, "Enter text to add:");
        if (text != null && !text.trim().isEmpty()) {
            fileHandler.addLine(text.trim());
            loadFileToDisplayPreserveSearch();
            showStatus("Line added");
        }
    }

    private void editLine() {
        String lineStr = JOptionPane.showInputDialog(this, "Enter line number to edit:");
        if (lineStr == null) return;
        try {
            int lineNum = Integer.parseInt(lineStr.trim());
            int zeroIndex = lineNum - 1;
            String current = getLineText(zeroIndex);
            if (current == null) {
                JOptionPane.showMessageDialog(this, "Invalid line number.");
                return;
            }
            String newText = JOptionPane.showInputDialog(this, "Edit line " + lineNum + ":", current);
            if (newText != null) {
                fileHandler.editLine(zeroIndex, newText);
                loadFileToDisplayPreserveSearch();
                showStatus("Edited line " + lineNum);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    private void deleteLine() {
        String lineStr = JOptionPane.showInputDialog(this, "Enter line number to delete:");
        if (lineStr == null) return;
        try {
            int lineNum = Integer.parseInt(lineStr.trim());
            int zeroIndex = lineNum - 1;
            String current = getLineText(zeroIndex);
            if (current == null) {
                JOptionPane.showMessageDialog(this, "Invalid line number.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete line " + lineNum + "?\n" + current,
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                fileHandler.deleteLine(zeroIndex);
                loadFileToDisplayPreserveSearch();
                showStatus("Deleted line " + lineNum);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    private void promptSearch() {
        String keyword = JOptionPane.showInputDialog(this, "Enter text to search:");
        if (keyword != null) {
            lastSearchKeyword = keyword.trim();
            highlightSearch(lastSearchKeyword);
        }
    }

    private void refreshFile() {
        loadFileToDisplayPreserveSearch();
        showStatus("Refreshed from disk");
    }

    private void loadFileToDisplay() {
        displayArea.setText(fileHandler.readFileAsString());
        lastSearchKeyword = "";
        clearHighlights();
        updateStatus();
    }

    private void loadFileToDisplayPreserveSearch() {
        displayArea.setText(fileHandler.readFileAsString());
        if (lastSearchKeyword != null && !lastSearchKeyword.isEmpty()) {
            highlightSearch(lastSearchKeyword);
        } else {
            clearHighlights();
        }
        updateStatus();
    }

    private void updateStatus() {
        int lines = fileHandler.getLineCount();
        String path = fileHandler.getFileName();
        statusLabel.setText("Lines: " + lines + "    |    File: " + path);
    }

    private void showStatus(String msg) {
        statusLabel.setText(msg + "    |    Lines: " + fileHandler.getLineCount() + "    |    File: " + fileHandler.getFileName());
        // optional: restore default status after a delay
        Timer t = new Timer(3500, e -> updateStatus());
        t.setRepeats(false);
        t.start();
    }

    // ------------------ Search & Highlighting ------------------
    private void highlightSearch(String keyword) {
        clearHighlights();
        matchPositions.clear();
        currentMatchIndex = -1;

        if (keyword == null || keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empty search.");
            return;
        }

        String content = displayArea.getText().toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = content.indexOf(lowerKeyword);
        Highlighter highlighter = displayArea.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        while (index >= 0) {
            int start = index;
            int end = index + lowerKeyword.length();
            try {
                highlighter.addHighlight(start, end, painter);
                matchPositions.add(new int[] { start, end });
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            index = content.indexOf(lowerKeyword, end);
        }

        if (!matchPositions.isEmpty()) {
            currentMatchIndex = 0;
            jumpToCurrentMatch();
            JOptionPane.showMessageDialog(this, matchPositions.size() + " matches found.");
        } else {
            JOptionPane.showMessageDialog(this, "No matches found.");
        }
    }

    private void clearHighlights() {
        displayArea.getHighlighter().removeAllHighlights();
    }

    private void jumpToCurrentMatch() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matchPositions.size()) {
            int[] pos = matchPositions.get(currentMatchIndex);
            displayArea.requestFocus();
            displayArea.setCaretPosition(pos[0]);
            displayArea.moveCaretPosition(pos[1]); // selects the current match
        }
    }

    private void nextMatch() {
        if (matchPositions.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
        jumpToCurrentMatch();
    }

    private void previousMatch() {
        if (matchPositions.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex - 1 + matchPositions.size()) % matchPositions.size();
        jumpToCurrentMatch();
    }

    // ------------------ Helpers for line-based editing ------------------
    private int getLineNumberAtCaret(int caretPos) {
        try {
            int line = displayArea.getLineOfOffset(caretPos);
            return line; // zero-based
        } catch (BadLocationException ex) {
            return -1;
        }
    }

    private String getLineText(int zeroBasedLine) {
        try {
            int start = displayArea.getLineStartOffset(zeroBasedLine);
            int end = displayArea.getLineEndOffset(zeroBasedLine);
            String line = displayArea.getText(start, end - start);
            // remove trailing newline if present
            return line.replaceAll("\\r?\\n$", "");
        } catch (BadLocationException ex) {
            return null;
        }
    }

    // ------------------ Main ------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppGUI g = new AppGUI();
            g.setVisible(true);
        });
    }
}
