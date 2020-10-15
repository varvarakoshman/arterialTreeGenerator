package model;

import javafx.util.Pair;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.Constants;

import java.awt.geom.Line2D;
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
@NoArgsConstructor
public class VascularTree {
    private int kTerm;
    private int kTot;
    private double rSupp;
    private Map<Integer, Segment> existingSegments = new HashMap<>();
    private List<Vertex> existingVertices = new ArrayList<>();
    private Random generator = new Random();

    public void generateRoot() {
        this.rSupp = Constants.R_SUPP;
        Vertex proximalEnd = new Vertex(0, 0.0, Constants.R_SUPP); // proximal end of a root segment is a point on a circle of radius r_supp
        existingVertices.add(proximalEnd);
        Vertex distalEnd = getNewNode(); // distal end is any point within a r_supp circle
        existingVertices.add(distalEnd);
        double length = Math.sqrt(Math.pow(distalEnd.getX() - proximalEnd.getX(), 2) + Math.pow(distalEnd.getY() - proximalEnd.getY(), 2));
        Double r_root = getRadius(length, Constants.Q_TERM);
        Segment rootSegment = new Segment(-1, -1, -1, r_root, length, proximalEnd, distalEnd, Constants.Q_TERM, 1, 1);
        existingSegments.put(0, rootSegment);
        this.kTot++;
        this.kTerm++;
    }

    public void stretchCoordinates(double radScalingFactor) {
        for (Segment segment : existingSegments.values()) {
            segment.getTo().setX(segment.getTo().getX() * radScalingFactor); // scale x-coordinate
            segment.getTo().setY(segment.getTo().getY() * radScalingFactor); // scale y-coordinate
            segment.setLength(segment.getLength() * radScalingFactor); // increase length
            segment.setRadius(getRadius(segment.getLength(), segment.getQi())); // increase radius correspondingly
        }
    }

    private Double getRadius(double length, double qi) { // from Poiseuille's law
        return Math.pow(qi * 8 * Constants.VISCOSITY * length / (Constants.DELTA_P_ * Math.PI), 1.0 / 4);
    }

    public Vertex getNewNode() {
        double x_distal = this.rSupp * Math.cos(generator.nextDouble());
        double y_distal = this.rSupp * Math.sin(generator.nextDouble());
        return new Vertex(existingVertices.size(), x_distal, y_distal);
    }

    public double updateRsupp() {
        double rSuppPrev = rSupp;
        rSupp = Math.sqrt((this.kTot + 1) * Constants.A_PERF / (Constants.N_TOTAL * Math.PI));
        return rSupp / rSuppPrev;
    }

    public void addValidVertex(Vertex newNode) {
        existingVertices.add(newNode);
        kTot++;
        kTerm++;
    }

    public Vertex generateNewVertex() {
        Vertex validVertex = null;
        boolean success = false;
        double dThersh = Math.sqrt(Math.PI * Math.pow(this.rSupp, 2) / this.kTerm); // threshold
        while (!success) {
            Vertex vertex = tryVerticesNtimes(dThersh);
            if (vertex != null) {
                validVertex = vertex;
                success = true;
            }
            dThersh = dThersh * 0.9;
        }
        return validVertex;
    }

    private Vertex tryVerticesNtimes(double dThersh) {
        for (int i = 0; i < Constants.N_TOSS; i++) {
            Vertex newNode = getNewNode();
            if (isVertexValid(newNode, dThersh)) {
                return newNode;
            }
        }
        return null;
    }

    // check new point on distance conditions
    private boolean isVertexValid(Vertex newPoint, double dThersh) {
        boolean conditionViolated = false;
        for (Segment existingSegment : existingSegments.values()) {
            double xBj = existingSegment.getFrom().getX();
            double yBj = existingSegment.getFrom().getY();
            double xj = existingSegment.getTo().getX();
            double yj = existingSegment.getTo().getY();
            double lj = existingSegment.getLength();
            double dProj = ((xBj - xj) * (newPoint.getX() - xj) + (yBj - yj) * (newPoint.getY() - yj)) / Math.pow(lj, 2);
            double dCrit;
            if (dProj <= 1 && dProj >= 0) {
                dCrit = Math.abs((-yBj + yj) * (newPoint.getX() - xj) + (xBj - xj) * (newPoint.getY() - yj)) / lj;
            } else {
                double dToStart = Math.sqrt(Math.pow(newPoint.getX() - xj, 2) + Math.pow(newPoint.getY() - yj, 2));
                double dToEnd = Math.sqrt(Math.pow(newPoint.getX() - xBj, 2) + Math.pow(newPoint.getY() - yBj, 2));
                dCrit = Math.min(dToStart, dToEnd);
            }
            if (dCrit < dThersh) {
                conditionViolated = true;
                break;
            }
        }
        return conditionViolated;
    }

