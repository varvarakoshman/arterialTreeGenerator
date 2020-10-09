package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Segment {
    private int index;
    private Integer parentIndex;
    private Integer leftIndex;
    private Integer rightIndex;
    private double radius;
    private double length;
    private Vertex from;
    private Vertex to;
    private double qi;
    private double bettaLeft;
    private double bettaRight;
}
