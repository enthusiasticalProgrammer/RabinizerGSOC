package rabinizer.ltl;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

/**
 * @author jkretinsky
 */
public abstract class FormulaNullary extends Formula {

    @Override
    public Formula unfold(boolean unfoldG) {
        return this;
    }

    @Override
    public boolean hasSubformula(Formula f) {
        return this.equals(f);
    }

    @Override
    public FormulaNullary evaluate(Set<GOperator> Gs) {
        return this;
    }

    @Override
    public Set<GOperator> gSubformulas() {
        return Sets.newHashSet();
    }

    @Override
    public Set<GOperator> topmostGs() {
        return Collections.emptySet();
    }

}
