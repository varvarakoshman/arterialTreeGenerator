import model.VascularTree;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;

import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

public class Visualizer {

    public static void drawTree(VascularTree vascularTree) {
        Graph graph = new MultiGraph("Visualiser");
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        graph.addAttribute("ui.stylesheet", "url('file:style.css')");
        vascularTree.getExistingVertices().forEach(vertex -> graph.addNode(String.valueOf(vertex.getId())).setAttribute("xy", vertex.getX(), vertex.getY()));
        PrimitiveIterator.OfInt edgeIterator = IntStream.range(0, vascularTree.getExistingSegments().size()).iterator();
        vascularTree.getExistingSegments().values().forEach(segment -> {
            String edgeWidth = String.valueOf(Math.round(segment.getRadius() * 2 * 100 * 37.795)); // m to cm to px
            graph.addEdge(edgeIterator.next().toString(), segment.getFrom().getId(), segment.getTo().getId(), false)
                    .addAttribute("ui.style", "size: " + edgeWidth + "px; fill-color: red;");
        });
        graph.getEachEdge().forEach(edge -> edge.addAttribute("ui.label", edge.getAttribute("ui.style").toString().split(";")[0].split(" ")[1]));
        graph.display();
    }
}
