import controllers.pacman.PacManControllerBase;
import game.core.Game;

public class MeasureStats extends PacManControllerBase {
    AStarAgent agent = new AStarAgent();

    final static Integer testWarmupCount = 1000;
    final static Integer testSampleCount = 100000;
    final static Integer samples_per_iter = 100;

    boolean clone_warmup_finished = false;
    boolean clone_test_finished = false;
    Integer clone_samples = 0;
    long clone_time_accumulator = 0;

    boolean advance_warmup_finished = false;
    boolean advance_test_finished = false;
    Integer advance_samples = 0;
    long advance_time_accumulator = 0;


    boolean astariter_warmup_finished = false;

    boolean astariter_test_finished = false;

    Integer astariter_samples = 0;

    long astariter_time = 0;

    boolean finalStatsPrinted = false;

    @Override
    public void tick(Game game, long timeDue) {
        if(!clone_test_finished)
        {
            test_clone(game, timeDue);
            if(clone_warmup_finished)
                System.out.println("Test Clone | progress: " + clone_samples/10000 + "/100");
        }
        else if(!advance_test_finished)
        {
            test_advance(game, timeDue);
            if(advance_warmup_finished)
                System.out.println("Test Advance | progress: " + advance_samples/10000 + "/100");
        }
        else if(!astariter_test_finished)
        {
            test_astar_iter(game, timeDue);
            if(astariter_warmup_finished)
                System.out.println("Test Astar iter | progress: " + astariter_samples);
        }
        else if(!finalStatsPrinted)
        {
            finalStatsPrinted = true;
            System.out.println("=======================================================");
            float cloneResult = (float)clone_time_accumulator / clone_samples;
            System.out.println("Test Clone : avg clone time: " + cloneResult*1000000 + "ns");
            float advanceResult = (float)advance_time_accumulator / advance_samples;
            System.out.println("Test Advance : avg advance time: " + advanceResult*1000000 + "ns");
            float astarResult = (float)astariter_time / astariter_samples;
            System.out.println("Test AstarIter : avg iter time: " + astarResult*1000000 + "ns");
            System.out.println("=======================================================");
        }
        
        pacman.set(1);
    }

    public void test_clone(Game game, long timeDue)
    {
        while(!clone_test_finished && System.currentTimeMillis() < timeDue - 2 )
        {
            long startTime = System.currentTimeMillis();
            for(int i = 0; i < samples_per_iter; i++)
            {
                game.copy();
            }
            clone_samples  += samples_per_iter;
            clone_time_accumulator += System.currentTimeMillis() - startTime;
            //warmup
            if(!clone_warmup_finished && clone_samples == 1000 /* testWarmupCount - smh my head putting a static val here crashes */)
            {
                clone_warmup_finished = true;
                clone_samples = 0;
                clone_time_accumulator = 0;
            }
            else if(clone_samples == 1000000/*testSampleCount*/)
            {
                clone_test_finished = true;
            }
        }
    }

    public void test_advance(Game game, long timeDue)
    {
        while(!advance_test_finished && System.currentTimeMillis() < timeDue - 2 )
        {
            long startTime = System.currentTimeMillis();
            for(int i = 0; i < samples_per_iter; i++)
            {
                game.advanceGame(-1);
            }
            advance_samples  += samples_per_iter;
            advance_time_accumulator += System.currentTimeMillis() - startTime;
            //warmup
            if(!advance_warmup_finished && advance_samples == 1000)
            {
                advance_warmup_finished = true;
                advance_samples = 0;
                advance_time_accumulator = 0;
            }
            else if(advance_samples == 1000000)
            {
                advance_test_finished = true;
            }
        }
    }

    public void test_astar_iter(Game game, long timeDue)
    {
        long startTime = System.currentTimeMillis();
        AStarAgent.SearchResult result = agent.PacAStar(game, timeDue);
        astariter_samples  += result.SearchIters;
        astariter_time += System.currentTimeMillis() - startTime;
        //warmup
        if(!astariter_warmup_finished && astariter_samples >= 1000)
        {
            astariter_warmup_finished = true;
            astariter_samples = 0;
            astariter_time = 0;
        }
        else if(astariter_samples >= 500000)
        {
            astariter_test_finished = true;
        }
    }
}
