package flashcards;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Main {

    public enum Action {
        ADD,
        REMOVE,
        IMPORT,
        EXPORT,
        ASK,
        EXIT,
        LOG,
        HARDEST_CARD,
        RESET_STATS
    }

    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static final Map<String, Card> flashcards = new LinkedHashMap<>();
    private static final StringBuffer logBuffer = new StringBuffer();

    public static void main(String[] args) throws IOException {
        String importFileName = null;
        String exportFileName = null;
        if (args.length > 0) {
            for (int i = 0; i < args.length; i += 2) {
                switch (args[i]) {
                    case "-import":
                        importFileName = args[i + 1];
                        break;

                    case "-export":
                        exportFileName = args[i + 1];
                        break;
                }
            }
        }

        Action action;

        if (importFileName != null) {
            downloadCards(importFileName);
        }

        do {
            println("Input the action (add, remove, import, export, ask, exit):");
            action = Action.valueOf(readLine().replaceAll("\\s", "_").toUpperCase(Locale.ROOT));
            switch (action) {
                case ADD:
                    addCard();
                    break;

                case REMOVE:
                    removeCard();
                    break;

                case IMPORT:
                    importCards();
                    break;

                case EXPORT:
                    exportCards();
                    break;

                case ASK:
                    checkCards();
                    break;

                case LOG:
                    log();
                    break;

                case HARDEST_CARD:
                    hardestCard();
                    break;

                case RESET_STATS:
                    resetStats();
                    break;
            }

            if (!Action.EXIT.equals(action)) println();
        } while (!action.equals(Action.EXIT));

        println("Bye bye!");

        if (exportFileName != null) {
            uploadCards(exportFileName);
        }
    }

    public static void resetStats() {
        flashcards.values().forEach(Card::resetErrors);
        println("Card statistics have been reset");
    }

    private static void log() throws IOException{
        println("File name:");
        String fileName = readLine();
        try (PrintWriter printWriter = new PrintWriter(fileName)) {
            printWriter.print(logBuffer);
            println("The log has been saved.");
        }
    }

    private static String readLine() throws IOException{
        String input = reader.readLine();
        logBuffer.append(input).append("\n");
        return input;
    }

    private static void println() {
        logBuffer.append("\n");
        System.out.println();
    }

    private static void println(String output) {
        logBuffer.append(output).append("\n");
        System.out.println(output);
    }

    private static void printf(String output, Object... args) {
        String formattedOutput = String.format(output, args);
        logBuffer.append(formattedOutput);
        System.out.print(formattedOutput);
    }

    private static void hardestCard() {
        if (flashcards.values().stream().allMatch(card -> card.getErrors() == 0)) {
            println("There are no cards with errors.");
            return;
        }

        List<Card> hardestCards = flashcards
                .values()
                .stream()
                .collect(Collectors.groupingBy(Card::getErrors, TreeMap::new, Collectors.toList()))
                .lastEntry()
                .getValue();

        if (hardestCards.size() == 1) {
            Card hardestCard = hardestCards.get(0);
            printf(
                    "The hardest card is \"%s\". You have %d errors answering it.\n",
                    hardestCard.getTerm(),
                    hardestCard.getErrors()
            );
            return;
        }

        StringBuilder cardsBuilder = new StringBuilder();
        cardsBuilder.append(String.format("\"The hardest cards are \"%s\"", hardestCards.get(0).getTerm()));
        for (int i = 1; i < hardestCards.size(); i++) {
            cardsBuilder.append(String.format(", \"%s\"", hardestCards.get(i).getTerm()));
        }
        cardsBuilder.append(String.format(". You have %d errors answering them.", hardestCards.get(0).getErrors()));
        println(cardsBuilder.toString());
    }

    private static void exportCards() throws IOException {
        println("File name:");
        String fileName = readLine();

        uploadCards(fileName);
    }

    private static void uploadCards(String fileName) throws FileNotFoundException {
        File file = new File(fileName);
        try (PrintWriter printWriter = new PrintWriter(file)) {
            flashcards.forEach((key, value) -> printWriter.printf("%s : %s : %d%n", key, value.getDefinition(), value.getErrors()));
            printf("%d cards have been saved.\n", flashcards.size());
        }
    }

    private static void importCards() throws IOException {
        println("File name:");

        String fileName = readLine();

        if (Files.exists(Paths.get(fileName))) {
            downloadCards(fileName);
        } else {
            println("File not found.");
        }
    }

    private static void downloadCards(String fileName) throws IOException {
        try (BufferedReader fileReader = Files.newBufferedReader(Paths.get(fileName))) {
            List<String> lines = fileReader.lines().collect(Collectors.toList());
            for (String line : lines) {
                String[] buf = line.trim().split("\\s+:\\s");
                String term = buf[0];
                String definition = buf[1];
                int errors = Integer.parseInt(buf[2]);

                if (flashcards.containsKey(term)) {
                    Card cardToChange = flashcards.get(term);
                    cardToChange.setDefinition(definition);
                    cardToChange.setErrors(errors);
                } else {
                    flashcards.put(term, new Card(term, definition, errors));
                }

            }

            printf("%d cards have been loaded.\n", lines.size());
        }
    }

    private static void checkCards() throws IOException {
        println("How many times to ask?");
        int questions = Integer.parseInt(readLine());

        List<Entry<String, Card>> cards = new ArrayList<>(flashcards.entrySet());
        Collections.shuffle(cards);

        for (int i = 0; i < questions; i++) {

            Card card = cards.get(i % cards.size()).getValue();
            printf("Print the definition of \"%s\":\n", card.getTerm());

            String answer = readLine();

            if (card.getDefinition().equals(answer)) {
                println("Correct!");
            } else
            {
                card.addError();

                if (flashcards
                        .values()
                        .stream()
                        .anyMatch(flashcard -> flashcard.getDefinition().equals(answer))) {
                    String answerTerm = flashcards
                            .keySet()
                            .stream()
                            .filter(key -> flashcards.get(key).getDefinition().equals(answer))
                            .findFirst()
                            .get();
                    printf("Wrong. The right answer is \"%s\", but your definition is correct for \"%s\".\n", card.getDefinition(), answerTerm);
                } else {
                    printf("Wrong. The right answer is \"%s\".\n", card.getDefinition());
                }
            }
        }
    }

    private static void removeCard() throws IOException {
        println("Which card?");
        String card = readLine();

        if(flashcards.containsKey(card)) {
            flashcards.remove(card);
            println("The card has been removed.");
        } else {
            printf("Can't remove \"%s\": there is no such card\n", card);
        }
    }

    private static void addCard() throws IOException {
        println("The card:");
        String term = readLine();

        if (flashcards.containsKey(term)) {
            printf("The card \"%s\" already exists.\n", term);
            return;
        }

        println("The definition of the card:");
        String definition = readLine();

        if (flashcards.values().stream().anyMatch(card -> card.getDefinition().equals(definition))) {
            printf("The definition \"%s\" already exists.\n", definition);
            return;
        }

        flashcards.put(term, new Card(term, definition));
        printf("The pair (\"%s\":\"%s\") has been added.\n", term, definition);
    }


}
