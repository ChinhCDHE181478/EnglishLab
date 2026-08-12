package fu.sep490.g23.backend.dto.response.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryEntryResponse {
    private String word;
    private String phonetic;
    private String audioUrl;
    private List<Meaning> meanings;
    private String meaningVietnamese;
    private boolean vietnameseMeaningAvailable;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meaning {
        private String partOfSpeech;
        private List<Definition> definitions;
        private List<String> synonyms;
        private List<String> antonyms;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Definition {
        private String definition;
        private String example;
    }
}
