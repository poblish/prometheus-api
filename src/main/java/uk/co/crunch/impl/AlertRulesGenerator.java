package uk.co.crunch.impl;

import com.google.common.base.Joiner;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import uk.co.crunch.api.AlertRule;
import uk.co.crunch.utils.PrometheusUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AlertRulesGenerator {

    private final static Joiner COMMAS = Joiner.on(",\n    ");

    public static String buildRulesFile(final String metricPrefix, final AlertRule... rules) {
        final JtwigModel model = JtwigModel.newModel();
        model.with("prefix", metricPrefix);

        final JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/rule_template.rule");

        final List<String> ruleStrs = new ArrayList<>();

        final String normalisedPrefix = PrometheusUtils.normaliseName(metricPrefix) + "_";

        for (AlertRule eachRule : rules) {
            String ruleStr = eachRule.rule();
            for (int i = 0; i < eachRule.metricNames().length; ++i) {
                final String rawName = eachRule.metricNames()[i];
                final String missingPrefix = rawName.startsWith(normalisedPrefix) ? "" : normalisedPrefix;

                ruleStr = ruleStr.replace("$" + (i + 1), missingPrefix + PrometheusUtils.normaliseName(rawName));
            }

            model.with("alertName", eachRule.name());
            model.with("duration", eachRule.duration());
            model.with("rule", ruleStr);

            //////////////////////////////////////////////////////////////////////////////////////////

            final Map<String,String> anns = new LinkedHashMap<>();
            anns.put("summary", eachRule.summary());
            anns.put("description", eachRule.description());

            if (eachRule.confluenceLink().startsWith("/")) {
                anns.put("confluence_link", "https://crunch.atlassian.net/wiki/spaces" + eachRule.confluenceLink());
            } else {
                anns.put("confluence_link", eachRule.confluenceLink());
            }

            for (AlertRule.Annotation ann : eachRule.annotations()) {
                anns.putIfAbsent(ann.name(), ann.value());
            }

            model.with("annotations", entriesMapToString(anns));

            //////////////////////////////////////////////////////////////////////////////////////////

            final Map<String,String> labels = new LinkedHashMap<>();
            labels.put("severity", eachRule.severity().toString().toLowerCase());

            for (AlertRule.Label label : eachRule.labels()) {
                labels.putIfAbsent(label.name(), label.value());
            }

            model.with("labels", entriesMapToString(labels));

            //////////////////////////////////////////////////////////////////////////////////////////

            ruleStrs.add(template.render(model));
        }

        return Joiner.on("\n").join(ruleStrs);
    }

    private static CharSequence quoteString(final String s) {
        return new StringBuilder(s.length() + 2).append("\"").append(s.replace("\"", "\\\"")).append("\"");
    }

    private static Function<Map.Entry<String,String>,String> formatEntry() {
        return entry -> entry.getKey() + " = " + quoteString(entry.getValue());
    }

    private static String entriesMapToString(final Map<String,String> entries) {
        return COMMAS.join(entries.entrySet().stream().map(formatEntry()).collect(Collectors.toList()));
    }
}
