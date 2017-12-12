package uk.co.crunch.impl.v1x;

import com.google.common.base.Joiner;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import uk.co.crunch.api.AlertRule;
import uk.co.crunch.utils.PrometheusUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.co.crunch.impl.AlertRulesGenerator.*;

public class AlertRulesGenerator1x {
    private final static Joiner COMMAS = Joiner.on(",\n    ");

    public static String buildRulesFile(final String metricPrefix, final AlertRule... rules) {
        final JtwigModel model = JtwigModel.newModel();
        model.with("prefix", metricPrefix);

        final JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/rule_template_1.x.rule");

        final List<String> ruleStrs = new ArrayList<>();

        final String normalisedPrefix = PrometheusUtils.normaliseName(metricPrefix) + "_";

        for (AlertRule eachRule : rules) {
            model.with("alertName", eachRule.name());
            model.with("duration", eachRule.duration());
            model.with("rule", replaceRulePlaceholders(eachRule, normalisedPrefix));
            model.with("annotations", entriesMapToString( getAnnotations(eachRule) ));
            model.with("labels", entriesMapToString( getLabels(eachRule) ));

            ruleStrs.add(template.render(model));
        }

        return Joiner.on("\n").join(ruleStrs);
    }

    private static CharSequence quoteString(final String s) {
        return new StringBuilder().append("\"").append(s.replace("\"", "\\\"")).append("\"");
    }

    private static Function<Map.Entry<String,String>,String> formatEntry() {
        return entry -> entry.getKey() + " = " + quoteString(entry.getValue());
    }

    private static String entriesMapToString(final Map<String,String> entries) {
        return COMMAS.join( entries.entrySet().stream().map( formatEntry() ).collect( Collectors.toList() ) );
    }
}
