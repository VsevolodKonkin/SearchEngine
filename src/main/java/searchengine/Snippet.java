package searchengine;

import lombok.Data;
import java.util.*;
@Data
public class Snippet {
    private String text;
    private TreeMap<Integer, String> queryWordsIndexes;
    private Set<String> searchWords;

    public Snippet(String text, Set<String> searchWords) {
        this.text = text;
        queryWordsIndexes = new TreeMap<>();
        this.searchWords = searchWords;
    }

    public String getFormattedText(int length) {
        if (text.length() <= length) {
            return getCutBoldText(0, text.length());
        }
        List<Integer> searchWordIndexes = new ArrayList<>();
        for (String searchWord : searchWords) {
            int index = text.indexOf(searchWord);
            if (index != -1) {
                searchWordIndexes.add(index);
            }
        }
        if (searchWordIndexes.isEmpty()) {
            return "";
        }
        Collections.sort(searchWordIndexes);
        int centralIndex = searchWordIndexes.get(searchWordIndexes.size() / 2);
        int halfLength = length / 2;
        int beginIndex = Math.max(0, centralIndex - halfLength);
        int endIndex = Math.min(text.length(), centralIndex + halfLength);
        return getCutBoldText(beginIndex, endIndex);
    }

    private String getCutBoldText(int beginIndex, int endIndex) {
        String formattedText = text.substring(Math.max(0, beginIndex), Math.min(text.length(), endIndex));
        Map.Entry<Integer, String> entry = queryWordsIndexes.lastEntry();
        while (entry != null) {
            int entryKey = entry.getKey();
            if (entryKey >= beginIndex && entryKey + entry.getValue().length() <= endIndex) {
                int startIndex = Math.max(0, entryKey - beginIndex);
                int endIndexInSnippet = entryKey - beginIndex + entry.getValue().length();
                formattedText = formattedText.substring(0, startIndex)
                        .concat("<b>")
                        .concat(entry.getValue())
                        .concat("</b>")
                        .concat(formattedText.substring(endIndexInSnippet));
            }
            entry = queryWordsIndexes.lowerEntry(entryKey);
        }
        return formattedText;
    }
}
