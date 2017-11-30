package uk.co.crunch.impl.v2x;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.representer.Representer;
import uk.co.crunch.api.AlertRule;
import uk.co.crunch.utils.PrometheusUtils;

import java.util.*;

import static uk.co.crunch.impl.AlertRulesGenerator.*;

public class AlertRulesGenerator2x {

    public static String buildRulesFile(final String metricPrefix, final String alertGroupName, final AlertRule... rules) {
        final String normalisedPrefix = PrometheusUtils.normaliseName(metricPrefix) + "_";

        final AlertRulesGroup group = new AlertRulesGroup(alertGroupName);

        for (AlertRule eachRule : rules) {
            final String alertName = titlecase(metricPrefix) + titlecase( eachRule.name() );

            group.addRule( new AlertRulePojo(alertName, replaceRulePlaceholders(eachRule, normalisedPrefix), eachRule.duration(), getLabels(eachRule), getAnnotations(eachRule)) );
        }

        return getYaml().dumpAsMap( new AlertRulesPojo(group) );
    }

    private static Yaml getYaml() {
        final Representer repr = new Representer();
        repr.setPropertyUtils( new UnsortedPropertyUtils() );

        final DumperOptions dumper = new DumperOptions();
        dumper.setSplitLines(false);

        return new Yaml(new Constructor(), repr, dumper);
    }

    private static String titlecase( final String s) {
        return new StringBuilder(s.length()).append( Character.toTitleCase( s.charAt(0) ) ).append( s.substring(1) ).toString();
    }

    // https://bitbucket.org/asomov/snakeyaml/src/tip/src/test/java/org/yaml/snakeyaml/issues/issue60/CustomOrderTest.java?fileviewer=file-view-default
    private static class UnsortedPropertyUtils extends PropertyUtils {
        @Override
        protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess) {
            return new LinkedHashSet<>(getPropertiesMap(type, BeanAccess.FIELD).values());
        }
    }

    private static class AlertRulesPojo {
        private final List<AlertRulesGroup> groups = new ArrayList<>();

        public AlertRulesPojo(AlertRulesGroup group) {
            groups.add(group);
        }
    }

    private static class AlertRulesGroup {
        private final String name;
        private final List<AlertRulePojo> rules = new ArrayList<>();

        public AlertRulesGroup(String alertGroupName) {
            this.name = alertGroupName;
        }

        public void addRule(AlertRulePojo rule) {
            this.rules.add(rule);
        }
    }

    private static class AlertRulePojo {
        String alert = "OpenstackCinderVolumeStuck";
        String expr;
        String duration;
        Map<String,String> labels;
        Map<String,String> annotations;

        public AlertRulePojo(String alert, String expr, String duration, Map<String,String> labels, Map<String,String> annotations) {
            this.alert = alert;
            this.expr = expr;
            this.duration = duration;
            this.labels = labels;
            this.annotations = annotations;
        }
    }
}
