import javafx.util.Pair;
import model.Segment;
import model.VascularTree;
import model.Vertex;
import util.Constants;

import java.util.ArrayList;
import java.util.List;

public class VascularTreeGenerator {

    public static void main(String[] args) {
        VascularTree vascularTree = new VascularTree();
        vascularTree.generateRoot(); // generate root segment
        for (int i = 0; i < Constants.N_TERM; i++) {
            vascularTree.updateRsupp(); // increase radius for new generations
            vascularTree.stretchCoordinates();
            // generate new segment that implies distance condition
            boolean conditionsMet = false;
            Vertex newNode = null;
            while (!conditionsMet) {
                newNode = vascularTree.generateNewVertex();
                conditionsMet = newNode != null;
            }
            List<Segment> possibleSegments = vascularTree.getPossibleBifurcations(newNode); // connect new segment to each existing segment
            Pair<Integer, Segment> bifSegment = vascularTree.pickNewSegment(possibleSegments); // pick an optimal segment to bifurcate
            vascularTree.scaleTree(bifSegment); // update bifurcation rations in a tree
        }
//        Visualizer.drawTree(vascularTree.getExistingSegments());
    }
}
