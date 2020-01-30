package core;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The Class DFA.
 */
public class DFA extends FSM {

    /** The minimum DFA state transition matrix. */
    StateTransitionMatrix minSTM;

    /** The NFA state transition matrix. */
    StateTransitionMatrix nfaSTM;

    /** The final states. */
    private ArrayList<Integer> nfaFinalStates;

    /**
     * Instantiates a new DFA.
     */
    public DFA() {

        this.stmat = new StateTransitionMatrix();
        this.finalStates = new ArrayList<Integer>();
    }

    /**
     * Builds the DFA of an incoming NFA.
     *
     * @param the input NFA to convert to DFA
     */
    public void build(NFA nfa) {

        this.nfaSTM = nfa.stmat;
        this.nfaFinalStates = nfa.getFinalStates();

        /** Initialize the DFA table. */
        this.initSTM();
        this.addNewColumn();

        /** Get the first state e-closure. */
        ArrayList<Integer> visitedStates = new ArrayList<Integer>();
        ArrayList<Integer> eClosure = this.nfaSTM.
                getEpsilonClosure(this.nfaSTM.get(1).get(0).get(0),
                        visitedStates);

        /** Add this as first state in DFA. */
        this.stmat.get(1).get(0).addAll(eClosure);

        /** See where the last state added goes, fill its column,
         * add the new states to state transition matrix as well.
         */
        int lastCheckedColumn = 0;

        /** Continue until there is no new state. */
        while (lastCheckedColumn < this.stmat.size() - 1) {

            /** Increase the last checked number. */
            lastCheckedColumn++;

            /** Fill the current column. */
            this.fillColumnFromNFA(lastCheckedColumn);

            /** Check whether a new state is found and add it. */
            this.addNewStates(lastCheckedColumn);
        }

        /** Set correct final and start states. */
        this.simplifySTM(this.stmat);
        LOGGER.info("Simplified STM: " + this.stmat.toString() + "\n");
    }

    /**
     * Simplify the state transition matrix.
     *
     * @param stm the simplified state transition matrix.
     */
    private void simplifySTM(StateTransitionMatrix stm) {

        Integer newName = 1;

        /** Set start state. */
        this.setStartState(newName);

        for (ArrayList<ArrayList<Integer>> col : stm) {

            if (col.get(0).get(0) == 0)
                continue;

            ArrayList<Integer> oldName = new ArrayList<Integer>();
            oldName.addAll(col.get(0));

            /** Set final states. */
            for (Integer finalState : this.nfaFinalStates) {

                if (oldName.contains(finalState)) {

                    this.getFinalStates().add(newName);
                }
            }

            /** Change state number in the entire table. */
            for (ArrayList<ArrayList<Integer>> c : stm) {

                for (ArrayList<Integer> cell : c) {

                    if (cell.equals(oldName)) {

                        cell.clear();
                        cell.add(newName);
                    }
                }
            }

            newName++;
        }
    }

    /**
     * Adds the new states to state transition matrix.
     *
     * @param lastCheckedColumn the last checked column
     */
    private void addNewStates(int lastCheckedColumn) {

        ArrayList<ArrayList<Integer>> column =
                this.stmat.get(lastCheckedColumn);

        ArrayList<ArrayList<Integer>> previousStates =
                new ArrayList<ArrayList<Integer>>();

        for (ArrayList<ArrayList<Integer>> col : this.stmat) {

            if (this.stmat.indexOf(col) != 0)
                previousStates.add(col.get(0));
        }

        previousStates.forEach(element -> Collections.sort(element));
        column.forEach(element -> Collections.sort(element));

        for (ArrayList<Integer> cell : column) {

            if ((!previousStates.contains(cell)) && (!cell.isEmpty())) {

                this.addNewColumn();
                this.stmat.get(this.stmat.size() - 1).get(0).addAll(cell);
            }
        }
    }

    /**
     * Fill the column based on NFA state transmission mat.
     *
     * @param lastCheckedState the last checked state
     */
    private void fillColumnFromNFA(int lastCheckedColumn) {

        ArrayList<ArrayList<Integer>> column =
                this.stmat.get(lastCheckedColumn);

        ArrayList<Integer> state = column.get(0);
        ArrayList<Integer> nextState;
        ArrayList<Integer> nfaCell = new ArrayList<Integer>();
        ArrayList<Integer> nfaCellClosure = new ArrayList<Integer>();

        for (ArrayList<Integer> cell : this.stmat.get(0)) {

            if (cell.get(0) == 0)
                continue;

            nextState = new ArrayList<Integer>();

            for (Integer s : state) {

                nfaCell = StateTransitionMatrix.retrieveCell(this.nfaSTM,
                        s, cell.get(0));

                for (Integer dstState : nfaCell) {

                    if (!nextState.contains(dstState)) {

                        /** Add the state itself. */
                        nextState.add(dstState);

                        /** Calculate epsilon closures. */
                        ArrayList<Integer> visitedStates =
                                new ArrayList<Integer>();
                        nfaCellClosure = this.nfaSTM.
                                getEpsilonClosure(dstState, visitedStates);

                        /** Add closures to next state. */
                        for (Integer closureState : nfaCellClosure) {

                            if (!nextState.contains(closureState))
                                nextState.add(closureState);
                        }
                    }
                }
            }
            column.get(this.stmat.get(0).indexOf(cell)).addAll(nextState);
        }
    }

