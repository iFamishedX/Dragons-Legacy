package dev.dragonslegacy;

class Perms {
    public static final String BASE = "deg";
    public static final String ADMIN = child(BASE, "admin");
    public static final String MOD_INFO = child(ADMIN, "info");
    public static final String RELOAD = child(ADMIN, "reload");
    public static final String BEARER = child(BASE, "bearer");
    public static final String INFO = child(BASE, "info");
    public static final String HELP = child(BASE, "help");
    public static final String PLACEHOLDERS = child(BASE, "placeholders");
    public static final String EXACT_POS_PLACEHOLDER = child(PLACEHOLDERS, "exact_pos");
    public static final String RANDOMIZED_POS_PLACEHOLDER = child(PLACEHOLDERS, "randomized_pos");

    public static String child(String node, String childName) {
        return node + "." + childName;
    }

    public static String name(String node) {
        return node.substring(node.lastIndexOf('.') + 1);
    }

    public static String wildcard(String node) {
        return node + ".*";
    }
}