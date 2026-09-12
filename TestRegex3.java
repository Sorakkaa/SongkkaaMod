
public class TestRegex3 {
    public static void main(String[] args) {
        String text = "Defeated\u00A0Bonzo\u00A0in\u00A003m";
        System.out.println("Contains space? " + text.contains("Defeated "));
    }
}

