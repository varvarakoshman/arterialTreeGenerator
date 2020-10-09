package model;

import javafx.util.Pair;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.Constants;

import java.awt.geom.Line2D;
import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class VascularTree {
    private int kTerm;
    private int kTot;
    private double rSupp;
    private Map<Integer, Segment> existingSegments = new HashMap<>();
    private Random generator = new Random();

    public void generateRoot() {
        this.rSupp = Constants.R_SUPP;
        Vertex proximalEnd = new Vertex(0.0, Constants.R_SUPP); // proximal end of a root segment is a point on a circle of radius r_supp
        Vertex distalEnd = getNewNode(); // distal end is any point within a r_supp circle
        double length = Math.sqrt(Math.pow(distalEnd.getX() - proximalEnd.getX(), 2) + Math.pow(distalEnd.getY() - proximalEnd.getY(), 2));
        Double r_root = getRadius(length, Constants.Q_TERM);
        Segment rootSegment = new Segment(null, null, null, r_root, length, proximalEnd, distalEnd, Constants.Q_TERM, 1, 1);
        existingSegments.put(0, rootSegment);
        this.kTot++;
        this.kTerm++;
    }

    public void stretchCoordinates() {
        for (Segment segment : existingSegments.values()) {
            segment.getTo().setX(segment.getTo().getX() * this.rSupp); // scale x-coordinate
            segment.getTo().setY(segment.getTo().getY() * this.rSupp); // scale y-coordinate
            segment.setLength(segment.getLength() * this.rSupp); // increase length
            segment.setRadius(getRadius(segment.getLength(), segment.getQi())); // increase radius correspondingly
        }
    }

    private Double getRadius(double length, double qi) { // from Poiseuille's law
        return Math.pow(qi * 8 * Constants.VISCOSITY * length / (Constants.DELTA_P_ * Math.PI), 1.0 / 4);
    }

    public Vertex getNewNode() {
        double x_distal = this.rSupp * Math.cos(generator.nextDouble());
        double y_distal = this.rSupp * Math.sin(generator.nextDouble());
        return new Vertex(x_distal, y_distal);
    }

    public void updateRsupp() {
        this.rSupp = Math.sqrt((this.kTot + 1) * Constants.A_PERF / (Constants.N_TOTAL * Math.PI));
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
        this.kTot++;
        this.kTerm++;
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
            Vertex bifVertex = new Vertex(existingSegment.getValue().getFrom().getX() + (existingSegment.getValue().getTo().getX() - existingSegment.getValue().getFrom().getX()) / 2,
                    existingSegment.getValue().getFrom().getY() + (existingSegment.getValue().getFrom().getY() - existingSegment.getValue().getTo().getY()) / 2);
            Line2D lineNew = new Line2D.Double(newNode.getX(), newNode.getY(), bifVertex.getX(), bifVertex.getY());
            Line2D existingLine = new Line2D.Double(existingSegment.getValue().getFrom().getX(), existingSegment.getValue().getFrom().getY(),
                    existingSegment.getValue().getTo().getX(), existingSegment.getValue().getTo().getY());
            if (!lineNew.intersectsLine(existingLine)) { // leave only segments which don't intersect with existing ones
                Segment newSegment = new Segment();
                newSegment.setFrom(bifVertex);
                newSegment.setTo(newNode);
                newSegment.setParentIndex(existingSegment.getKey());
                newSegment.setBettaLeft(1);
                newSegment.setBettaRight(1);
                newSegment.setQi(Constants.Q_TERM);
                double newLen = Math.sqrt(Math.pow(newNode.getX() - bifVertex.getX(), 2) + Math.pow(newNode.getY() - bifVertex.getY(), 2));
                newSegment.setLength(newLen);
                newSegment.setRadius(getRadius(newLen, Constants.Q_TERM));
                possibleSegments.add(newSegment);
            }
        }
        return possibleSegments;
    }

    public void scaleTree(Pair<Integer, Segment> bifSegment) {
        boolean rootReached = false;
        int currIndex = bifSegment.getKey();
        Segment currSegment = bifSegment.getValue();
        while (!rootReached) {
            Segment parentSegment = existingSegments.get(currSegment.getParentIndex());
            double balancedRadius;
            if (Objects.equals(parentSegment.getLeftIndex(), currIndex)) {
                Segment rightChild = existingSegments.get(parentSegment.getRightIndex());
                balancedRadius = getBalancedRadius(currSegment.getRadius(), rightChild.getRadius());
                parentSegment.setBettaLeft(currSegment.getRadius() / balancedRadius);
                parentSegment.setBettaRight(rightChild.getRadius() / balancedRadius);
            } else {
                Segment leftChild = existingSegments.get(parentSegment.getLeftIndex());
                balancedRadius = getBalancedRadius(currSegment.getRadius(), leftChild.getRadius());
                parentSegment.setBettaRight(currSegment.getRadius() / balancedRadius);
                parentSegment.setBettaLeft(leftChild.getRadius() / balancedRadius);
            }
            parentSegment.setRadius(balancedRadius);
            currSegment = parentSegment;
            if (Objects.isNull(currSegment.getParentIndex())) {
                rootReached = true;
            }
        }
    }

    // split old segment by bifurcation node - remove old segment and insert 2 new, update indices
    private void updateNodeIndicesAfterInsert(Segment inew) {
        int ibifIndex = inew.getParentIndex();
        int inewIndex = existingSegments.size(); // new segment is the last
        int iconIndex = existingSegments.size() + 1;
        Segment ibif = existingSegments.get(ibifIndex);
        boolean isLeftChild = inew.getTo().getX() > inew.getFrom().getX();
        double iconRadius = getRadius(ibif.getLength() / 2, ibif.getQi()); // recalculate the radius when lenght gets twice shorter
        Segment icon = new Segment(ibifIndex,
                isLeftChild ? inewIndex : ibif.getLeftIndex(),
                isLeftChild ? ibif.getRightIndex() : inewIndex,
                iconRadius,
                ibif.getLength() / 2,
                inew.getFrom(),
                ibif.getTo(),
                ibif.getQi(),
                existingSegments.get(ibif.getLeftIndex()).getRadius() / iconRadius,
                existingSegments.get(ibif.getRightIndex()).getRadius() / iconRadius);
        ibif.setTo(inew.getFrom()); // qi, rad, bettaL, bettaR
        ibif.setLength(ibif.getLength() / 2);
        ibif.setLeftIndex(isLeftChild ? iconIndex : inewIndex);
        ibif.setRightIndex(isLeftChild ? inewIndex : iconIndex);
        ibif.setQi(ibif.getQi() + inew.getQi());
        double balancedRadius = getBalancedRadius(ibif.getRadius(), inew.getRadius());
        ibif.setRadius(balancedRadius);
        double bettaLeft = isLeftChild ? inew.getRadius() / balancedRadius : icon.getRadius() / balancedRadius;
        double bettaRight = isLeftChild ? icon.getRadius() / balancedRadius : inew.getRadius() / balancedRadius;
        ibif.setBettaLeft(bettaLeft);
        ibif.setBettaRight(bettaRight);
        existingSegments.put(iconIndex, icon);
        this.kTot++;
    }

    // from possible segments pick the one with minimum volume and assign label to it
    public Pair<Integer, Segment> pickNewSegment(List<Segment> possibleSegments) {
        double min = Double.MAX_VALUE;
        Segment targetSegment = null;
        for (Segment segment : possibleSegments) {
            double segmentVolume = targetFunction(segment);
            if (segmentVolume < min) {
                min = segmentVolume;
                targetSegment = segment;
            }
        }
        int targetIndex = existingSegments.size() + 1;
        existingSegments.put(targetIndex, targetSegment);
        updateNodeIndicesAfterInsert(targetSegment);
        return new Pair<>(targetIndex, targetSegment);
    }

    // target function is proportional to volume of a segment
    private double targetFunction(Segment segment) {
        return Math.pow(segment.getLength(), Constants.MU) * Math.pow(segment.getRadius(), Constants.LAMBDA);
    }

    private double getBalancedRadius(double firstRadius, double secondRadius) {
        return Math.pow(Math.pow(firstRadius, Constants.GAMMA) + Math.pow(secondRadius, Constants.GAMMA), 1 / Constants.GAMMA);
    }
}
