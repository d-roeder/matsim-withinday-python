package modechoice.rl.java.core;

import java.io.File;
import java.net.URISyntaxException;

import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.withinday.core.WithinDayCli;
import org.matsim.withinday.core.WithinDayModule;
import org.matsim.withinday.utils.WithinDayConfigGroup;

import modechoice.random.java.core.RunBerlinRandomModeChoice;
import modechoice.rl.java.utils.CustomConfigGroup;
import picocli.CommandLine;

/**
 * <h2>RunBerlinRLModeChoice</h2>
 * <p>
 * The reinforcement learning flavour of the within-day mode choice on the MATSim Open Berlin
 * scenario. Everything about the scenario itself - Berlin's calibrated scoring, its subpopulation
 * strategies, the within-day layer - comes from {@link RunBerlinRandomModeChoice}; this class only
 * swaps the replanner and observer and brings in the {@code agentModeChoice} hyperparameters.
 * </p>
 * <p>
 * Those hyperparameters live in a second config file, named by
 * {@code withinday.externalModelFileName} and resolved next to the scenario config, exactly as in
 * {@link RunRLModeChoice}. It is read in {@code prepareConfig}, which MATSimApplication calls
 * before it applies the {@code --config:} overrides, so the values the launcher passes on the
 * command line still win.
 * </p>
 */
@CommandLine.Command(header = ":: Open Berlin Scenario (within-day RL mode choice) ::", version = OpenBerlinScenario.VERSION,
        mixinStandardHelpOptions = true, showDefaultValues = true)
public class RunBerlinRLModeChoice extends RunBerlinRandomModeChoice {

    public static void main(String[] args) {
        // Accepts both the positional config path used by the python launcher and "--config <path>".
        MATSimApplication.execute(RunBerlinRLModeChoice.class, WithinDayCli.normalize(args));
    }

    @Override
    protected Config prepareConfig(Config config) {
        // Berlin plus the within-day framework settings.
        super.prepareConfig(config);

        CustomConfigGroup customConfigGroup = ConfigUtils.addOrGetModule(config, CustomConfigGroup.class);
        loadExternalModelConfig(config, customConfigGroup);

        return config;
    }

    @Override
    protected AbstractModule createWithinDayModule(Config config) {
        // Berlin brings its own scoring function factory, so SplitAwareScoringFunctionFactory stays off;
        // CustomRLObserver builds its CustomRLScoringEngine on top of Berlin's scoring parameters.
        return new WithinDayModule(config, CustomRLReplanner.class, CustomRLObserver.class, false);
    }

    /**
     * Reads the hyperparameter config named by {@code withinday.externalModelFileName} into the
     * already registered {@link CustomConfigGroup}. A missing file is not an error: the group then
     * keeps its defaults, and the launcher can still set every value via {@code --config:}.
     *
     * @param config The configuration being prepared.
     * @param target The config group the values are read into.
     */
    private static void loadExternalModelConfig(Config config, CustomConfigGroup target) {
        String modelFileName = WithinDayConfigGroup.getExternalModelFileName();
        if (modelFileName == null || modelFileName.isBlank() || config.getContext() == null) {
            return;
        }

        File scenarioDirectory;
        try {
            scenarioDirectory = new File(config.getContext().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve the scenario directory from " + config.getContext(), e);
        }

        File modelFile = new File(scenarioDirectory, modelFileName);
        if (!modelFile.exists()) {
            return;
        }

        // Parses into the group instance that is already registered on config, so the values land there.
        ConfigUtils.loadConfig(modelFile.getAbsolutePath(), target);
    }
}
