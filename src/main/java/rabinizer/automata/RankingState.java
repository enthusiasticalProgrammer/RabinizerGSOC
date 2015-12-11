package rabinizer.automata;

import java.util.HashMap;

public class RankingState extends HashMap<FormulaAutomatonState, Integer> {

    private static final long serialVersionUID = 1L;

    public RankingState() {
        super();
    }
    
    @Override
    public String toString() {
        String result = "";
        for (FormulaAutomatonState f : keySet()) {
            result += " " + f + "=" + get(f);
        }
        return result;
    }

}
