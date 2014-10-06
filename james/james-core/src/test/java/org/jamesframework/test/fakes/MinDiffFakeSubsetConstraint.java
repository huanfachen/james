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

package org.jamesframework.test.fakes;

import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jamesframework.core.problems.constraints.Constraint;
import org.jamesframework.core.problems.constraints.Validation;
import org.jamesframework.core.problems.constraints.validations.SimpleValidation;
import org.jamesframework.core.subset.SubsetSolution;

/**
 * Fake subset constraint based on fake subset data. Only accepts solutions where the minimum difference
 * in score of selected entities is larger than a given value. Used for testing only.
 * 
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public class MinDiffFakeSubsetConstraint implements Constraint<SubsetSolution, ScoredFakeSubsetData>{

    private static final Validation SATISFIED = new SimpleValidation(true);
    private static final Validation VIOLATED = new SimpleValidation(false);
    
    // minimum required difference in score of selected entities
    private final double minDiff;
    
    /**
     * Create a fake subset constraint with given minimum difference in scores of selected entities.
     * 
     * @param minDiff minimum difference in scores of selected entities.
     */
    public MinDiffFakeSubsetConstraint(double minDiff){
        this.minDiff = minDiff;
    }

    public double getMinDiff() {
        return minDiff;
    }
    
    /**
     * Check whether no entities are selected with a smaller difference in score than the required minimum difference.
     * 
     * @param solution solution to verify
     * @param data underlying (fake) subset data
     * @return true if minimum difference is satisfied
     */
    @Override
    public Validation validate(SubsetSolution solution, ScoredFakeSubsetData data) {
        // store scores in sorted set
        TreeSet<Double> scores = solution.getSelectedIDs().stream()
                                                          .map(ID -> data.getScore(ID))
                                                          .collect(Collectors.toCollection(TreeSet::new));
        // compute difference between consecutive sorted scores, break if minimum violated
        Double prevScore = null;
        for(double score : scores){
            if(prevScore != null && score-prevScore < minDiff){
                return VIOLATED;
            }
            prevScore = score;
        }
        // all ok
        return SATISFIED;
    }

}
