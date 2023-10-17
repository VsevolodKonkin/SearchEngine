package searchengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    public static LemmaFinder getInstance() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        return new LemmaFinder(morphology);
    }

    private LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public HashMap<String, Integer> collectLemmas(String text) {
        HashMap<String, Integer> wordsMap = new HashMap<>();
        String textCleaned = removeHtmlTags(text);
        Matcher matcher = getMatcherRussianWord(textCleaned);
        while (matcher.find()) {
            String baseWord = getNormalForm(matcher.group());
            if (baseWord.isBlank()) {
                continue;
            }
            if (wordsMap.containsKey(baseWord)) {
                wordsMap.put(baseWord, wordsMap.get(baseWord) + 1);
            } else {
                wordsMap.put(baseWord, 1);
            }
        }
        return wordsMap;
    }

    public  String removeHtmlTags(String htmlText) {
        String regexScriptBlock = "<script[\\s\\S]*?</script>";
        String regexAllTags = "(<[^<>]+>\\s*)+";
        return htmlText.replaceAll(regexScriptBlock, "")
                .replaceAll(regexAllTags, "<\n>");
    }

    public List<Snippet> getSnippetList(String text, Set<String> lemmas) {
        List<Snippet> snippetList = new ArrayList<>();
        String textCleaned = removeHtmlTags(text);
        String[] fragments = textCleaned.split("<\n>");
        for(String fragment: fragments){
            Snippet snippet = new Snippet(fragment);
            Matcher matcher = getMatcherRussianWord(fragment);
            while (matcher.find()){
                String baseWord = getNormalForm(matcher.group());
                if (lemmas.contains(baseWord)) {
                    snippet.getQueryWordsIndexes().put(matcher.start(), matcher.group());
                }
            }
            if (!snippet.getQueryWordsIndexes().isEmpty()){
                snippetList.add(snippet);
            }
        }
        return snippetList;
    }

    public Matcher getMatcherRussianWord(String text) {
        String regexRussianWord = "[а-яёА-ЯЁ]+";
        Pattern pattern = Pattern.compile(regexRussianWord);
        return pattern.matcher(text);
    }

    public String getNormalForm(String word) {
        String wordLowerCase = word.toLowerCase();
        if (!isNormalWord(wordLowerCase)) {
            return "";
        }
        List<String> wordBaseForms = luceneMorphology.getNormalForms(wordLowerCase);
        return wordBaseForms.get(0);
    }

    public boolean isNormalWord(String word) {
        if (word.isBlank()) {
            return false;
        }
        String[] particleNames = new String[]{"ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МЕЖД"};
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        return wordBaseForms.stream().anyMatch(s -> {
            for (String particle : particleNames) {
                if (s.contains(particle)) {
                    return false;
                }
            }
            return true;
        });
    }
}
