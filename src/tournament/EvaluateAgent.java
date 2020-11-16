package tournament;

import java.io.*;

import tournament.run.PacManResults;
import tournament.run.PacManRunResult;
import tournament.utils.Sanitize;
import game.*;
import game.core.Game;
import controllers.pacman.IPacManController;

public class EvaluateAgent {
	private int seed = 0;
	private SimulatorConfig config;
	private int runCount;
	private File resultDir;
	
	public EvaluateAgent(int seed, SimulatorConfig config, int runCount, File resultDir) {
		this.seed = seed;
		this.config = config;
		this.runCount = runCount;
		this.resultDir = resultDir;
	}
	
	public PacManResults evaluateAgent(String agentId, IPacManController agent, boolean verbose) {
		agentId = Sanitize.idify(agentId);
		
        System.out.println("Evaluating agent...");
        
		PacManResults results = new PacManResults();
		
		File replayDir = null;

		if (resultDir != null) {
            resultDir.mkdirs();
            if (config.replay) {
                replayDir = new File(resultDir, "replays");
                replayDir.mkdirs();
            }
		}
						
		for (int i = 0; i < runCount; ++i) {
            if (replayDir != null)
                config.replayFile = new File(replayDir, agentId + "-Run-" + i + ".replay");
        
            config.game.seed = seed + i;
            config.pacManController = agent;
        
            Game info = PacManSimulator.play(config);
            PacManRunResult result = new PacManRunResult(config.game.seed, info);
            
            if (verbose)
                System.out.printf(
                    "seed %2d: reached level %d, score = %5d\n",
                    config.game.seed, info.getCurLevel(), info.getScore());
                
			results.addRunResults(result);
		}
		
		System.out.println(results);
		
		if (resultDir != null)
			outputResults(agentId, results);

		return results;
	}

	private void outputResults(String agentId, PacManResults results) {		
		resultDir.mkdirs();
		
		outputRuns(agentId, results);
		outputAverages(agentId, results);
	}
	
	private void outputRuns(String agentId, PacManResults results) {
		File file = new File(resultDir, agentId + ".runs.csv");
		System.out.println("Writing runs into " + file.getPath());
		
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
			writer.println("agentId;" + results.getRunResults().get(0).getCSVHeader());
			for (PacManRunResult run : results.getRunResults()) {
				writer.println(agentId + ";" + run.getCSV());				
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to write results into " + file.getPath());
		}
	}
	
	private void outputAverages(String agentId, PacManResults results) {
		File file = new File(resultDir, "results.csv");		
		System.out.println("Writing averages into " + file.getPath());
		
        boolean outputHeaders = !file.exists();
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(file, true))) {
			if (outputHeaders) {
				writer.println("agentId;configSeed;" + results.getCSVHeader());
			}
			writer.print(agentId + ";");
			writer.print(seed + ";");
			writer.println(results.getCSV());
		} catch (Exception e) {
			throw new RuntimeException("Failed to write results into: " + file.getPath());
		}
	}

}
