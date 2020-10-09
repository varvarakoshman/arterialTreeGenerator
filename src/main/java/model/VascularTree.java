package model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Setter
@Getter
@NoArgsConstructor
public class VascularTree {
    private int kTerm;
    private int kTot;
    private double rSupp;
    private List<Segment> existingSegments = new ArrayList<>();
    private Random generator = new Random();

    public void generateRoot() {
        this.rSupp = Constants.R_SUPP;
        Vertex proximalEnd = new Vertex(0.0, Constants.R_SUPP); // proximal end of a root segment is a point on a circle of radius r_supp
        Vertex distalEnd = getNewNode(); // distal end is any point within a r_supp circle
        double length = Math.sqrt(Math.pow(distalEnd.getX() - proximalEnd.getX(), 2) + Math.pow(distalEnd.getY() - proximalEnd.getY(), 2));
        Double r_root = getRootRadius(length, Constants.Q_PERF);
        Segment rootSegment = new Segment(0, null, null, null, r_root, length, proximalEnd, distalEnd, Constants.Q_PERF);
        existingSegments.add(rootSegment);
        updateRsupp();
    }

    private Double getRootRadius(double length, double qi) { // from Poiseuille's law
        Double rootRadius = Math.pow(qi * 8 * Constants.VISCOSITY * length / (Constants.DELTA_P_ * Math.PI), 1.0 / 4);
        this.kTot++;
        this.kTerm++;
        return rootRadius;
    }

    public Vertex getNewNode() {
        double x_distal = this.rSupp * Math.cos(generator.nextDouble());
        double y_distal = this.rSupp * Math.sin(generator.nextDouble());
        return new Vertex(x_distal, y_distal);
    }

    private void updateRsupp() {
        this.rSupp = (this.kTot + 1) * Constants.A_PERF / Constants.N_TOTAL;
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
        for (Segment existingSegment : existingSegments) {
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

    public Segment createBifurcation(Vertex newNode, Segment existingSegment) {
        return null;
    }

    public boolean isBifValid(Segment newSegment) {

    }

    public void scaleTree() {

    }

    public void balanceTree() {

    }

    public void pickNewSegment(List<Segment> possibleSegments) {
        double min = Double.MAX_VALUE;
        Segment targetSegment = null;
        for (Segment segment : possibleSegments) {
            double segmentVolume = targetFunction(segment);
            if (segmentVolume < min) {
                min = segmentVolume;
                targetSegment = segment;
            }
        }
        existingSegments.add(targetSegment);
    }

    // T is proportional to volume of segment
    private double targetFunction(Segment segment) {
        return Math.pow(segment.getLength(), Constants.MU) * Math.pow(segment.getRadius(), Constants.LAMBDA);
    }

}