    public List<Segment> getPossibleBifurcations(Vertex newNode) {
        List<Segment> possibleSegments = new ArrayList<>();
        for (Map.Entry<Integer, Segment> existingSegment : existingSegments.entrySet()) {
            Vertex bifVertex = new Vertex(existingVertices.size() + 1, (existingSegment.getValue().getFrom().getX() + existingSegment.getValue().getTo().getX()) / 2,
                    (existingSegment.getValue().getTo().getY() + existingSegment.getValue().getFrom().getY()) / 2);
            Line2D lineNew = new Line2D.Double(newNode.getX(), newNode.getY(), bifVertex.getX(), bifVertex.getY());
            Line2D existingLine = new Line2D.Double(existingSegment.getValue().getFrom().getX(), existingSegment.getValue().getFrom().getY(),
                    existingSegment.getValue().getTo().getX(), existingSegment.getValue().getTo().getY());
            if (!lineNew.intersectsLine(existingLine)) { // leave only segments which don't intersect with existing ones
                double newLen = Math.sqrt(Math.pow(newNode.getX() - bifVertex.getX(), 2) + Math.pow(newNode.getY() - bifVertex.getY(), 2));

                Segment newSegment = new Segment();
                newSegment.setFrom(bifVertex);
                newSegment.setTo(newNode);
                newSegment.setParentIndex(existingSegment.getKey());
                newSegment.setQi(Constants.Q_TERM);
                newSegment.setLength(newLen);
                newSegment.setRadius(getRadius(newLen, Constants.Q_TERM));
                newSegment.setBettaLeft(1);
                newSegment.setBettaRight(1);
                newSegment.setLeftIndex(-1);
                newSegment.setRightIndex(-1);

                possibleSegments.add(newSegment);
            }
        }
        return possibleSegments;
    }

    public void scaleTree(Pair<Integer, Segment> bifSegment) {
        boolean rootReached = bifSegment.getValue().getParentIndex() == -1;
        int currIndex = bifSegment.getKey();
        Segment currSegment = bifSegment.getValue();
        while (!rootReached) {
            Segment parentSegment = existingSegments.get(currSegment.getParentIndex());
            Segment rightChild = existingSegments.get(parentSegment.getRightIndex());
            Segment leftChild = existingSegments.get(parentSegment.getLeftIndex());
            parentSegment.setQi(rightChild.getQi() + leftChild.getQi());
            double balancedRadius;
            if (Objects.equals(parentSegment.getLeftIndex(), currIndex)) {
                balancedRadius = getBalancedRadius(currSegment.getRadius(), rightChild.getRadius());
                parentSegment.setBettaLeft(currSegment.getRadius() / balancedRadius);
                parentSegment.setBettaRight(rightChild.getRadius() / balancedRadius);
            } else {
                balancedRadius = getBalancedRadius(currSegment.getRadius(), leftChild.getRadius());
                parentSegment.setBettaRight(currSegment.getRadius() / balancedRadius);
                parentSegment.setBettaLeft(leftChild.getRadius() / balancedRadius);
            }
            if (balancedRadius > parentSegment.getRadius()) {
                parentSegment.setRadius(balancedRadius);
            }
//            parentSegment.setRadius(balancedRadius);
            currSegment = parentSegment;
            if (currSegment.getParentIndex() == -1) {
                rootReached = true;
                balanceRoot();
            }
        }
//        checkConditions();
    }

    private void balanceRoot() {
        List<Double> childRadii = existingSegments.values().stream()
                .filter(seg -> seg.getFrom().getId() == 0)
                .map(Segment::getRadius)
                .collect(Collectors.toList());
        if (childRadii.size() == 2) { // root has 2 children
            existingSegments.get(0).setRadius(getBalancedRadius(childRadii.get(0), childRadii.get(1)));
        } else { // root may have only 1 child - rebalance anyway
            existingSegments.get(0).setRadius(getBalancedRadius(childRadii.get(0), 0));
        }
    }

//    private void checkConditions() {
//        existingSegments.forEach((id, seg) -> {
//            if (seg.getLeftIndex() != -1 && seg.getRightIndex() != -1) {
//                System.out.println("id : " + id + " expected= " + Math.pow(Math.pow(existingSegments.get(seg.getLeftIndex()).getRadius(),
//                        Constants.GAMMA) + Math.pow(existingSegments.get(seg.getRightIndex()).getRadius(), Constants.GAMMA), 1 / Constants.GAMMA) + " actual= " + seg.getRadius());
//            }
//        });
//        System.out.println("=====\n");
//    }

