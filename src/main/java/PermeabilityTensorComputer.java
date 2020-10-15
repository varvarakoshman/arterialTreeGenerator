import lombok.AllArgsConstructor;
import model.Segment;
import model.VascularTree;
import util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

//@AllArgsConstructor
public class PermeabilityTensorComputer {

    private VascularTree vascularTree;

    public PermeabilityTensorComputer( VascularTree vascularTree) {
        this.vascularTree = vascularTree;
    }

    private List<Segment> capp;
    private List<Segment> arterioll;
    private List<Segment> arteria;

    private double x0cap;
    private double x0arterll;
    private double x0arteria;

    private double computeVavg(VascularTree vascularTree) {
        int nSegments = vascularTree.getExistingSegments().values().size();
        double totalV = 0;
        for (Segment segment : vascularTree.getExistingSegments().values()) {
            totalV += vascularTree.targetFunction(segment) * Math.PI;
        }
        return totalV / nSegments;
    }

    private void prepareLists(){
        arteria = vascularTree.getExistingSegments().values().stream()
                .filter(seg -> seg.getRadius() >= 9)
                .collect(Collectors.toList());
        arterioll = vascularTree.getExistingSegments().values().stream()
                .filter(seg -> seg.getRadius() < 9 && seg.getRadius() > 2)
                .collect(Collectors.toList());
        arterioll = vascularTree.getExistingSegments().values().stream()
                .filter(seg -> seg.getRadius() < 2)
                .collect(Collectors.toList());
        x0cap = 1;
        x0arteria = arterioll.stream().map(seg -> seg.getTo().getX0()).reduce(Double::sum).get() / arterioll.size();
        x0arterll = 0;
        for (Segment seg : arterioll) {
            x0arterll += seg.getTo().getX0();
        }
        x0arterll = x0arterll / arteria.size();
    }

    // 0 - arterial
    // 1 - arteriolar
    // 2 - cappilary
    private int getPointType(Segment segment) {
        if (segment.getRadius() >= 9) {
            return 0;
        } else if (segment.getRadius() < 9 && segment.getRadius() > 2) {
            return 1;
        } else {
            return 2;
        }
    }

    private double computeDeltaX0(int pointType) {
        if (pointType == 0) {
            return x0arterll;
        } else if (pointType == 1) {
            return x0arterll;
        } else {
            return x0cap;
        }
    }

    private Function<Segment, Double> getCompf(int componenti){
        Function<Segment, Double> f;
        if (componenti == 0) {
            f = seg -> seg.getTo().getX0() - seg.getFrom().getX0();
        } else if (componenti == 1) {
            f = seg -> seg.getTo().getX() - seg.getFrom().getX();
        } else {
            f = seg -> seg.getTo().getY() - seg.getFrom().getY();
        }
        return f;
    }

    private double discreteSum(List<Segment> list, int componenti, int componentj){
        return list.stream()
                .map(seg -> Math.pow(seg.getRadius() * 2, 4) * getCompf(componenti).apply(seg) * getCompf(componentj).apply(seg) / seg.getLength())
                .reduce(0., Double::sum);
    }

    //     ( K00 K01 K02 )
    // K = ( K10 K11 K12 )
    //     ( K20 K21 K22 )

    // compute 6 Kij independent components of symmetric 3*3 permeability tensor for each point
    private double computeKtensor(double vavg, Segment segment, int componenti, int componentj) {
        int pointType = getPointType(segment);
        double c = Math.PI / (128 * vavg * Constants.VISCOSITY * computeDeltaX0(pointType));
        double discreteS = 0;
        if (getPointType(segment) == 0){
            discreteS = discreteSum(arteria, componenti, componentj);
        } else if (getPointType(segment) == 1) {
            discreteS = discreteSum(arterioll, componenti, componentj);
        } else {
            discreteS = discreteSum(capp, componenti, componentj);
        }
        return 0;
    }

    public void computeKtoFiles() {
        prepareLists();
        double vavg = computeVavg(vascularTree);
        List<Double> k00s = new ArrayList<>();
        List<Double> k11s = new ArrayList<>();
        List<Double> k22s = new ArrayList<>();
        List<Double> k01s = new ArrayList<>();
        List<Double> k02s = new ArrayList<>();
        List<Double> k12s = new ArrayList<>();
        for (Segment segment : vascularTree.getExistingSegments().values()) {
            k00s.add(computeKtensor(vavg, segment, 0, 0));
            k11s.add(computeKtensor(vavg, segment, 1, 1));
            k22s.add(computeKtensor(vavg, segment, 2, 2));
            k01s.add(computeKtensor(vavg, segment, 0, 1));
            k02s.add(computeKtensor(vavg, segment, 0, 2));
            k12s.add(computeKtensor(vavg, segment, 1, 2));
        }
        // записать каждый в файл
        String converted = "C:\\Users\\Varvara_Koshman\\IdeaProjects\\vascularTree\\src\\main\\resources\\k00.txt";
    }

}
