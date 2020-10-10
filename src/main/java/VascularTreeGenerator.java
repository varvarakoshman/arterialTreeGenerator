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
        List<Segment> possibleSegments = new ArrayList<>();
        for (int i = 0; i < Constants.N_TERM; i++) {
            double radScalingFactor = vascularTree.updateRsupp();// increase radius for new generations
            vascularTree.stretchCoordinates(radScalingFactor);
            // generate new segment that implies distance condition
            boolean conditionsMet = false;
            Vertex newNode;
            while (!conditionsMet) {
                newNode = vascularTree.generateNewVertex();
                if (newNode != null) {
                    possibleSegments = vascularTree.getPossibleBifurcations(newNode); // connect new segment to each existing segment
                    if (!possibleSegments.isEmpty()){
                        vascularTree.getExistingVertices().add(newNode);
                        conditionsMet = true;
                    }
                }
            }
            Pair<Integer, Segment> bifSegment = vascularTree.pickNewSegment(possibleSegments); // pick an optimal segment to bifurcate
            vascularTree.scaleTree(bifSegment); // update bifurcation rations in a tree
        }
        System.out.println("aaa");
        Visualizer.drawTree(vascularTree);
    }
}
