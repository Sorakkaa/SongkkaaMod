
public class TestRegex2 {
    public static void main(String[] args) {
        String text = "-----------------------------------------------------\\n\\n                           The Catacombs - F1\\n\\n                       Defeated Bonzo in 03m 22s\\n                       Score: 215 (B)    +11 Bits\\n                   +119 Cata EXP  +108.8 Tank EXP\\n                                 2,68M-93-0\\n                                1-0 / 1-0-0\\n                                     Solo\\n\\n-----------------------------------------------------";
        String clean = text.replaceAll("(?i)\\u00A7[0-9a-fk-or]", "").trim();
        System.out.println("Clean text matches: " + (clean.contains("Defeated ") && clean.contains(" in ")));
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})m\\s*(\\d{1,2})s").matcher(clean);
        System.out.println("Matcher finds: " + m.find());
        if (m.find(0)) {
            System.out.println("Extract time: " + m.group(1) + "m " + m.group(2) + "s");
        }
    }
}