    // split old segment by bifurcation node - remove old segment and insert 2 new, update indices
    private Pair<Integer, Segment> updateNodeIndicesAfterInsert(Segment inew) {
        int ibifIndex = inew.getParentIndex();
        int inewIndex = existingSegments.size() - 1; // new segment is the last
        int iconIndex = existingSegments.size();
        Segment ibif = existingSegments.get(ibifIndex);
        boolean isLeftChild = inew.getTo().getX() > inew.getFrom().getX();
//        double iconRadius = getRadius(ibif.getLength() / 2, ibif.getQi()); // recalculate the radius when length gets twice shorter

        Segment icon = new Segment(ibifIndex,
                ibif.getLeftIndex(),
                ibif.getRightIndex(),
                ibif.getRadius(),
                ibif.getLength() / 2,
                inew.getFrom(),
                ibif.getTo(),
                ibif.getQi(),
                ibif.getLeftIndex() != -1 ? existingSegments.get(ibif.getLeftIndex()).getRadius() / ibif.getRadius() : 1,
                ibif.getRightIndex() != -1 ? existingSegments.get(ibif.getRightIndex()).getRadius() / ibif.getRadius() : 1);

        ibif.setTo(inew.getFrom()); // qi, rad, bettaL, bettaR
        ibif.setLength(ibif.getLength() / 2);
        ibif.setLeftIndex(isLeftChild ? inewIndex : iconIndex);
        ibif.setRightIndex(isLeftChild ? iconIndex : inewIndex);
        ibif.setQi(ibif.getQi() + inew.getQi());
        double balancedRadius = getBalancedRadius(ibif.getRadius(), inew.getRadius());
        ibif.setRadius(balancedRadius);
        double bettaLeft = isLeftChild ? inew.getRadius() / balancedRadius : icon.getRadius() / balancedRadius;
        double bettaRight = isLeftChild ? icon.getRadius() / balancedRadius : inew.getRadius() / balancedRadius;
        ibif.setBettaLeft(bettaLeft);
        ibif.setBettaRight(bettaRight);
        existingSegments.put(iconIndex, icon);
        this.kTot++;
        return new Pair<>(ibifIndex, ibif);
    }

    // from possible segments pick the one with minimum volume and assign label to it
    public Pair<Integer, Segment> pickNewSegment(List<Segment> possibleSegments) {
        double min = Double.MAX_VALUE;
        Segment targetSegment = new Segment();
        for (Segment segment : possibleSegments) {
            double segmentVolume = targetFunction(segment);
            if (segmentVolume < min) {
                min = segmentVolume;
                targetSegment = segment;
            }
        }
        int targetIndex = existingSegments.size();
        existingSegments.put(targetIndex, targetSegment);
        existingVertices.add(targetSegment.getFrom());
        return updateNodeIndicesAfterInsert(targetSegment);
    }

    // target function is proportional to volume of a segment
    public double targetFunction(Segment segment) {
        return Math.pow(segment.getLength(), Constants.MU) * Math.pow(segment.getRadius(), Constants.LAMBDA);
    }

    private double getBalancedRadius(double firstRadius, double secondRadius) {
        return Math.pow(Math.pow(firstRadius, Constants.GAMMA) + Math.pow(secondRadius, Constants.GAMMA), 1 / Constants.GAMMA);
    }

    public void computeX0(){
        double dn =  existingSegments.values().stream()
                .filter(seg -> seg.getLeftIndex() == -1 && seg.getRightIndex() == -1)
                .findFirst().get().getRadius() * 2;
        existingSegments.values().stream()
                .filter(seg -> seg.getLeftIndex() == -1 && seg.getRightIndex() == -1)
                .collect(Collectors.toList()).forEach(seg -> seg.getTo().setX0(1)); // x0 = 1 for terminals
        List<Segment> bifurcSegms = existingSegments.values().stream()
                .filter(seg -> seg.getLeftIndex() != -1 && seg.getRightIndex() != -1)
                .collect(Collectors.toList());
        for (Segment bifurcSegm : bifurcSegms) {
            double d0 = bifurcSegm.getRadius() * 2;
            double d1 = existingSegments.get(bifurcSegm.getLeftIndex()).getRadius() * 2;
            double d2 = existingSegments.get(bifurcSegm.getRightIndex()).getRadius() * 2;
            Double [] temp = {d0, d1, d2};
            Arrays.sort(temp, Collections.reverseOrder());
            bifurcSegm.getTo().setX0(-Math.pow((temp[0] + temp[1]) / 2 * dn, 1/Constants.GAMMA));
        }
    }
}
