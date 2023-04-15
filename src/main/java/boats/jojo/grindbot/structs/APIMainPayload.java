package boats.jojo.grindbot.structs;

import java.util.List;

public class APIMainPayload {
    private  String api_key;
    private String player_name;
    private String uuid;
    private APIVec3 position;
    private double pitch;
    private double yaw;
    private List<APIInventoryItem> inventory;
    private List<APIPlayer> players;
    private String middle_block;
    private List<String> last_messages;
    private List<APIContainerItem> container;
    private List<APIDroppedItem> dropped_items;
    private String important_chat_msg;
    private String current_open_gui;
    private List<APIVec3>  villager_positions;
    private double health;
    private double xp_level;
    private String version;

    public String getApiKey() {
        return api_key;
    }

    public void setApiKey(String api_key) {
        this.api_key = api_key;
    }

    public String getPlayerName() {
        return player_name;
    }

    public void setPlayerName(String player_name) {
        this.player_name = player_name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public APIVec3 getPosition() {
        return position;
    }

    public void setPosition(APIVec3 pos) {
        this.position = pos;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public List<APIInventoryItem> getInventory() {
        return inventory;
    }

    public void setInventory(List<APIInventoryItem> inventory) {
        this.inventory = inventory;
    }

    public List<APIPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<APIPlayer> players) {
        this.players = players;
    }

    public String getMiddleBlock() {
        return middle_block;
    }

    public void setMiddleBlock(String middle_block) {
        this.middle_block = middle_block;
    }

    public List<String> getLastMessages() {
        return last_messages;
    }

    public void setLastMessages(List<String> last_messages) {
        this.last_messages = last_messages;
    }

    public List<APIContainerItem> getContainer() {
        return container;
    }

    public void setContainer(List<APIContainerItem> container) {
        this.container = container;
    }

    public List<APIDroppedItem> getDroppedItems() {
        return dropped_items;
    }

    public void setDroppedItems(List<APIDroppedItem> dropped_items) {
        this.dropped_items = dropped_items;
    }

    public String getImportantChatMsg() {
        return important_chat_msg;
    }

    public void setImportantChatMsg(String important_chat_msg) {
        this.important_chat_msg = important_chat_msg;
    }

    public String getCurrentOpenGui() {
        return current_open_gui;
    }

    public void setCurrentOpenGui(String current_open_gui) {
        this.current_open_gui = current_open_gui;
    }

    public List<APIVec3> getVillagerPositions() {
        return villager_positions;
    }

    public void setVillagerPositions(List<APIVec3> villager_positions) {
        this.villager_positions = villager_positions;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getXp_level() {
        return xp_level;
    }

    public void setXpLevel(double xp_level) {
        this.xp_level = xp_level;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
