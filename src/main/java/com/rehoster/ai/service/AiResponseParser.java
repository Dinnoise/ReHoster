package com.rehoster.ai.service;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.rehoster.ai.model.AiChange;
import com.rehoster.ai.model.AiRefinementResult;

public class AiResponseParser {
    private final Gson gson;

    public AiResponseParser() {
        this.gson = new Gson();
    }

    public AiRefinementResult parse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return null;
        }

        String candidate = extractJsonObject(rawResponse.trim());
        try {
            JsonObject root = JsonParser.parseString(candidate).getAsJsonObject();
            JsonObject normalized = normalize(root);
            return gson.fromJson(normalized, AiRefinementResult.class);
        } catch (JsonSyntaxException e) {
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private JsonObject normalize(JsonObject root) {
        JsonObject normalized = root.deepCopy();

        if (!normalized.has("dockerCompose") && normalized.has("compose")) {
            normalized.add("dockerCompose", normalized.get("compose"));
        }

        if (normalized.has("summary") && normalized.get("summary").isJsonPrimitive()) {
            JsonArray summaryArray = new JsonArray();
            summaryArray.add(normalized.get("summary").getAsString());
            normalized.add("summary", summaryArray);
        }

        if (normalized.has("appliedChanges") && normalized.get("appliedChanges").isJsonArray()) {
            JsonArray inputChanges = normalized.getAsJsonArray("appliedChanges");
            JsonArray outputChanges = new JsonArray();
            for (JsonElement changeElement : inputChanges) {
                if (changeElement == null || changeElement.isJsonNull()) {
                    continue;
                }
                if (changeElement.isJsonObject()) {
                    JsonObject changeObject = changeElement.getAsJsonObject();
                    if (!changeObject.has("target")) {
                        changeObject.addProperty("target", "artifacts");
                    }
                    if (!changeObject.has("type")) {
                        changeObject.addProperty("type", "update");
                    }
                    if (!changeObject.has("reason")) {
                        changeObject.addProperty("reason", "AI suggested change");
                    }
                    outputChanges.add(changeObject);
                } else if (changeElement.isJsonPrimitive()) {
                    AiChange change = new AiChange();
                    change.setTarget("artifacts");
                    change.setType("update");
                    change.setReason(changeElement.getAsString());
                    outputChanges.add(gson.toJsonTree(change));
                }
            }
            normalized.add("appliedChanges", outputChanges);
        }

        if (normalized.has("warnings") && normalized.get("warnings").isJsonPrimitive()) {
            JsonArray warningsArray = new JsonArray();
            warningsArray.add(normalized.get("warnings").getAsString());
            normalized.add("warnings", warningsArray);
        }

        if (normalized.has("confidence")) {
            JsonElement confEl = normalized.get("confidence");
            if (confEl.isJsonPrimitive() && confEl.getAsJsonPrimitive().isString()) {
                String confStr = confEl.getAsString().trim().toLowerCase();
                double confValue;
                switch (confStr) {
                    case "high":    confValue = 0.9; break;
                    case "medium":  confValue = 0.6; break;
                    case "low":     confValue = 0.3; break;
                    default:
                        try {
                            confValue = Double.parseDouble(confStr);
                        } catch (NumberFormatException e) {
                            confValue = 0.5;
                        }
                        break;
                }
                normalized.addProperty("confidence", confValue);
            }
        }

        if (!normalized.has("summary")) {
            normalized.add("summary", gson.toJsonTree(new ArrayList<String>()));
        }
        if (!normalized.has("warnings")) {
            normalized.add("warnings", gson.toJsonTree(new ArrayList<String>()));
        }
        if (!normalized.has("appliedChanges")) {
            normalized.add("appliedChanges", gson.toJsonTree(new ArrayList<AiChange>()));
        }

        return normalized;
    }

    private String extractJsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }
}
