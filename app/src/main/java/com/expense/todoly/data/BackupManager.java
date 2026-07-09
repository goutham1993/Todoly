package com.expense.todoly.data;

import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Serializes and parses a full Todoly backup (all categories + todos) in either a single
 * JSON document or a zip bundle containing {@code categories.csv} and {@code todos.csv}.
 */
public final class BackupManager {

    public static final int BACKUP_VERSION = 4;

    private static final String CATEGORIES_CSV = "categories.csv";
    private static final String TODOS_CSV = "todos.csv";

    private static final String CATEGORY_HEADER = "id,name,colorHex,sortOrder,createdAt";
    private static final String TODO_HEADER =
            "id,categoryId,title,notes,isCompleted,important,quick,weekend,weekday,timesensitive,today,tomorrow,createdAt,completedAt,sortOrder";

    private BackupManager() {
    }

    public static final class BackupData {
        public final List<Category> categories;
        public final List<Todo> todos;

        public BackupData(List<Category> categories, List<Todo> todos) {
            this.categories = categories;
            this.todos = todos;
        }
    }

    // ---------------------------------------------------------------- JSON --

    public static void writeJson(OutputStream out, List<Category> categories, List<Todo> todos)
            throws IOException, JSONException {
        JSONObject root = new JSONObject();
        root.put("version", BACKUP_VERSION);

        JSONArray catArray = new JSONArray();
        for (Category c : categories) {
            JSONObject o = new JSONObject();
            o.put("id", c.id);
            o.put("name", c.name);
            o.put("colorHex", c.colorHex);
            o.put("sortOrder", c.sortOrder);
            o.put("createdAt", c.createdAt);
            catArray.put(o);
        }
        root.put("categories", catArray);

        JSONArray todoArray = new JSONArray();
        for (Todo t : todos) {
            JSONObject o = new JSONObject();
            o.put("id", t.id);
            o.put("categoryId", t.categoryId);
            o.put("title", t.title);
            o.put("notes", t.notes == null ? JSONObject.NULL : t.notes);
            o.put("isCompleted", t.isCompleted);
            o.put("important", t.important);
            o.put("quick", t.quick);
            o.put("weekend", t.weekend);
            o.put("weekday", t.weekday);
            o.put("timesensitive", t.timesensitive);
            o.put("today", t.today);
            o.put("tomorrow", t.tomorrow);
            o.put("createdAt", t.createdAt);
            o.put("completedAt", t.completedAt);
            o.put("sortOrder", t.sortOrder);
            todoArray.put(o);
        }
        root.put("todos", todoArray);

        out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public static BackupData readJson(InputStream in) throws IOException, JSONException {
        String content = readFully(in);
        JSONObject root = new JSONObject(content);

        List<Category> categories = new ArrayList<>();
        JSONArray catArray = root.optJSONArray("categories");
        if (catArray != null) {
            for (int i = 0; i < catArray.length(); i++) {
                JSONObject o = catArray.getJSONObject(i);
                Category c = new Category(
                        o.getString("name"),
                        o.getString("colorHex"),
                        o.optInt("sortOrder", i),
                        o.optLong("createdAt", System.currentTimeMillis()));
                c.id = o.getLong("id");
                categories.add(c);
            }
        }

        List<Todo> todos = new ArrayList<>();
        JSONArray todoArray = root.optJSONArray("todos");
        if (todoArray != null) {
            for (int i = 0; i < todoArray.length(); i++) {
                JSONObject o = todoArray.getJSONObject(i);
                String notes = o.isNull("notes") ? null : o.optString("notes", null);
                Todo t = new Todo(
                        o.getLong("categoryId"),
                        o.getString("title"),
                        notes,
                        o.optLong("createdAt", System.currentTimeMillis()),
                        o.optInt("sortOrder", i));
                t.id = o.getLong("id");
                t.isCompleted = o.optBoolean("isCompleted", false);
                t.important = o.optBoolean("important", false);
                t.quick = o.optBoolean("quick", false);
                t.weekend = o.optBoolean("weekend", false);
                t.weekday = o.optBoolean("weekday", false);
                t.timesensitive = o.optBoolean("timesensitive", false);
                t.today = o.optBoolean("today", false);
                t.tomorrow = o.optBoolean("tomorrow", false);
                t.completedAt = o.optLong("completedAt", 0L);
                todos.add(t);
            }
        }
        return new BackupData(categories, todos);
    }

    // ----------------------------------------------------------------- CSV --

    public static void writeCsvZip(OutputStream out, List<Category> categories, List<Todo> todos)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(CATEGORIES_CSV));
            zip.write(buildCategoriesCsv(categories).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry(TODOS_CSV));
            zip.write(buildTodosCsv(todos).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    public static BackupData readCsvZip(InputStream in) throws IOException {
        String categoriesCsv = null;
        String todosCsv = null;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                String content = readFully(zip);
                if (name.endsWith(CATEGORIES_CSV)) {
                    categoriesCsv = content;
                } else if (name.endsWith(TODOS_CSV)) {
                    todosCsv = content;
                }
                zip.closeEntry();
            }
        }
        if (categoriesCsv == null && todosCsv == null) {
            throw new IOException("Zip does not contain categories.csv or todos.csv");
        }
        return new BackupData(parseCategoriesCsv(categoriesCsv), parseTodosCsv(todosCsv));
    }

    private static String buildCategoriesCsv(List<Category> categories) {
        StringBuilder sb = new StringBuilder(CATEGORY_HEADER).append('\n');
        for (Category c : categories) {
            sb.append(c.id).append(',')
                    .append(csv(c.name)).append(',')
                    .append(csv(c.colorHex)).append(',')
                    .append(c.sortOrder).append(',')
                    .append(c.createdAt).append('\n');
        }
        return sb.toString();
    }

    private static String buildTodosCsv(List<Todo> todos) {
        StringBuilder sb = new StringBuilder(TODO_HEADER).append('\n');
        for (Todo t : todos) {
            sb.append(t.id).append(',')
                    .append(t.categoryId).append(',')
                    .append(csv(t.title)).append(',')
                    .append(csv(t.notes)).append(',')
                    .append(t.isCompleted ? 1 : 0).append(',')
                    .append(t.important ? 1 : 0).append(',')
                    .append(t.quick ? 1 : 0).append(',')
                    .append(t.weekend ? 1 : 0).append(',')
                    .append(t.weekday ? 1 : 0).append(',')
                    .append(t.timesensitive ? 1 : 0).append(',')
                    .append(t.today ? 1 : 0).append(',')
                    .append(t.tomorrow ? 1 : 0).append(',')
                    .append(t.createdAt).append(',')
                    .append(t.completedAt).append(',')
                    .append(t.sortOrder).append('\n');
        }
        return sb.toString();
    }

    private static List<Category> parseCategoriesCsv(String csv) {
        List<Category> result = new ArrayList<>();
        if (csv == null) return result;
        List<List<String>> rows = parseCsv(csv);
        for (int i = 1; i < rows.size(); i++) {
            List<String> f = rows.get(i);
            if (f.size() < 5) continue;
            Category c = new Category(
                    f.get(1),
                    f.get(2),
                    parseInt(f.get(3), i),
                    parseLong(f.get(4), System.currentTimeMillis()));
            c.id = parseLong(f.get(0), 0L);
            result.add(c);
        }
        return result;
    }

    private static List<Todo> parseTodosCsv(String csv) {
        List<Todo> result = new ArrayList<>();
        if (csv == null) return result;
        List<List<String>> rows = parseCsv(csv);
        for (int i = 1; i < rows.size(); i++) {
            List<String> f = rows.get(i);
            if (f.size() < 12) continue;
            boolean hasTimesensitive = f.size() >= 13;
            boolean hasTodayTomorrow = f.size() >= 15;
            int createdIdx = hasTodayTomorrow ? 12 : (hasTimesensitive ? 10 : 9);
            int completedIdx = hasTodayTomorrow ? 13 : (hasTimesensitive ? 11 : 10);
            int sortIdx = hasTodayTomorrow ? 14 : (hasTimesensitive ? 12 : 11);
            String notes = f.get(3).isEmpty() ? null : f.get(3);
            Todo t = new Todo(
                    parseLong(f.get(1), 0L),
                    f.get(2),
                    notes,
                    parseLong(f.get(createdIdx), System.currentTimeMillis()),
                    parseInt(f.get(sortIdx), i));
            t.id = parseLong(f.get(0), 0L);
            t.isCompleted = "1".equals(f.get(4));
            t.important = "1".equals(f.get(5));
            t.quick = "1".equals(f.get(6));
            t.weekend = "1".equals(f.get(7));
            t.weekday = "1".equals(f.get(8));
            t.timesensitive = hasTimesensitive && "1".equals(f.get(9));
            t.today = hasTodayTomorrow && "1".equals(f.get(10));
            t.tomorrow = hasTodayTomorrow && "1".equals(f.get(11));
            t.completedAt = parseLong(f.get(completedIdx), 0L);
            result.add(t);
        }
        return result;
    }

    // ------------------------------------------------------------- helpers --

    private static String csv(String value) {
        if (value == null) return "";
        boolean needsQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }

    /** Parses RFC-4180 style CSV text into rows of fields. */
    private static List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                } else if (ch == '\n') {
                    current.add(field.toString());
                    field.setLength(0);
                    rows.add(current);
                    current = new ArrayList<>();
                } else if (ch == '\r') {
                    // ignore; handled by following \n
                } else {
                    field.append(ch);
                }
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current);
        }
        return rows;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
