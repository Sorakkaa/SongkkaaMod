public class TestRegex {
    public static void main(String[] args) {
        String text = "[BOSS] The Watcher: \u00A7cLet's\u00A0see\u00A0how\u00A0you\u00A0can\u00A0handle\u00A0this.";
        System.out.println("Original: " + text);
        
        String cleanText = text.replaceAll("\u00A7.", "");
        String justLetters = cleanText.replaceAll("[^a-zA-Z]", " ").replaceAll(" +", " ").toLowerCase();
        System.out.println("Result: " + justLetters);
        
        System.out.println("Matches 'how you can handle this': " + justLetters.contains("how you can handle this"));
        System.out.println("Matches 'handle this': " + justLetters.contains("handle this"));
    }
}
