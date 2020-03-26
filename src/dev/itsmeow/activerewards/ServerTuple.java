package dev.itsmeow.activerewards;

import java.util.Objects;

import dev.itsmeow.activerewards.RewardAction.RewardActionCondition;

public class ServerTuple {

    public int playtime;
    public int points;
    public int historicalPoints;

    public ServerTuple(int playtime, int points, int historicalPoints) {
        this.playtime = playtime;
        this.points = points;
        this.historicalPoints = historicalPoints;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;
        if(!(o instanceof ServerTuple)) {
            return false;
        }
        ServerTuple a = (ServerTuple) o;
        return playtime == a.playtime && points == a.points && historicalPoints == a.historicalPoints;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playtime, points, historicalPoints);
    }
    
    public int fromWhat(RewardActionCondition.What what) {
        switch(what) {
        case LASTPLAYTIME: return playtime;
        case LASTHISTORICPOINTS: return historicalPoints;
        case LASTPOINTS: return points;
        case NEWHISTORICPOINTS: return historicalPoints;
        case NEWPLAYTIME: return playtime;
        case NEWPOINTS: return points;
        }
        throw new NullPointerException();
    }

}
