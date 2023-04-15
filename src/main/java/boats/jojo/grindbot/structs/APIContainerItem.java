package boats.jojo.grindbot.structs;

public class APIContainerItem {
    private String item;
    private String display_name;
    private int stack_size;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDisplayName() {
        return display_name;
    }

    public void setDisplayName(String display_item) {
        this.display_name = display_item;
    }

    public int getStackSize() {
        return stack_size;
    }

    public void setStackSize(int stack_size) {
        this.stack_size = stack_size;
    }
}
