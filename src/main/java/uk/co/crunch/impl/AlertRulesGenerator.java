package uk.co.crunch.impl;

import uk.co.crunch.api.AlertRule;
import uk.co.crunch.api.AlertRule.Annotation;
import uk.co.crunch.api.AlertRule.Label;
import uk.co.crunch.api.PrometheusVersion;
import uk.co.crunch.impl.v1x.AlertRulesGenerator1x;
import uk.co.crunch.impl.v2x.AlertRulesGenerator2x;
import uk.co.crunch.utils.PrometheusUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class AlertRulesGenerator {

    public static String buildRulesFile(final PrometheusVersion version, final String metricPrefix, final String alertGroupName, final AlertRule... rules) {
        return version == PrometheusVersion.V2_X ? AlertRulesGenerator2x.buildRulesFile(metricPrefix, alertGroupName, rules) : AlertRulesGenerator1x.buildRulesFile(metricPrefix, rules);
    }

    public static String replaceRulePlaceholders(final AlertRule rule, final String normalisedPrefix) {
        String ruleStr = rule.rule();
        for (int i = 0; i < rule.metricNames().length; ++i) {
            final String rawName = rule.metricNames()[i];
            final String missingPrefix = rawName.startsWith(normalisedPrefix) ? "" : normalisedPrefix;

            ruleStr = ruleStr.replace("$" + (i + 1), missingPrefix + PrometheusUtils.normaliseName(rawName));
        }
        return ruleStr;
    }

    public static Map<String,String> getLabels(final AlertRule rule) {
        final Map<String,String> labels = new LinkedHashMap<>();
        labels.put("severity", rule.severity().toString().toLowerCase());

        for (Label label : rule.labels()) {
            labels.putIfAbsent(label.name(), label.value());
        }

        return labels;
    }

    public static Map<String,String> getAnnotations(final AlertRule rule) {
        final Map<String,String> anns = new LinkedHashMap<>();
        anns.put("summary", rule.summary());
        anns.put("description", rule.description());

        if (rule.confluenceLink().startsWith("/")) {
            anns.put("confluence_link", "https://crunch.atlassian.net/wiki/spaces" + rule.confluenceLink());
        } else {
            anns.put("confluence_link", rule.confluenceLink());
        }

        for (Annotation ann : rule.annotations()) {
            anns.putIfAbsent(ann.name(), ann.value());
        }

        return anns;
    }
}
