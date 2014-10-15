/*
 * Copyright 2014 Ghent University, Bayer CropScience.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jamesframework.core.subset.neigh.adv;

import org.jamesframework.core.subset.neigh.moves.GeneralSubsetMove;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.jamesframework.core.subset.SubsetSolution;
import org.jamesframework.core.subset.algo.exh.SubsetSolutionIterator;
import org.jamesframework.core.subset.neigh.SingleSwapNeighbourhood;
import org.jamesframework.core.subset.neigh.moves.SubsetMove;
import org.jamesframework.core.subset.neigh.SubsetNeighbourhood;
import org.jamesframework.core.util.SetUtilities;

/**
 * <p>
 * A subset neighbourhood that generates moves performing a fixed number of simultaneous swaps of selected and
 * unselected IDs. Generated moves are of type {@link GeneralSubsetMove} which is a subtype of {@link SubsetMove}.
 * When applying moves generated by this neighbourhood to a given subset solution, the set of selected IDs will
 * always remain of the same size. Therefore, this neighbourhood is only suited for fixed size subset selection
 * problems. If desired, a set of fixed IDs can be provided which are not allowed to be swapped.
 * </p>
 * <p>
 * If it happens to be impossible to perform the desired number of swaps (e.g. because the current selection is
 * too small or too large) a lower, maximum number of swaps will be performed.
 * </p>
 * <p>
 * Note that the number of possible moves quickly becomes very large when the size of the full set and/or selected
 * subset increase. For example, generating all combinations of 2 simultaneous swaps already yields
 * \[
 *  \frac{s(s-1)}{2} \times \frac{(n-s)(n-s-1)}{2}
 * \]
 * possibilities, where \(n\) is the size of the full set and \(s\) is the subset size. When selecting e.g.
 * 30 out of 100 items, this value already exceeds one million. Because of the large number of possible moves,
 * this advanced neighbourhood should be used with care, especially in combination with searches that inspect
 * all moves in every step. Furthermore, searches that inspect random moves may have few chances to find an
 * improvement in case of a huge amount of possible neighbours.
 * </p>
 * <p>
 * This neighbourhood is thread-safe: it can be safely used to concurrently generate moves in different searches
 * running in separate threads.
 * </p>
 * 
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public class DisjointMultiSwapNeighbourhood extends SubsetNeighbourhood {

    // number of simultaneous swaps
    private final int numSwaps;
    
    /**
     * Creates a multi swap neighbourhood without fixed IDs, indicating the number of (simultaneous)
     * swaps performed by any generated move. If <code>numSwaps</code> is 1, this neighbourhood
     * generates exactly the same moves as the {@link SingleSwapNeighbourhood} so in such case
     * it is advised to use the latter neighbourhood which has been optimized for this specific
     * scenario.
     * 
     * @param numSwaps number of swaps performed by any generated move (&gt; 0)
     * @throws IllegalArgumentException if <code>maxSwaps</code> is not strictly positive
     */
    public DisjointMultiSwapNeighbourhood(int numSwaps){
        this(numSwaps, null);
    }
    
    /**
     * Creates a multi swap neighbourhood with a given set of fixed IDs which are not allowed to be swapped.
     * None of the generated moves will add nor remove any of these fixed IDs. All generated moves will swap
     * exactly <code>numSwaps</code> pairs of IDs. If <code>numSwaps</code> is 1, this neighbourhood generates
     * exactly the same moves as the {@link SingleSwapNeighbourhood} so in such case it is advised
     * to use the latter neighbourhood which has been optimized for this specific scenario.
     * 
     * @param numSwaps number of swaps performed by any generated move (&gt; 0)
     * @param fixedIDs set of fixed IDs which are not allowed to be swapped
     * @throws IllegalArgumentException if <code>numSwaps</code> is not strictly positive
     */
    public DisjointMultiSwapNeighbourhood(int numSwaps, Set<Integer> fixedIDs){
        super(fixedIDs);
        // check number of swaps
        if(numSwaps <= 0){
            throw new IllegalArgumentException("The number of swaps should be strictly positive.");
        }
        this.numSwaps = numSwaps;
    }
    
    /**
     * Get the fixed number of swaps performed by generated moves.
     * 
     * @return fixed number of swaps
     */
    public int getNumSwaps() {
        return numSwaps;
    }
    
    /**
     * Generates a move for the given subset solution that removes a random subset of \(k\) IDs from the current
     * selection and replaces them with an equally large random subset of the currently unselected IDs, where
     * \(k\) is the number of swaps specified at construction. If it is not possible to swap \(k\) items a
     * smaller, maximum number of swaps will be performed. Possible fixed IDs are not considered to be swapped.
     * If no swaps can be performed, <code>null</code> is returned.
     * 
     * @param solution solution for which a random multi swap move is generated
     * @return random multi swap move, <code>null</code> if no swaps can be performed
     */
    @Override
    public SubsetMove getRandomMove(SubsetSolution solution) {
        // get set of candidate IDs for deletion and addition (fixed IDs are discarded)
        Set<Integer> removeCandidates = getRemoveCandidates(solution);
        Set<Integer> addCandidates = getAddCandidates(solution);
        // return null if no swaps are possible
        int curNumSwaps = numSwaps(addCandidates, removeCandidates);
        if(curNumSwaps == 0){
            // impossible to perform a swap
            return null;
        }
        // use thread local random for better concurrent performance
        Random rg = ThreadLocalRandom.current();
        // pick random IDs to remove from selection
        Set<Integer> del = SetUtilities.getRandomSubset(removeCandidates, curNumSwaps, rg);
        // pick random IDs to add to selection
        Set<Integer> add = SetUtilities.getRandomSubset(addCandidates, curNumSwaps, rg);
        // create and return move
        return new GeneralSubsetMove(add, del);
    }

    /**
     * <p>
     * Generates the set of all possible moves that perform exactly \(k\) swaps, where \(k\) is the desired number
     * of swaps specified at construction. Possible fixed IDs are not considered to be swapped. If \(m &lt; k\)
     * IDs are currently selected or unselected (excluding any fixed IDs), no swaps can be performed and the
     * returned set will be empty.
     * 
     * @param solution solution for which all possible multi swap moves are generated
     * @return set of all multi swap moves, may be empty
     */
    @Override
    public Set<SubsetMove> getAllMoves(SubsetSolution solution) {
        // create empty set to store generated moves
        Set<SubsetMove> moves = new HashSet<>();
        // get set of candidate IDs for deletion and addition
        Set<Integer> removeCandidates = getRemoveCandidates(solution);
        Set<Integer> addCandidates = getAddCandidates(solution);
        // possible to perform desired number of swaps?
        int curNumSwaps = numSwaps(addCandidates, removeCandidates);
        if(curNumSwaps == 0){
            // impossible: return empty set
            return moves;
        }
        // create all moves performing numSwaps swaps
        SubsetSolutionIterator itDel, itAdd;
        Set<Integer> del, add;
        itDel = new SubsetSolutionIterator(removeCandidates, curNumSwaps);
        while(itDel.hasNext()){
            del = itDel.next().getSelectedIDs();
            itAdd = new SubsetSolutionIterator(addCandidates, curNumSwaps);
            while(itAdd.hasNext()){
                add = itAdd.next().getSelectedIDs();
                // create and add move
                moves.add(new GeneralSubsetMove(add, del));
            }
        }
        // return all moves
        return moves;
    }
    
    /**
     * Infer the number of swaps that will be performed, taking into account the fixed number of desired
     * swaps as specified at construction and the maximum number of possible swaps for the given subset solution.
     * 
     * @param addCandidates candidate IDs to be added to the selection
     * @param deleteCandidates candidate IDs to be removed from the selection
     * @return number of swaps to be performed: desired fixed number if possible, or less (as many as possible)
     */
    private int numSwaps(Set<Integer> addCandidates, Set<Integer> deleteCandidates){
        return IntStream.of(addCandidates.size(), deleteCandidates.size(), numSwaps).min().getAsInt();
    }

}
