package rabinizer.automata;

public enum Optimisation {
    /* Common */
    EAGER,

    /* DetLimit */
    COVER, SKELETON, OR_BREAKUP,

    /* Rabinizer */
    ONLY_RELEVANT_SLAVES, SLAVE_SUSPENSION,

    /* additionnaly for DTGRARaw: */
    COMPUTE_ACC_CONDITION, SINKS, OPTIMISE_INITIAL_STATE, NOT_ISABELLE_ACC, COMPLETE, EMPTINESS_CHECK

}