    /**
     * Initializes the STM using nfaSTM and adds the first column.
     *
     */
    private void initSTM() {

        ArrayList<ArrayList<Integer>> newColumn =
                new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> newCell;

        for (ArrayList<Integer> cell : this.nfaSTM.get(0)) {

            if (cell.get(0) == NFA.epsilon)
                continue;

            newCell = new ArrayList<Integer>();
            newCell.add(cell.get(0));
            newColumn.add(cell);
        }
        this.stmat.add(newColumn);
    }

    /**
     * Adds the new column based on NFA alphabet length.
     *
     */
    private void addNewColumn() {

        ArrayList<ArrayList<Integer>> newColumn =
                new ArrayList<ArrayList<Integer>>();

        ArrayList<Integer> newCell;

        for (ArrayList<Integer> cell : this.nfaSTM.get(0)) {

            if (cell.get(0) == NFA.epsilon)
                continue;

            newCell = new ArrayList<Integer>();
            newColumn.add(newCell);
        }

        this.stmat.add(newColumn);
    }

    /**
     * Makes the minimum DFA out of an NFA.
     *
     * @param nfa the input NFA which is to be minimized
     */
    public void makeMin() {

        /** Create first partition. */
        Partition partition  = new Partition();

        partition.getSets().add(this.getFinalStates());
        partition.getSets().add(this.getNonFinalStates());

        /** Try to build P_k while P_k and P_k-1 are different. */
        partition = this.maximumPartitioning(partition);
        LOGGER.info("Final Partitions: " +
                partition.getSets().toString() + "\n");

        /** Convert final partition to DFA. */
        this.partitionToDFA(partition);
    }

    /**
     * Gets the non final states.
     *
     * @return the non final states
     */
    private ArrayList<Integer> getNonFinalStates() {

        ArrayList<Integer> nonFinalStates = new ArrayList<Integer>();

        for (ArrayList<ArrayList<Integer>> column : this.stmat) {

            int state = column.get(0).get(0);
            if (state == 0)
                continue;

            if (!(this.getFinalStates().contains(state)))
                nonFinalStates.add(state);
        }

        return nonFinalStates;
    }

    /**
     * Creates the DFA corresponding to final partitioning.
     *
     * @param partition the final partition
     */
    private void partitionToDFA(Partition partition) {

        /** First column is the same as NFA except for epsilon. */
        //        ArrayList<ArrayList<Integer>> newColumn =
        //                new ArrayList<ArrayList<Integer>>();
        //        ArrayList<Integer> newCell;
        //
        //        for (ArrayList<Integer> cell : this.nfaSTM.get(0)) {
        //
        //            if (cell.get(0) == NFA.epsilon)
        //                continue;
        //
        //            newCell = new ArrayList<Integer>();
        //            newCell.add(cell.get(0));
        //            newColumn.add(cell);
        //        }
        //        this.stmat.add(newColumn);
        //
        //        /** Initialize the DFA state transition matrix. */
        //        for (ArrayList<Integer> set : partition.getSets()) {
        //
        //            newColumn = new ArrayList<ArrayList<Integer>>();
        //
        //            for (ArrayList<Integer> c : this.nfaSTM.get(0)) {
        //
        //                if (c.get(0) == NFA.epsilon)
        //                    continue;
        //
        //                newCell = new ArrayList<Integer>();
        //                newColumn.add(newCell);
        //            }
        //
        //            this.stmat.add(newColumn);
        //        }

        /** Fill the state transition matrix based on partitions. */
        //        int index = 0;
        //        for (ArrayList<ArrayList<Integer>> col : stmat) {
        //
        //            if (col.get(0).get(0) == 0)
        //                continue;
        //
        //            String [] str = this.getStateTransitionMat().
        //                    get(index).split("\\s+");
        //
        //            int cellIndex = 0;
        //            for (String s : str) {
        //
        //                cellIndex++;
        //                if (s.charAt(0) == Chars.none) {
        //
        //                    col.get(cellIndex).clear();
        //                }
        //                else {
        //
        //                    String [] st = s.split(",");
        //                    col.get(cellIndex).addAll(this.
        //                            convertStatesToIntegers(st));
        //                }
        //            }
        //
        //            index++;
        //        }
    }

    /**
     * Performs the maximum partitioning.
     *
     * @param partition the array of all partitions
     * @return the partition
     */
    private Partition maximumPartitioning(Partition partition) {

        Partition nextPartition  = new Partition();

        do {

            nextPartition =
                    partition.makeNextPartitioning(this.stmat);

            if ((partition.equals(nextPartition)))
                return partition;
            else
                partition = nextPartition;

        } while (true);
    }
}
