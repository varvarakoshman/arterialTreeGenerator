import javafx.util.Pair;
import model.Segment;
import model.VascularTree;
import model.Vertex;
import util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VascularTreeGenerator {

    public static void main(String[] args) {
        VascularTree vascularTree = new VascularTree();
        vascularTree.generateRoot(); // generate root segment
        List<Segment> possibleSegments = new ArrayList<>();
        while (vascularTree.getKTerm() < Constants.N_TERM) {
            double radScalingFactor = vascularTree.updateRsupp();// increase radius for new generations
//            vascularTree.stretchCoordinates(radScalingFactor);
            // generate new segment that implies distance condition
            boolean conditionsMet = false;
            Vertex newNode;
            while (!conditionsMet) {
                newNode = vascularTree.generateNewVertex();
                if (newNode != null) {
                    possibleSegments = vascularTree.getPossibleBifurcations(newNode); // connect new segment to each existing segment
                    if (!possibleSegments.isEmpty()) {
                        vascularTree.addValidVertex(newNode);
                        conditionsMet = true;
                    }
                }
            }
            Pair<Integer, Segment> bifSegment = vascularTree.pickNewSegment(possibleSegments); // pick an optimal segment to bifurcate
            vascularTree.scaleTree(bifSegment); // update bifurcation rations in a tree
        }
        testResultTree(vascularTree);
        Visualizer.drawTree(vascularTree);
    }

    private static void testResultTree(VascularTree vascularTree) {
        Map<Integer, Pair<Double, Double>> test = new HashMap<>();
        vascularTree.getExistingSegments().forEach((id, seg) -> {
            if (seg.getLeftIndex() != -1 && seg.getRightIndex() != -1) {
                test.put(id, new Pair<>(seg.getRadius(), Math.pow(Math.pow(vascularTree.getExistingSegments().get(seg.getLeftIndex()).getRadius(),
                        Constants.GAMMA) + Math.pow(vascularTree.getExistingSegments().get(seg.getRightIndex()).getRadius(), Constants.GAMMA), 1 / Constants.GAMMA)));
            }
        });
        test.forEach((k, v) -> System.out.println(String.format("id: %d expected= %f actual= %f", k, v.getValue(), v.getKey())));
    }
}
