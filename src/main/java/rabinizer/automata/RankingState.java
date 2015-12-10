package rabinizer.automata;

import java.util.HashMap;

public class RankingState extends HashMap<FormulaAutomatonState, Integer> {

    public RankingState() {
        super();
    }
    
    public String toString() {
        String result = "";
        for (FormulaAutomatonState f : keySet()) {
            result += " " + f + "=" + get(f);
        }
        return result;
    }

}
