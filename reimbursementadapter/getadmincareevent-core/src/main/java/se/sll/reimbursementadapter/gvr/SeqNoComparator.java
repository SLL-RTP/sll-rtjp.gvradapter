package se.sll.reimbursementadapter.gvr;

import java.util.Comparator;

import riv.followup.processdevelopment.reimbursement.v1.ActivityType;
import riv.followup.processdevelopment.reimbursement.v1.OrderedCVType;

/**
 * Comparator class that compares two objects by looking at their seqNo properties.
 * The two objects are expected to be of the same type and only objects of the type
 * <code>OrderedCVType</code> or <code>ActivityType</code> are allowed.
 *
 */
public class SeqNoComparator implements Comparator<Object> {

    @Override
    public int compare(Object o1, Object o2) throws NullPointerException, ClassCastException {
        if (o1 instanceof OrderedCVType) {
            return compareSeqNo(((OrderedCVType) o1).getSeqNo(), ((OrderedCVType) o2).getSeqNo());
        } else if (o1 instanceof ActivityType) {
            return compareSeqNo(((ActivityType) o1).getSeqNo(), ((ActivityType) o2).getSeqNo());
        } else {
            throw new ClassCastException("Objects to compare must be either OrderedCVType or ActivityType.");
        }
    }
    
    /**
     * Compares two seqNo Integers.
     * 
     * @param seqNo1 The first seqNo to be compared
     * @param seqNo2 The second seqNo to be compared
     * @return -1, 0, or 1 as the first argument is less than, equal to, or greater than the second.
     */
    private int compareSeqNo(Integer seqNo1, Integer seqNo2) {
        if (seqNo1 < seqNo2) {
            return -1;
        } else if (seqNo1 > seqNo2) {
            return 1;
        } else {
            return 0;
        }
    }

}
