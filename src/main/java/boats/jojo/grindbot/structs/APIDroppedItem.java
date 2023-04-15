package boats.jojo.grindbot.structs;

public class APIDroppedItem {
    private APIVec3 pos;

    private String name;

    public APIVec3 getPos() {
        return pos;
    }

    public void setPos(APIVec3 pos) {
        this.pos = pos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
