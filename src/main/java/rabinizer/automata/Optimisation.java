package rabinizer.automata;

public enum Optimisation {
    /* Common */
    EAGER,

    /* DetLimit */
    COVER, SKELETON, DELAYED_JUMP, OR_BREAKUP,

    /* Rabinizer */
    ALL_SLAVES
}
