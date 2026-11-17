package de.leon.frosty;

public final class FrostyText {
    private static final String FROM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String TO = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    private FrostyText() {}
    public static String sc(String input) {
        StringBuilder out = new StringBuilder();
        for (char c : input.toCharArray()) {
            int i = FROM.indexOf(Character.toUpperCase(c));
            if (i >= 0) out.append(TO.charAt(i));
            else if (c == 'ä' || c == 'Ä') out.append("ᴀ̈");
            else if (c == 'ö' || c == 'Ö') out.append("ᴏ̈");
            else if (c == 'ü' || c == 'Ü') out.append("ᴜ̈");
            else if (c == 'ß') out.append("ꜱꜱ");
            else out.append(c);
        }
        return out.toString();
    }
    public static String tr(String english, String german) {
        return sc("de".equals(FrostyConfig.language) ? german : english);
    }
}
