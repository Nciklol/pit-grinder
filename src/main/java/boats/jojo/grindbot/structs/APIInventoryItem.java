package boats.jojo.grindbot.structs;

public class APIInventoryItem {
    private String item;
    private int stack;

    public APIInventoryItem() {};
    public APIInventoryItem(String _itemName, int _stack) {
        item = _itemName;
        stack = _stack;
    }


    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getStack() {
        return stack;
    }

    public void setStack(int stack) {
        this.stack = stack;
    }
}
