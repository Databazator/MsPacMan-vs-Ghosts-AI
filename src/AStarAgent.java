import controllers.pacman.PacManControllerBase;
import game.core.Game;
import game.core.GameView;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;

public final class AStarAgent extends PacManControllerBase
{
	public ArrayList<Integer> Actions = new ArrayList<>();
	public Double CurrentBestCost = Double.MAX_VALUE;
	public Integer CurrentTargetNode = -1;
	public Boolean GoalFound = false;

	public class FringeElement
	{
		public FringeElement Parent;
		public Double Cost;
		public Game State;
		public Integer Depth;
		public Integer Action;

		public FringeElement(FringeElement parent, Double cost, Game state, Integer depth, Integer action)
		{
			Parent = parent;
			Cost = cost;
			State = state;
			Depth = depth;
			Action = action;
		}
	}
	public class SearchResult
	{
		public ArrayList<Integer> ActionsList;
		public boolean GoalFound;
		public Double BestNodeCost;
		public Integer BestNodeTileIndex;
		public Integer SearchIters;
		public SearchResult(ArrayList<Integer> actions, boolean goalFound, Double bestCost, Integer bestIndex, Integer searchIters)
		{
			ActionsList = actions;
			GoalFound = goalFound;
			BestNodeCost = bestCost;
			BestNodeTileIndex = bestIndex;
			SearchIters = searchIters;
		}
	}

	public int StateHash(Game state)
	{
		return (state.getCurPacManLoc() << 16) | state.getNumActivePills(); // | state.getLevelTime();
	}
	public Integer StateCost(Game state, Game prevState)
	{
//		int posPillIndex = state.getPillIndex(state.getCurPacManLoc());
//		if(posPillIndex != -1 && prevState.checkPill(posPillIndex))
//			return 1;
//		return 5;
		return 1;
	}
	public Double HeuristicEstimate(Game state, int currLevel)
	{
		// goal is found - it's cost is zero
		if(IsGoal(state, currLevel))
			return 0.0;

		return ( 4 * (state.getDistanceToNearestPill() + 12.0)  + state.getNumActivePills() * 4) * 2.0;
	}

	public Boolean IsGoal(Game state, int currLevel)
	{
		return state.getNumActivePills() == 0;
		//return currLevel != state.getCurLevel() || (currLevel == 16 && state.gameOver() && state.getLivesRemaining() > 0); //state.getNumActivePills() == 1 && state.getDistanceToNearestPill() == 4;
	}

	public Boolean Contains(int[] array, int element)
	{
		for(int i : array)
		{
			if(i == element)
				return true;
		}
		return false;
	}

	public int[] GetPossiblePacActions(Game state)
	{
		// get possible actions and adjust them to allow wall bump reversals
		int[] possibleActions = state.getPossiblePacManDirs(false);
		int curPacDir = state.getCurPacManDir();
		if(!Contains(possibleActions,curPacDir))
		{
			possibleActions = Arrays.copyOf(possibleActions, possibleActions.length + 1);
			possibleActions[possibleActions.length - 1] = curPacDir;
		}
		return possibleActions;
	}

