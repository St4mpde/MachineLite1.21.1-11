package com.lite.machinelite.module.impl;

import com.lite.machinelite.event.Event;
import com.lite.machinelite.event.impl.ChatInputEvent;
import com.lite.machinelite.module.Module;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

public class AntiSpam extends Module {
    private static final float SIMILARITY_THRESHOLD = 0.80f;
    private static final Pattern INVISIBLE_CHARS =
        Pattern.compile("[\u200B\u200C\u200D\u00AD\uFEFF\u2060]");
    private static final Pattern COUNTER_PATTERN =
        Pattern.compile("(?i)\\s*(\u00A78)?\\s*\\[[×x]\\d+\\]$");

    public AntiSpam(String name, int keyCode) {
        super(name, keyCode);
    }

    private String getStringFromOrderedText(OrderedText text) {
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private String normalize(String raw) {
        String stripped = COUNTER_PATTERN.matcher(raw).replaceAll("").trim();
        String noInvisible = INVISIBLE_CHARS.matcher(stripped).replaceAll("");
        try {
            return Normalizer.normalize(noInvisible, Normalizer.Form.NFKC).trim();
        } catch (Exception e) {
            return noInvisible.trim();
        }
    }

    private boolean isSpam(String cleanOld, String cleanNew) {
        if (cleanOld.equals(cleanNew)) return true;

        int len1 = cleanOld.length();
        int len2 = cleanNew.length();

        int minLen = Math.min(len1, len2);
        int commonPrefixLen = 0;
        while (commonPrefixLen < minLen
               && cleanOld.charAt(commonPrefixLen) == cleanNew.charAt(commonPrefixLen)) {
            commonPrefixLen++;
        }
        if (commonPrefixLen >= 10) {
            String tailOld = cleanOld.substring(commonPrefixLen);
            String tailNew = cleanNew.substring(commonPrefixLen);
            if (tailOld.length() <= 12 && tailNew.length() <= 12
                    && !tailOld.contains(" ") && !tailNew.contains(" ")) {
                if (commonPrefixLen >= 20 || (containsDigit(tailOld) && containsDigit(tailNew))) {
                    return true;
                }
            }
        }

        if (len1 >= 15 && len2 >= 15) {
            float sim = similarity(cleanOld, cleanNew);
            if (sim >= SIMILARITY_THRESHOLD) return true;
        }

        return false;
    }

    private boolean containsDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private float similarity(String a, String b) {
        int maxLen = 128;
        if (a.length() > maxLen) a = a.substring(0, maxLen);
        if (b.length() > maxLen) b = b.substring(0, maxLen);

        int lenA = a.length(), lenB = b.length();
        int[] prev = new int[lenB + 1];
        int[] curr = new int[lenB + 1];

        for (int j = 0; j <= lenB; j++) prev[j] = j;
        for (int i = 1; i <= lenA; i++) {
            curr[0] = i;
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        int dist = prev[lenB];
        return 1.0f - (float) dist / Math.max(lenA, lenB);
    }

    @Override
    public void onEvent(Event event) {
        if (!isEnabled()) return;
        if (!(event instanceof ChatInputEvent chatEvent)) return;

        try {
            List<ChatHudLine.Visible> chatLines = chatEvent.getChatLines();
            if (chatLines.isEmpty()) return;

            String newRaw = chatEvent.getTextComponent().getString();
            String cleanNew = normalize(newRaw);
            if (cleanNew.isEmpty()) return;

            int spamCounter = 1;
            int searchLimit = Math.max(0, chatLines.size() - 50);

            for (int i = chatLines.size() - 1; i >= searchLimit; --i) {
                String oldRaw = getStringFromOrderedText(chatLines.get(i).content());
                String cleanOld = normalize(oldRaw);

                if (isSpam(cleanOld, cleanNew)) {
                    java.util.regex.Matcher m =
                        Pattern.compile("\\[[×x](\\d+)\\]$").matcher(oldRaw);
                    if (m.find()) {
                        try { spamCounter += Integer.parseInt(m.group(1)); }
                        catch (NumberFormatException ignored) { spamCounter++; }
                    } else {
                        spamCounter++;
                    }
                    chatLines.remove(i);
                    break;
                }
            }

            if (spamCounter > 1) {
                chatEvent.setCancelled(true);
                chatEvent.setTextComponent(
                    Text.literal(newRaw + " \u00A78[\u00D7" + spamCounter + "]")
                );
            }
        } catch (Exception ignored) {}
    }
}
