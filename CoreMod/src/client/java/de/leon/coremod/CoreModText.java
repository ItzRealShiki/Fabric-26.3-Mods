package de.leon.coremod;

public final class CoreModText {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    private CoreModText() {}

    public static String smallCaps(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            int index = LOWER.indexOf(Character.toLowerCase(c));
            out.append(index >= 0 ? SMALL.charAt(index) : c);
        }
        return out.toString();
    }
}
