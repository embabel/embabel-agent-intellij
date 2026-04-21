package demo;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;

@Agent(description = "demo agent")
public class TestAgent {
    @AchievesGoal(description = "broken")
    public void badMethod() {}

    @AchievesGoal(description = "valid")
    public GoalResult goodMethod() {
        return new GoalResult();
    }

    public static final class GoalResult {}
}
