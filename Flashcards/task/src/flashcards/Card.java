package flashcards;

public class Card {
    private int errors = 0;
    private String term;
    private String definition;

    public Card(String term, String definition) {
        this.term = term;
        this.definition = definition;
    }

    public Card(String term, String definition, int errors) {
        this.term = term;
        this.definition = definition;
        this.errors = errors;
    }

    public void addError() {
        errors++;
    }

    public void resetErrors() {
        errors = 0;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getErrors() {
        return errors;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public String toString() {
        return String.format("%s : %s : %d", term, definition, errors);
    }
}
