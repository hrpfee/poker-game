public enum PlayerActionType {
    CHECK("check"),
    CALL("call"),
    RAISE("raise"),
    FOLD("fold");

    private final String command;

    PlayerActionType(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public static PlayerActionType fromCommand(String command) {
        for (PlayerActionType type : values()) {
            if (type.command.equalsIgnoreCase(command)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid action");
    }
}
