package dev.strongholdfinder.xaero;

import java.util.Iterator;
import java.util.List;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

final class StrongholdProvider extends ElementRenderProvider<MapMark, MapFrame> {
    private Iterator<MapMark> marks = List.<MapMark>of().iterator();

    @Override
    public void begin(ElementRenderLocation location, MapFrame frame) {
        marks = frame.marks.iterator();
    }

    @Override
    public boolean hasNext(ElementRenderLocation location, MapFrame frame) {
        return marks.hasNext();
    }

    @Override
    public MapMark getNext(ElementRenderLocation location, MapFrame frame) {
        return marks.next();
    }

    @Override
    public void end(ElementRenderLocation location, MapFrame frame) {
        marks = List.<MapMark>of().iterator();
    }
}
