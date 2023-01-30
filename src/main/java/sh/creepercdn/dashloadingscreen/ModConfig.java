package sh.creepercdn.dashloadingscreen;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.Map;

@Config(name = "dash-loading-screen")
public class ModConfig implements ConfigData {
    int paddingSize = 10;
    boolean debug = false;
    int progressBarHeight = 2;

    @ConfigEntry.Gui.CollapsibleObject
    InnerColorConfig color = new InnerColorConfig();
    @ConfigEntry.Gui.CollapsibleObject
    InnerLineConfig line = new InnerLineConfig();

    public enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    public static class InnerColorConfig {
        @ConfigEntry.Gui.Excluded
        Map<String, String> colorVariables = Map.of(
                "red", "#ff6188",
                "orange", "#fc9867",
                "yellow", "#ffd866",
                "green", "#a9dc76",
                "blue", "#78dce8",
                "purple", "#ab9df2",
                "text", "#fcfcfa",
                "base0", "#19181a",
                "base1", "#221f22",
                "base2", "#2d2a2e");
        @ConfigEntry.Gui.Excluded
        Map<String, Integer> lineColors = Map.of(
                "base2", 1000,
                "blue", 50,
                "red", 1);
        String[] progressColors = new String[]{"red", "orange", "yellow", "green"};
        String backgroundColor = "base1";
        String foregroundColor = "text";
        String progressTrackColor = "base0";
    }

    public static class InnerLineConfig {
        float lineSpeedDifference = 4.0f;
        int lineAmount = 200;
        Direction lineDirection = Direction.LEFT;
        int lineWidth = 100;
        int lineMinHeight = 4;
        int lineMaxHeight = 10;
        float lineSpeed = 2;
    }
}
