package boats.jojo.grindbot.structs;

public class APIPlayer {
    private APIVec3 pos;
    private double health;
    private double armor;
    private String username;

    public APIVec3 getPos() {
        return pos;
    }

    public void setPos(APIVec3 pos) {
        this.pos = pos;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getArmor() {
        return armor;
    }

    public void setArmor(double armor) {
        this.armor = armor;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
