package com.github.mirum8.jnscli.build.parameters.activechoises;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ActiveChoiceExtractor {

    private static final Pattern REF_PARAM_PATTERN = Pattern.compile("referencedParameters\\.push\\(\"(\\w+)\"\\)");
    private static final Pattern UID_PATTERN_V1 = Pattern.compile("/\\$stapler/bound/script/\\$stapler/bound/([\\w-]+)");
    private static final Pattern UID_PATTERN_V2 = Pattern.compile("makeStaplerProxy\\('/\\$stapler/bound/([\\w-]+)'");
    private static final Pattern CRUMBS_PATTERN = Pattern.compile("makeStaplerProxy\\('/\\$stapler/bound/[\\w-]+','([\\w]+)'");

    public ActiveChoice getActiveChoice(String parameterName, String html) {
        return extractActiveChoice(parameterName, html)
            .or(() -> extractActiveChoiceLegacy(parameterName, html))
            .orElse(ActiveChoice.NOT_FOUND);
    }

    private Optional<ActiveChoice> extractActiveChoice(String parameterName, String html) {
        return findScriptElement(parameterName, html)
            .flatMap(scriptElement -> {
                String uid = extractUid(scriptElement.toString());
                if (uid == null) {
                    return Optional.empty();
                }
                Element refScript = scriptElement.nextElementSibling();
                List<String> referencedParameters = refScript == null ? List.of() : extractReferencedParameters(refScript.toString());
                return Optional.of(new ActiveChoice(uid, referencedParameters, false, null));
            });
    }

    private Optional<ActiveChoice> extractActiveChoiceLegacy(String parameterName, String html) {
        return findScriptElement(parameterName, html)
            .flatMap(scriptElement -> {
                String scriptContent = scriptElement.html();
                String uid = extractUidLegacy(scriptContent);
                if (uid == null) {
                    return Optional.empty();
                }
                List<String> referencedParameters = extractReferencedParameters(scriptContent);
                return Optional.of(new ActiveChoice(uid, referencedParameters, true, extractCrumbsLegacy(scriptContent)));
            });
    }

    private Optional<Element> findScriptElement(String parameterName, String html) {
        Document doc = Jsoup.parse(html);
        Element activeChoiceDiv = doc.selectFirst("div.active-choice:has(input[name=name][value=\"" + escapeForCssAttr(parameterName) + "\"])");
        if (activeChoiceDiv == null) {
            return Optional.empty();
        }
        Element grandparent = parentOrNull(parentOrNull(activeChoiceDiv));
        if (grandparent == null) {
            return Optional.empty();
        }
        Element scriptElement = grandparent.nextElementSibling();
        if (scriptElement == null || !scriptElement.tagName().equals("script")) {
            return Optional.empty();
        }
        return Optional.of(scriptElement);
    }

    private static Element parentOrNull(Element element) {
        return element == null ? null : element.parent();
    }

    private String extractCrumbsLegacy(String scriptContent) {
        Matcher crumbsMatcher = CRUMBS_PATTERN.matcher(scriptContent);
        return crumbsMatcher.find() ? crumbsMatcher.group(1) : null;
    }

    private String extractUid(String scriptSrc) {
        Matcher uidMatcher = UID_PATTERN_V1.matcher(scriptSrc);
        return uidMatcher.find() ? uidMatcher.group(1) : null;
    }

    private String extractUidLegacy(String scriptContent) {
        Matcher uidMatcher = UID_PATTERN_V2.matcher(scriptContent);
        return uidMatcher.find() ? uidMatcher.group(1) : null;
    }

    private static String escapeForCssAttr(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<String> extractReferencedParameters(String scriptContent) {
        List<String> referencedParameters = new ArrayList<>();
        Matcher refParamMatcher = REF_PARAM_PATTERN.matcher(scriptContent);
        while (refParamMatcher.find()) {
            referencedParameters.add(refParamMatcher.group(1));
        }
        return referencedParameters;
    }
}
