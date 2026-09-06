import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegex {
    private static final Pattern TIME_MS_PATTERN = Pattern.compile("(?:(\\d{1,2})m\\s*)?(\\d{1,2})(?:\\.(\\d+))?s");
    private static final Pattern TIME_COLON_PATTERN = Pattern.compile("(?:\\b|Time:\\s*)(\\d{1,2}):(\\d{2})(?:\\.(\\d+))?\\b");

    public static void main(String[] args) {
        String text1 = "Defeated Maxor, Storm, Goldor, and Necron in 06m 21s";
        String clean1 = text1.replaceAll("(?i)\\u00A7[0-9a-fk-or]", "").replace('\u00A0', ' ').trim();
        
        boolean isDefeated = clean1.matches("(?s).*Defeated.*\\s+in\\s+.*");
        System.out.println("isDefeated: " + isDefeated);
        System.out.println("extractTime(1): " + extractTime(clean1));

        String text2 = "Score: 300 (S+)";
        String clean2 = text2.replaceAll("(?i)\\u00A7[0-9a-fk-or]", "").replace('\u00A0', ' ').trim();
        boolean isScore = clean2.matches("(?i).*(Team Score|Score d'équipe|Score de l'équipe|Score total|Score\\s*:).*");
        System.out.println("isScore: " + isScore);
    }

    public static long extractTime(String text) {
        if (text == null) return -1;
        Matcher m = TIME_MS_PATTERN.matcher(text);
        if (m.find()) {
            try {
                long min = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
                long sec = Long.parseLong(m.group(2));
                long ms = 0;
                if (m.group(3) != null) {
                    String msStr = m.group(3);
                    if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                    else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                    else if (msStr.length() == 3) ms = Long.parseLong(msStr);
                    else ms = Long.parseLong(msStr.substring(0, 3));
                }
                return (min * 60 + sec) * 1000 + ms;
            } catch (Exception ignored) {}
        }
        return -1;
    }
}
