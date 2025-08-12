import java.io.*;
import java.util.*;

public class FileHandler {
    private String fileName;

    public FileHandler(String fileName) {
        this.fileName = fileName;
        ensureFileExists();
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        ensureFileExists();
    }

    public String getFileName() {
        return fileName;
    }

    private void ensureFileExists() {
        try {
            File f = new File(fileName);
            if (!f.exists()) {
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                f.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> readAllLines() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public String readFileAsString() {
        StringBuilder sb = new StringBuilder();
        List<String> lines = readAllLines();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i != lines.size() - 1) sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public void writeAllLines(List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                if (i != lines.size() - 1) bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLine(String text) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            bw.write(text);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void editLine(int indexZeroBased, String newText) {
        List<String> lines = readAllLines();
        if (indexZeroBased >= 0 && indexZeroBased < lines.size()) {
            lines.set(indexZeroBased, newText);
            writeAllLines(lines);
        }
    }

    public void deleteLine(int indexZeroBased) {
        List<String> lines = readAllLines();
        if (indexZeroBased >= 0 && indexZeroBased < lines.size()) {
            lines.remove(indexZeroBased);
            writeAllLines(lines);
        }
    }

    public int getLineCount() {
        return readAllLines().size();
    }
}