	public SearchResult PacAStar(Game game, long timeDue)
	{
		boolean goalFound = false;
		int currentLives = game.getLivesRemaining();
		int currentLevel = game.getCurLevel();
		// --------------------------------------

		int astarIters = 0;

		PriorityQueue<FringeElement> fringe = new PriorityQueue<FringeElement>(
				(FringeElement first, FringeElement second) -> Double.compare(first.Cost, second.Cost)
		);
		fringe.add(new FringeElement(null, HeuristicEstimate(game, currentLevel), game, 0, null));
		HashMap<Integer, Double> visited = new HashMap<>();

		FringeElement bestFringeElement = fringe.peek();

		long iterStart = System.nanoTime();

		while (!fringe.isEmpty() && System.currentTimeMillis() < timeDue - 2)
		{
			FringeElement current = fringe.poll();
			Game currentState = current.State;
			double currentCost = current.Cost;
			int currentDepth = current.Depth;
			//System.out.println("FringePop: d:" + currentDepth + "| c:" + currentCost + " | pills:" + currentState.getNumActivePills() + "| dist:" + currentState.getDistanceToNearestPill());

			if(bestFringeElement != null && bestFringeElement.Depth != 0)
				bestFringeElement = currentCost < bestFringeElement.Cost ? current : bestFringeElement;
			else
				bestFringeElement = current;

			if(IsGoal(currentState, currentLevel))
			{
				goalFound = true;
				bestFringeElement = current;
				break;
			}

			int[] possibleActions = GetPossiblePacActions(currentState);
			for(int action: possibleActions)
			{
				Game newState = currentState.copy();
				newState.advanceGame(action);

				// check if pacman died during state advance
				if(newState.getLivesRemaining() < currentLives || (newState.gameOver() && currentLevel != 16))
				{
					// Pac died during state advance - purge the entire branch path to avoid these nodes
					FringeElement parent = current;
					while (parent != null && GetPossiblePacActions(parent.State).length == 1) {
						parent.Cost = 9999.0 - (parent.Depth * 10);
						visited.put(StateHash(parent.State), parent.Cost);

						if(parent == bestFringeElement && !fringe.isEmpty()) {
							if(fringe.peek() == parent)
								fringe.poll();
							bestFringeElement = !fringe.isEmpty() ? fringe.peek() : parent;
						}

						parent = parent.Parent;
					}
					continue;
				}
				// calculate new advanced state's cost and update the fringe if its good
				Integer newStateHash = StateHash(newState);
				Double newStateCost = currentDepth + StateCost(newState, currentState) + HeuristicEstimate(newState, currentLevel);
				if(!visited.containsKey(newStateHash) || newStateCost < visited.get(newStateHash))
				{
					visited.put(newStateHash, newStateCost);
					FringeElement newStateFE = new FringeElement(current, newStateCost, newState, currentDepth + StateCost(newState, currentState), action);
					fringe.add(newStateFE);

					bestFringeElement = newStateCost < bestFringeElement.Cost ? newStateFE : bestFringeElement;
				}
			}

			// ---------- timing debug -----------------
			long currTime = System.nanoTime();
			float iterDur = (float)(currTime - iterStart)/1000000f;
			long due = timeDue - System.currentTimeMillis();
			//out.println("AStar iter dur: " + iterDur + "ms | due: " + due + "ms");
			iterStart = currTime;

			astarIters++;
		}

		//gather and return an action path to the best found node
		ArrayList<Integer> actionList = new ArrayList<>();
		FringeElement currElement = bestFringeElement;

		while (currElement.Parent != null) {
			if (currElement.Action != null) {
					actionList.add(currElement.Action);
			}
			currElement = currElement.Parent;
			}

		SearchResult result = new SearchResult(
				actionList, goalFound,
				bestFringeElement.Cost,
				bestFringeElement.State.getCurPacManLoc(),
				astarIters
		);

		//System.out.println("AStarIters: " + astarIters);
		return result;
	}
	@Override
	public void tick(Game game, long timeDue) {

		int actionToUse = - 1;
		SearchResult result = null;
		if(GoalFound)
		{
			if(Actions.isEmpty())
			{
				GoalFound = false;
				return;
			}
			else {
				actionToUse = Actions.removeLast();
				pacman.set(actionToUse);

				GameView.addPoints(game, Color.GREEN, CurrentTargetNode);
				GameView.addText(0, 5, Color.GREEN, "Ac:" + actionToUse + "| Best:" + CurrentBestCost + "| Goal:" + GoalFound);
				return;
			}
		}

//		int[] possibleDirs = GetPossiblePacActions(game);
//		String dir = "";
//        for (int possibleDir : possibleDirs) {
//            switch (possibleDir) {
//                case 0:
//                    dir += "UP | ";
//					break;
//				case 1:
//					dir += "RIGHT | ";
//					break;
//				case 2:
//					dir += "DOWN | ";
//					break;
//				case 3:
//					dir += "LEFT | ";
//					break;
//            }
//        }
//		out.println("PossibleActions: | " + dir);

		result = PacAStar(game, timeDue);
		//System.out.println("Result: Cost: " + result.BestNodeCost + " | target: " + result.BestNodeTileIndex + "| " + result.ActionsList.toString());

		if(result.GoalFound) {
			GoalFound = true;
			Actions = result.ActionsList;
			CurrentTargetNode = result.BestNodeTileIndex;
		}
		else if(CurrentBestCost == null || result.BestNodeCost < CurrentBestCost || Actions.isEmpty()){
			CurrentBestCost = result.BestNodeCost;
			Actions = result.ActionsList;
			CurrentTargetNode = result.BestNodeTileIndex;
		}


		if(!Actions.isEmpty()) {
			actionToUse = Actions.removeLast();
			pacman.set(actionToUse);
		}

		// ----------debug draw------------------
		if(CurrentTargetNode != -1) {
			GameView.addPoints(game, Color.BLUE, CurrentTargetNode);
		}

		if(actionToUse == -1)
			GameView.addText(0, 5, Color.RED, "Ac: NO ACTION" + "| Best:" + CurrentBestCost + "| Goal:" + GoalFound);
		else
			GameView.addText(0, 5, Color.YELLOW, "Ac:" + actionToUse + "| Best:" + CurrentBestCost + "| Goal:" + GoalFound);
		// ---------------------------------------
	}
}
