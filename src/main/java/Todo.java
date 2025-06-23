public class Todo {
    public int id;
    public String title;
    public String description;
    public boolean done;

    public Todo(int id, String title, String description, boolean done) {
        this.title = title;
        this.description = description;
        this.done = done;
    }
}