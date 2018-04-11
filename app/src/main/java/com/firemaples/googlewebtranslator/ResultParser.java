package com.firemaples.googlewebtranslator;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Convert from:
 * https://github.com/matheuss/google-translate-api/blob/master/index.js
 */
public class ResultParser {
    public static TranslatedResult parse(String raw) {
        TranslatedResult result = new TranslatedResult();
        result.raw = raw;
        _parseResultText(result, raw);
        _parseFromLanguage(result, raw);
        _parseFromText(result, raw);
        return result;
    }

    private static void _parseResultText(TranslatedResult result, String raw) {
        try {
            JSONArray mainArray = new JSONArray(raw);

            StringBuilder translatedText = new StringBuilder();
            for (int i = 0; i < mainArray.length(); i++) {
                if (!(mainArray.get(i) instanceof JSONArray)) {
                    break;
                }
                JSONArray arr = mainArray.getJSONArray(i);
                for (int j = 0; j < arr.length(); j++) {
                    if (!(arr.get(j) instanceof JSONArray)) {
                        break;
                    }
                    JSONArray obj = arr.getJSONArray(j);
                    if (obj.length() > 0 && obj.get(0) instanceof String) {
                        if (translatedText.length() > 0) {
                            translatedText.append("\n");
                        }
                        translatedText.append(obj.getString(0));
                    } else {
                        break;
                    }
                }
            }

            result.text = translatedText.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void _parseFromLanguage(TranslatedResult result, String raw) {
        try {
            JSONArray mainArray = new JSONArray(raw);
            if (mainArray.get(8) instanceof JSONArray
                    && mainArray.getJSONArray(8).get(0) instanceof JSONArray) {
                if (mainArray.get(2) instanceof String
                        && mainArray.getJSONArray(8).getJSONArray(0).get(0) instanceof String) {
                    if (mainArray.getString(2).equals(mainArray.getJSONArray(8).getJSONArray(0).getString(0))) {
                        result.fromLanguage_iso = mainArray.getString(2);
                    } else {
                        result.fromLanguage_didYouMean = true;
                        result.fromLanguage_iso = mainArray.getJSONArray(8).getJSONArray(0).getString(0);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void _parseFromText(TranslatedResult result, String raw) {
        try {
            JSONArray mainArray = new JSONArray(raw);
            if (mainArray.get(7) instanceof JSONArray && mainArray.getJSONArray(7).get(0) instanceof String) {
                String str = mainArray.getJSONArray(7).getString(0);

                str = str.replaceAll("<b><i>", "[");
                str = str.replaceAll("</i></b>", "]");

                result.fromText_value = str;

                if (mainArray.getJSONArray(7).get(5) instanceof Boolean && mainArray.getJSONArray(7).getBoolean(5)) {
                    result.fromText_autoCorrected = true;
                } else {
                    result.fromText_didYouMean = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
