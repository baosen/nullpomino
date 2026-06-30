package nullpomino.game.mode;

/**
 * RetroMarathonMode line 351 analysis:
 *
 * <pre>
 *   switch (gametype) {
 *     case GAMETYPE_TYPE_A:   ...  // 0
 *     case GAMETYPE_TYPE_B:   ...  // 1
 *     case GAMETYPE_ARRANGE:  ...  // 2
 *     default: strLine = String.valueOf(engine.statistics.lines); break;  // line 351
 *   }
 * </pre>
 *
 * {@code gametype} is bounded by the menu to [0, GAMETYPE_MAX-1] = [0, 2].
 * The switch already has cases for all three values (0, 1, 2), so the
 * {@code default} branch at line 351 is DEAD CODE — it can never execute at
 * runtime.  JaCoCo therefore reports it uncovered forever regardless of any
 * test. No test is added for this class; the analysis is documented here.
 */
class RetroMarathonModeBranchCoverageTest4 {
    // No tests — line 351 is dead code, see class Javadoc.
}
