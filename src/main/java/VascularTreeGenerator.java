import model.Segment;
import model.VascularTree;
import model.Vertex;
import util.Constants;

import java.util.ArrayList;
import java.util.List;

public class VascularTreeGenerator {

    public static void main(String[] args) {
        VascularTree vascularTree = new VascularTree();
        // generate root segment
        vascularTree.generateRoot();
        // vascularTree.scaleTree(); ??????
        for (int i = 0; i < Constants.N_TERM; i++) {
            // generate new segment that implies distance condition
            boolean conditionsMet = false;
            Vertex newNode = null;
            while (!conditionsMet) {
                newNode = vascularTree.generateNewVertex();
                conditionsMet = newNode != null;
            }
            // connect new segment to each existing segment
            List<Segment> possibleSegments = new ArrayList<>();
            for (Segment existingSegment : vascularTree.getExistingSegments()) {
                Segment bifurcationNode = vascularTree.createBifurcation(newNode, existingSegment);
                if (vascularTree.isBifValid(bifurcationNode)) {
                    possibleSegments.add(bifurcationNode);
                }
            }
            vascularTree.pickNewSegment(possibleSegments);
            vascularTree.scaleTree();
            vascularTree.balanceTree();
        }
//        Visualizer.drawTree(vascularTree.getExistingSegments());
    }
}
