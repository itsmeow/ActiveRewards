package dev.itsmeow.activerewards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;

import com.google.common.collect.Sets;

public class RewardAction {
    
    private RewardActionCondition[] conditions;
    private String[] commands;
    
    public RewardAction(RewardActionCondition[] conditions, String[] commands) {
        this.conditions = conditions;
        this.commands = commands;
    }

    public RewardActionCondition[] getConditions() {
        return conditions;
    }

    public void execute(ActiveRewards plugin, String username) {
        for(String command : commands) {
            plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("{username}", username));
        }
    }
    
    public void checkExecute(ActiveRewards plugin, String username, Map<String, ServerTuple> serversLast, Map<String, ServerTuple> serversCurrent) {
        boolean allConditions = true;
        for(RewardActionCondition condition : conditions) {
            allConditions = allConditions && condition.eval(serversLast, serversCurrent);
        }
        if(allConditions) {
            for(String command : commands) {
                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{username\\}", username));
            }
        }
    }

    public static class RewardActionCondition {

        private int triggerValue;
        private When whenTrigger;
        private What whatTrigger;
        private ValueFrom valueFrom;

        private Set<String> servers;

        public RewardActionCondition(ValueFrom valueFrom, What whatTrigger, When whenTrigger, int triggerValue, List<String> servers) {
            this.triggerValue = triggerValue;
            this.whenTrigger = whenTrigger;
            this.whatTrigger = whatTrigger;
            this.valueFrom = valueFrom;

            this.servers = Sets.newHashSet(servers.toArray(new String[0]));
        }

        public boolean eval(Map<String, ServerTuple> serversLast, Map<String, ServerTuple> serversCurrent) {
            int value = 0;
            Map<String, ServerTuple> map = whatTrigger.isLast() ? serversLast : serversCurrent;
            map.forEach((server, tuple) -> ActiveRewards.debug(whatTrigger.isLast() + "-" + server + " - " + tuple.playtime + " - " + tuple.points + " - " + tuple.historicalPoints));
            for(String str : map.keySet()) {
                if(valueFrom == ValueFrom.ALL || servers.contains(str)) {
                    value += map.get(str).fromWhat(whatTrigger);
                }
            }
            boolean result = whenTrigger.op(value, triggerValue);
            ActiveRewards.debug("EVAL " + whatTrigger + " " + value + " " + whenTrigger.symbol + " " + triggerValue + " TO " + result);
            return result;
        }

        public int getTriggerValue() {
            return triggerValue;
        }

        public When getWhenTrigger() {
            return whenTrigger;
        }

        public What getWhatTrigger() {
            return whatTrigger;
        }

        public ValueFrom getValueFrom() {
            return valueFrom;
        }

        public Set<String> getServers() {
            return servers;
        }

        @Override
        public boolean equals(Object o) {

            if(o == this)
                return true;
            if(!(o instanceof RewardActionCondition)) {
                return false;
            }
            RewardActionCondition a = (RewardActionCondition) o;
            return triggerValue == a.triggerValue &&
            whenTrigger.ordinal() == a.whenTrigger.ordinal() &&
            whatTrigger.ordinal() == a.whatTrigger.ordinal() &&
            valueFrom.ordinal() == a.valueFrom.ordinal() &&
            Objects.equals(servers, a.servers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(triggerValue, whenTrigger.ordinal(), whatTrigger.ordinal(), valueFrom.ordinal(), servers);
        }

        public static enum When {
            GREATER_EQUAL(">=", (l, r) -> l >= r),
            LESS_EQUAL("<=", (l, r) -> l <= r),
            EQUAL("=", (l, r) -> l == r),
            GREATER(">", (l, r) -> l > r),
            LESS("<", (l, r) -> l < r);
            
            public final String symbol;
            private BiFunction<Integer, Integer, Boolean> operation;
            
            private When(String symbol, BiFunction<Integer, Integer, Boolean> operation) {
                this.symbol = symbol;
                this.operation = operation;
            }
            
            public boolean op(int left, int right) {
                return operation.apply(left, right);
            }
            
            public static When match(String symbolContaining) {
                for(When when : When.values()) {
                    if(symbolContaining.contains(when.symbol)) {
                        return when;
                    }
                }
                return null;
            }
        }

        public static enum What {
            LASTPLAYTIME(true),
            LASTPOINTS(true),
            LASTHISTORICPOINTS(true),
            NEWPLAYTIME(false),
            NEWPOINTS(false),
            NEWHISTORICPOINTS(false);
            
            private boolean isLast;
            
            private What(boolean isLast) {
                this.isLast = isLast;
            }

            public boolean isLast() {
                return isLast;
            }
        }

        public static enum ValueFrom {
            ALL,
            SERVERS;
        }
        
        public static RewardActionCondition fromString(String in) {
            List<String> servers = new ArrayList<String>();
            String whatFrom = in.substring(in.indexOf('[') + 1, in.indexOf(']'));
            ValueFrom valueFrom = ValueFrom.ALL;
            if(whatFrom.contains(":")) {
                valueFrom = ValueFrom.SERVERS;
                String[] whatFromSp = whatFrom.split(":");
                whatFrom = whatFromSp[0];
                String serversList = whatFromSp[1];
                for(String server : serversList.split(",")) {
                    servers.add(server);
                }
            }
            What whatTrigger = What.valueOf(whatFrom.toUpperCase());
            When whenTrigger = When.match(in);
            int triggerValue = Integer.parseInt(in.substring(in.indexOf(whenTrigger.symbol) + whenTrigger.symbol.length()));
            
            return new RewardActionCondition(valueFrom, whatTrigger, whenTrigger, triggerValue, servers);
        }
    }

}
